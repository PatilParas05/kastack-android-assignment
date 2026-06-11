package dev.paraspatil.luminaai.domain

sealed interface ChatState {
    object Idle : ChatState
    object Typing : ChatState
    object Validating : ChatState
    object Processing : ChatState
    data class Responding(val partialMessage: String) : ChatState
    data class Error(val errorMessage: String) : ChatState
}