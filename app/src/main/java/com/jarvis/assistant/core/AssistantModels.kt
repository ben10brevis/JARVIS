package com.jarvis.assistant.core

enum class RequestSource { WAKE_WORD, BUTTON, NOTIFICATION, TEXT }

data class AssistantRequest(
    val text: String,
    val source: RequestSource = RequestSource.BUTTON,
    val languageTag: String? = null
)

data class AssistantResponse(
    val text: String,
    val status: ResponseStatus = ResponseStatus.INFO,
    val actionLabel: String? = null
)

enum class ResponseStatus { INFO, SUCCESS, NEEDS_CONFIRMATION, ERROR }
