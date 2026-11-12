package com.jarvis.assistant.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

class SpeechController(private val context: Context) {
    private var recognizer: SpeechRecognizer? = null

    fun listen(onResult: (String) -> Unit, onFailure: (String) -> Unit) {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onFailure("No speech recognition service is available on this device.")
            return
        }

        stop()
        try {
            recognizer = SpeechRecognizer.createSpeechRecognizer(context).also { speech ->
                speech.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) = Unit
                    override fun onBeginningOfSpeech() = Unit
                    override fun onRmsChanged(rmsdB: Float) = Unit
                    override fun onBufferReceived(buffer: ByteArray?) = Unit
                    override fun onEndOfSpeech() = Unit
                    override fun onEvent(eventType: Int, params: Bundle?) = Unit
                    override fun onPartialResults(partialResults: Bundle?) = Unit

                    override fun onResults(results: Bundle?) {
                        val phrase = results
                            ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            ?.firstOrNull()
                        if (phrase.isNullOrBlank()) {
                            onFailure("I didn't catch that. Please try again.")
                        } else {
                            onResult(phrase)
                        }
                        stop()
                    }

                    override fun onError(error: Int) {
                        val message = when (error) {
                            SpeechRecognizer.ERROR_AUDIO -> "Microphone audio error. Please try again."
                            SpeechRecognizer.ERROR_CLIENT -> "Speech recognition was interrupted. Please try again."
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission is required."
                            SpeechRecognizer.ERROR_NETWORK,
                            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Speech recognition network unavailable. Check your connection."
                            SpeechRecognizer.ERROR_NO_MATCH,
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "I didn't catch that. Please speak and try again."
                            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech recognition is busy. Please try again."
                            SpeechRecognizer.ERROR_SERVER -> "Speech recognition service error. Please try again."
                            else -> "Speech recognition failed (code $error). Please try again."
                        }
                        onFailure(message)
                        stop()
                    }
                })

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "en-IN")
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                }
                speech.startListening(intent)
            }
        } catch (_: Exception) {
            stop()
            onFailure("Couldn't start speech recognition. Check that a speech service is installed and try again.")
        }
    }

    fun stop() {
        recognizer?.let { speech ->
            try {
                speech.cancel()
                speech.destroy()
            } catch (_: Exception) {
                // The recognition service may already have released this instance.
            }
        }
        recognizer = null
    }
}
