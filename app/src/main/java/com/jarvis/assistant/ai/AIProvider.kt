package com.jarvis.assistant.ai

import com.jarvis.assistant.core.AssistantRequest
import com.jarvis.assistant.core.AssistantResponse

/** Implement this interface with a trusted backend. Never put a provider secret in the APK. */
interface AIProvider {
    suspend fun respond(request: AssistantRequest): AssistantResponse
}

class NotConfiguredAIProvider : AIProvider {
    override suspend fun respond(request: AssistantRequest) = AssistantResponse(
        text = "Online AI is not connected yet. Local phone commands are available; configure a secure backend to enable AI conversations."
    )
}
