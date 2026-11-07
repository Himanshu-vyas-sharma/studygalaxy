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

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.apply {
                    // Indian English where available: most of these students
                    // will find it easier to follow than US English.
                    val wanted = Locale("en", "IN")
                    val res = setLanguage(wanted)
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
        }
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
}

/** Builds a Narrator tied to the composition and shuts it down on exit. */
@Composable
fun rememberNarrator(): Narrator {
    val ctx = LocalContext.current
    val narrator = remember { Narrator(ctx) }
    DisposableEffect(Unit) { onDispose { narrator.shutdown() } }
    return narrator
}
