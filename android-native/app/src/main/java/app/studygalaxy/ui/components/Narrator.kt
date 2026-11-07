package app.studygalaxy.ui.components

import android.content.Context
import android.util.Log
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import android.speech.tts.UtteranceProgressListener
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

/**
 * The voice of Galaxy, the guide that walks a new student through setup.
 *
 * Uses Android's on-device TextToSpeech rather than recorded audio or a cloud
 * voice: it costs nothing, adds no megabytes to the APK, works with no network,
 * and speaks whatever language pack the phone already has. The trade-off is
 * that it sounds like the system voice rather than a performer — swap in
 * recorded clips later if the app earns the download size.
 */
class Narrator(context: Context) {
    private var tts: TextToSpeech? = null

    var ready by mutableStateOf(false)
        private set
    var speaking by mutableStateOf(false)
        private set
    /** Muting persists for the session; the captions carry the script regardless. */
    var muted by mutableStateOf(false)

    private var pending: String? = null
    private val appCtx = context.applicationContext

    init {
        start(engine = null)
    }

    /**
     * Some phones ship a TTS engine but leave `tts_default_synth` unset, and
     * then the no-engine constructor fails and the voice is silently absent.
     * If the default fails, try Google's engine by name before giving up.
     */
    private fun start(engine: String?) {
        val listener = TextToSpeech.OnInitListener { status ->
            if (status == TextToSpeech.SUCCESS) {
                configure()
            } else if (engine == null) {
                runCatching { tts?.shutdown() }
                tts = null
                start(engine = GOOGLE_TTS)
            }
        }
        tts = if (engine == null) TextToSpeech(appCtx, listener)
        else TextToSpeech(appCtx, listener, engine)
    }

    private fun configure() {
        tts?.apply {
            // Indian English where available: most of these students will
            // find it easier to follow than US English.
            val res = setLanguage(Locale("en", "IN"))
            if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
                setLanguage(Locale.UK)
            }
            // The default voice for a locale is usually the cheapest one the
            // engine ships. Google TTS normally also carries higher-quality
            // variants, and picking one is the single biggest difference
            // between "robot" and "person".
            pickBestVoice(this)?.let {
                voice = it
                Log.i("Narrator", "voice=${it.name} locale=${it.locale} quality=${it.quality}")
            }
            // Slower and very slightly lower: unhurried rather than brisk.
            setSpeechRate(0.88f)
            setPitch(0.96f)
            setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) { speaking = true }
                override fun onDone(utteranceId: String?) { speaking = false }
                @Deprecated("deprecated in API 21")
                override fun onError(utteranceId: String?) { speaking = false }
                override fun onError(utteranceId: String?, errorCode: Int) { speaking = false }
            })
        }
        ready = true
        pending?.let { speak(it); pending = null }
    }

    /**
     * Speaks a line as separate clauses with short silences between them.
     * One long utterance comes out flat and rushed; breaking on sentence
     * boundaries and inserting a breath is what gives the read its cadence.
     */
    fun speak(text: String) {
        if (muted) return
        val engine = tts
        if (!ready || engine == null) { pending = text; return }

        val clauses = text.split(Regex("(?<=[.!?—])[ ]+"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        var mode = TextToSpeech.QUEUE_FLUSH
        clauses.forEachIndexed { i, clause ->
            engine.speak(clause, mode, Bundle(), "galaxy-${text.hashCode()}-$i")
            mode = TextToSpeech.QUEUE_ADD
            if (i < clauses.lastIndex) {
                engine.playSilentUtterance(260L, TextToSpeech.QUEUE_ADD, "gap-$i")
            }
        }
    }

    /**
     * Scores every installed voice and takes the best English one. Quality
     * dominates; an Indian accent is preferred, then British, then anything
     * else English. Voices whose data is not downloaded are skipped.
     */
    private fun pickBestVoice(engine: TextToSpeech): Voice? = runCatching {
        engine.voices
            ?.asSequence()
            ?.filter { it.locale?.language == "en" }
            ?.filterNot { it.features?.contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED) == true }
            ?.maxByOrNull { v ->
                val localeBonus = when (v.locale?.country?.uppercase()) {
                    "IN" -> 300
                    "GB" -> 200
                    "AU" -> 120
                    else -> 60
                }
                // Google names its better variants with higher indices; the
                // quality field is the reliable signal, so weight it hardest.
                v.quality * 10 + localeBonus - (if (v.isNetworkConnectionRequired) 40 else 0)
            }
    }.getOrNull()

    fun stop() {
        runCatching { tts?.stop() }
        speaking = false
    }

    fun shutdown() {
        runCatching { tts?.stop(); tts?.shutdown() }
        tts = null
        ready = false
        speaking = false
    }

    private companion object { const val GOOGLE_TTS = "com.google.android.tts" }
}

/** Builds a Narrator tied to the composition and shuts it down on exit. */
@Composable
fun rememberNarrator(): Narrator {
    val ctx = LocalContext.current
    val narrator = remember { Narrator(ctx) }
    DisposableEffect(Unit) { onDispose { narrator.shutdown() } }
    return narrator
}
