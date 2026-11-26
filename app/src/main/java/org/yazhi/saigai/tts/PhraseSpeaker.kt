package org.yazhi.saigai.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.Locale
import org.yazhi.saigai.model.LanguageMode
import org.yazhi.saigai.model.Phrase

enum class SpeakerStatus {
    INITIALISING,
    READY,
    TAMIL_VOICE_MISSING,
    UNAVAILABLE,
}

/**
 * On-device speech output for Quick Cards (M1).
 *
 * Design doc §4/§6: speech is produced by Android native TTS, entirely offline.
 * "Both" speaks Tamil first and then English, sequenced through
 * [UtteranceProgressListener] instead of two racing queue entries.
 */
class PhraseSpeaker(private val appContext: Context) {

    var status by mutableStateOf(SpeakerStatus.INITIALISING)
        private set

    private var tts: TextToSpeech? = null
    private var pendingEnglish: String? = null
    private var tamilAvailable = false

    private val tamil = Locale("ta", "IN")
    private val english = Locale("en", "IN")

    fun start() {
        if (tts != null) return
        tts = TextToSpeech(appContext) { initStatus -> onInitialised(initStatus) }
    }

    fun stop() {
        pendingEnglish = null
        tts?.stop()
        tts?.shutdown()
        tts = null
    }

    private fun onInitialised(initStatus: Int) {
        val engine = tts ?: return
        if (initStatus != TextToSpeech.SUCCESS) {
            status = SpeakerStatus.UNAVAILABLE
            return
        }
        tamilAvailable = engine.isLanguageAvailable(tamil) >= TextToSpeech.LANG_AVAILABLE
        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = Unit

            override fun onDone(utteranceId: String?) {
                val next = pendingEnglish ?: return
                pendingEnglish = null
                speakNow(next, english)
            }

            @Suppress("OVERRIDE_DEPRECATION")
            override fun onError(utteranceId: String?) {
                pendingEnglish = null
            }
        })
        status = if (tamilAvailable) SpeakerStatus.READY else SpeakerStatus.TAMIL_VOICE_MISSING
    }

    fun speak(phrase: Phrase, mode: LanguageMode) {
        val engine = tts ?: return
        pendingEnglish = null
        engine.stop()
        when {
            tamilAvailable && mode.showsTamil() -> {
                if (mode.showsEnglish()) pendingEnglish = phrase.en
                speakNow(phrase.ta, tamil)
            }
            mode.showsEnglish() -> speakNow(phrase.en, english)
            else -> {
                // Tamil requested but no Tamil voice: fall back to English rather than silence,
                // and let the UI surface tts_tamil_missing.
                speakNow(phrase.en, english)
            }
        }
    }

    private fun speakNow(text: String, locale: Locale) {
        val engine = tts ?: return
        engine.language = locale
        engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_ID)
    }

    private companion object {
        const val UTTERANCE_ID = "saigai-phrase"
    }
}
