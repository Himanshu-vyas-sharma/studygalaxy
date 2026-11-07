package app.studygalaxy.ui.components

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

/**
 * The voice of Vega, the guide that walks a new student through setup.
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
            setSpeechRate(0.96f)  // a touch slower than default reads as calm
            setPitch(1.0f)
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

    fun speak(text: String) {
        if (muted) return
        val engine = tts
        if (!ready || engine == null) { pending = text; return }
        engine.speak(text, TextToSpeech.QUEUE_FLUSH, Bundle(), "vega-${text.hashCode()}")
    }

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
