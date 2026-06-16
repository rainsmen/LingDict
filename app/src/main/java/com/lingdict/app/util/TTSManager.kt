package com.lingdict.app.util

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

enum class TTSState {
    UNINITIALIZED,
    INITIALIZING,
    READY,
    ERROR,
    SPEAKING
}

@Singleton
class TTSManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var tts: TextToSpeech? = null

    private val _state = MutableStateFlow(TTSState.UNINITIALIZED)
    val state: StateFlow<TTSState> = _state.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    init {
        initialize()
    }

    private fun initialize() {
        if (_state.value != TTSState.UNINITIALIZED) return

        _state.value = TTSState.INITIALIZING

        tts = TextToSpeech(context) { status ->
            when (status) {
                TextToSpeech.SUCCESS -> {
                    tts?.let { engine ->
                        val result = engine.setLanguage(Locale.US)
                        if (result == TextToSpeech.LANG_MISSING_DATA ||
                            result == TextToSpeech.LANG_NOT_SUPPORTED) {
                            _state.value = TTSState.ERROR
                        } else {
                            engine.setSpeechRate(0.9f) // Slightly slower for clarity
                            engine.setPitch(1.0f)
                            setupProgressListener(engine)
                            _state.value = TTSState.READY
                        }
                    }
                }
                else -> {
                    _state.value = TTSState.ERROR
                }
            }
        }
    }

    private fun setupProgressListener(engine: TextToSpeech) {
        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                _isSpeaking.value = true
                _state.value = TTSState.SPEAKING
            }

            override fun onDone(utteranceId: String?) {
                _isSpeaking.value = false
                _state.value = TTSState.READY
            }

            override fun onError(utteranceId: String?) {
                _isSpeaking.value = false
                _state.value = TTSState.ERROR
            }
        })
    }

    /**
     * Speak the given text
     * @param text Text to speak
     * @param utteranceId Unique identifier for this utterance
     */
    fun speak(text: String, utteranceId: String = UUID.randomUUID().toString()) {
        if (_state.value != TTSState.READY && _state.value != TTSState.SPEAKING) {
            // Try to reinitialize if not ready
            if (_state.value == TTSState.ERROR || _state.value == TTSState.UNINITIALIZED) {
                initialize()
            }
            return
        }

        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    /**
     * Stop current speech
     */
    fun stop() {
        tts?.stop()
        _isSpeaking.value = false
        if (_state.value == TTSState.SPEAKING) {
            _state.value = TTSState.READY
        }
    }

    /**
     * Check if TTS is available on this device
     */
    fun isAvailable(): Boolean {
        return _state.value == TTSState.READY || _state.value == TTSState.SPEAKING
    }

    /**
     * Release TTS resources
     */
    fun release() {
        stop()
        tts?.shutdown()
        tts = null
        _state.value = TTSState.UNINITIALIZED
    }

    /**
     * Retry initialization if failed
     */
    fun retry() {
        if (_state.value == TTSState.ERROR) {
            release()
            initialize()
        }
    }
}
