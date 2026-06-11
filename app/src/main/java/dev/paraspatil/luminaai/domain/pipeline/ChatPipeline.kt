package dev.paraspatil.luminaai.domain.pipeline

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ChatPipeline(
    private val processor: suspend (String) -> String = { message ->
        // Simulates an AI/Network processing delay
        // Random delay between 2 and 4 seconds for standard messages
        delay((2000..4000).random().toLong())
        "Echo: $message"
    }
) {

    private val _chatState = MutableStateFlow<ChatState>(ChatState.Idle)
    val chatState: StateFlow<ChatState> = _chatState.asStateFlow()

    private var currentMessageJob: Job? = null

    /**
     * @param message The user's text message
     * @param scope Passed from ViewModel (viewModelScope) so it survives configuration changes
     */
    fun sendMessage(message: String, scope: CoroutineScope) {
        // REQUIREMENT: If user sends another message while in Processing/Responding,
        // cancel current job and restart pipeline.
        currentMessageJob?.cancel()

        currentMessageJob = scope.launch {
            try {
                // State 1: Typing
                _chatState.value = ChatState.Typing
                delay(300)

                // State 2: Validating
                _chatState.value = ChatState.Validating
                if (message.isBlank()) {
                    _chatState.value = ChatState.Error("Message cannot be empty")
                    return@launch
                }
                delay(300)

                // State 3: Processing (Requires 8-second timeout)
                _chatState.value = ChatState.Processing

                // withTimeoutOrNull will return null if the block takes longer than 8000ms
                val response = withTimeoutOrNull(8000L) {
                    processor(message)
                }

                if (response == null) {
                    // Timeout exceeded 8 seconds!
                    _chatState.value = ChatState.Error("Timeout exceeded 8 seconds. Please retry.")
                    return@launch
                }

                // State 4: Responding
                _chatState.value = ChatState.Responding(response)
                delay(1500) // Give UI time to show response

                // State 5: Back to Idle
                _chatState.value = ChatState.Idle

            } catch (e: CancellationException) {
                // Job was cancelled by a new message, expected behavior.
                throw e
            } catch (e: Exception) {
                _chatState.value = ChatState.Error(e.message ?: "Unknown error occurred")
            }
        }
    }
}