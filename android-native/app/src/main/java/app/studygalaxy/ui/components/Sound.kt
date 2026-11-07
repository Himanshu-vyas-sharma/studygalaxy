package app.studygalaxy.ui.components

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import androidx.compose.runtime.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * The melodic layer: a slow ambient pad under onboarding and a bell that rises
 * in pitch as the student answers.
 *
 * Tones are synthesised into a PCM buffer rather than shipped as audio files.
 * That keeps the APK unchanged, avoids a licensing question over background
 * music, and lets the chime pitch follow progress — a fixed clip could not.
 * Everything here is pure sine partials with an exponential decay, which is
 * what makes it read as a soft bell rather than a beep.
 */

private const val SAMPLE_RATE = 44100

/** A pentatonic run: any order of these sounds consonant, so progress always resolves. */
private val PENTATONIC = floatArrayOf(
    523.25f, 587.33f, 659.25f, 783.99f, 880.00f,   // C5 D5 E5 G5 A5
    1046.50f, 1174.66f, 1318.51f, 1567.98f, 1760.0f // and the octave above
)

private fun bellBuffer(freq: Float, seconds: Float, gain: Float): ShortArray {
    val n = (SAMPLE_RATE * seconds).toInt()
    val out = ShortArray(n)
    for (i in 0 until n) {
        val t = i / SAMPLE_RATE.toFloat()
        val decay = exp(-3.2f * t)
        // Fundamental plus two quiet partials: enough to sound struck, not synthetic.
        val s = sin(2f * PI.toFloat() * freq * t) * 0.6f +
            sin(2f * PI.toFloat() * freq * 2f * t) * 0.22f +
            sin(2f * PI.toFloat() * freq * 3.01f * t) * 0.10f
        out[i] = (s * decay * gain * Short.MAX_VALUE).toInt()
            .coerceIn(-32768, 32767).toShort()
    }
    return out
}

/** A slow two-note drone that loops without an audible seam. */
private fun padBuffer(): ShortArray {
    val seconds = 8f
    val n = (SAMPLE_RATE * seconds).toInt()
    val out = ShortArray(n)
    for (i in 0 until n) {
        val t = i / SAMPLE_RATE.toFloat()
        // Two low voices a fifth apart, breathing in and out of phase.
        val a = sin(2f * PI.toFloat() * 130.81f * t) * 0.5f      // C3
        val b = sin(2f * PI.toFloat() * 196.00f * t) * 0.35f     // G3
        val breathe = 0.55f + 0.45f * sin(2f * PI.toFloat() * t / seconds)
        // Equal fade at both ends so the loop point is inaudible.
        val edge = minOf(1f, t / 1.5f, (seconds - t) / 1.5f)
        out[i] = ((a + b) * breathe * edge * 0.16f * Short.MAX_VALUE).toInt()
            .coerceIn(-32768, 32767).toShort()
    }
    return out
}

private fun newTrack(buf: ShortArray, looping: Boolean): AudioTrack {
    val track = AudioTrack.Builder()
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
        )
        .setAudioFormat(
            AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(SAMPLE_RATE)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build()
        )
        .setBufferSizeInBytes(buf.size * 2)
        .setTransferMode(AudioTrack.MODE_STATIC)
        .build()
    track.write(buf, 0, buf.size)
    if (looping) track.setLoopPoints(0, buf.size, -1)
    return track
}

class Sound {
    private var pad: AudioTrack? = null
    private var padJob: Job? = null
    var muted = false
        set(value) { field = value; if (value) stopPad() }

    /** Starts the ambient bed. Safe to call repeatedly. */
    fun startPad(scope: CoroutineScope) {
        if (muted || pad != null) return
        padJob = scope.launch(Dispatchers.Default) {
            val buf = padBuffer()
            if (!isActive || muted) return@launch
            runCatching {
                pad = newTrack(buf, looping = true).also {
                    it.setVolume(0.5f)
                    it.play()
                }
            }
        }
    }

    fun stopPad() {
        padJob?.cancel(); padJob = null
        runCatching { pad?.stop(); pad?.release() }
        pad = null
    }

    /**
     * A bell whose pitch climbs with progress, so answering feels like a scale
     * resolving upward rather than the same blip ten times.
     */
    fun chime(scope: CoroutineScope, stepIndex: Int) {
        if (muted) return
        val freq = PENTATONIC[stepIndex.coerceIn(0, PENTATONIC.size - 1)]
        scope.launch(Dispatchers.Default) {
            runCatching {
                val t = newTrack(bellBuffer(freq, 1.1f, 0.5f), looping = false)
                t.setNotificationMarkerPosition((SAMPLE_RATE * 1.05f).toInt())
                t.setPlaybackPositionUpdateListener(
                    object : AudioTrack.OnPlaybackPositionUpdateListener {
                        override fun onMarkerReached(track: AudioTrack?) {
                            runCatching { track?.stop(); track?.release() }
                        }
                        override fun onPeriodicNotification(track: AudioTrack?) {}
                    }
                )
                t.play()
            }
        }
    }

    /** The low swell under the reveal. */
    fun swell(scope: CoroutineScope) {
        if (muted) return
        scope.launch(Dispatchers.Default) {
            runCatching {
                val t = newTrack(bellBuffer(261.63f, 2.6f, 0.65f), looping = false)
                t.play()
            }
        }
    }

    /**
     * Drops the pad under speech and lifts it afterwards. Without this the
     * drone and the voice compete at the same level and both sound muddy;
     * ducking is what makes the line feel scored rather than talked over.
     */
    fun duck(active: Boolean) {
        runCatching { pad?.setVolume(if (active) 0.14f else 0.5f) }
    }

    fun release() = stopPad()
}

@Composable
fun rememberSound(): Sound {
    val sound = remember { Sound() }
    DisposableEffect(Unit) { onDispose { sound.release() } }
    return sound
}
