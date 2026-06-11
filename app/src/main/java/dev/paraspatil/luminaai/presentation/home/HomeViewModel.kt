package dev.paraspatil.luminaai.presentation.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.paraspatil.luminaai.data.local.AppDatabase
import dev.paraspatil.luminaai.data.local.ChatMessage
import dev.paraspatil.luminaai.data.local.MessageMeta
import dev.paraspatil.luminaai.data.local.MessageSender
import dev.paraspatil.luminaai.domain.pipeline.ChatState
import dev.paraspatil.luminaai.domain.pipeline.ChatPipeline
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val chatDao = AppDatabase.getDatabase(application).chatDao()
    private val chatPipeline = ChatPipeline()

    // We expose the ChatState from our State Machine to the UI
    val chatState: StateFlow<ChatState> = chatPipeline.chatState

    // 1. Load Chat History from Room (Pagination support as requested: Limit 20)
    // For simplicity in this demo, we just fetch the first 20.
    // In a full production app, you'd use Jetpack Paging 3 here.
    val chatHistory: StateFlow<List<ChatMessage>> = chatDao.getChatHistory(limit = 20, offset = 0)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // 2. Send Message through Pipeline and save to Room
    fun sendMessage(text: String) {
        if (text.isBlank()) return

        // Save User's message to Room immediately
        viewModelScope.launch {
            chatDao.insertMessage(
                ChatMessage(
                    sender = MessageSender.USER,
                    messageText = text,
                    meta = MessageMeta()
                )
            )
        }

        // Trigger the State Machine Pipeline
        chatPipeline.sendMessage(text, viewModelScope)

        // We also need to listen for the pipeline's response and save that to Room!
        viewModelScope.launch {
            chatPipeline.chatState.collect { state ->
                if (state is ChatState.Responding) {
                    chatDao.insertMessage(
                        ChatMessage(
                            sender = MessageSender.ASSISTANT,
                            messageText = state.partialMessage,
                            meta = MessageMeta()
                        )
                    )
                }
            }
        }
    }
}