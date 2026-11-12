package com.jarvis.assistant.voice

/**
 * Contract for a future system-integrated hotword implementation.
 * This starter intentionally does not pretend to provide always-on detection.
 * A production implementation must use Android's supported assistant/hotword
 * path where available and must respect microphone/background restrictions.
 */
interface WakeWordEngine {
    val isAvailable: Boolean
    fun start(onWakeWord: (phrase: String) -> Unit)
    fun stop()
}

class NotConfiguredWakeWordEngine : WakeWordEngine {
    override val isAvailable: Boolean = false
    override fun start(onWakeWord: (String) -> Unit) = Unit
    override fun stop() = Unit
}
