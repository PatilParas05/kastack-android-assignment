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
import dev.paraspatil.luminaai.data.sync.SyncManager
import dev.paraspatil.luminaai.data.sync.SyncStatus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val chatDao = AppDatabase.getDatabase(application).chatDao()
    private val chatPipeline = ChatPipeline()
    private val syncManager = SyncManager(application)  // ✅ ADD THIS

    // We expose the ChatState from our State Machine to the UI
    val chatState: StateFlow<ChatState> = chatPipeline.chatState

    // ✅ EXPOSE SYNC STATUS TO UI
    val syncStatus: StateFlow<SyncStatus> = syncManager.syncStatus

    // Keep track of how many items to load for Pagination
    private val currentLimit = MutableStateFlow(20)

    // 1. Load Chat History with dynamic pagination
    @OptIn(ExperimentalCoroutinesApi::class)
    val chatHistory: StateFlow<List<ChatMessage>> = currentLimit
        .flatMapLatest { limit ->
            chatDao.getChatHistory(limit = limit, offset = 0)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
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

    // Called from HomeScreen when the user scrolls to the top
    fun loadMoreMessages() {
        currentLimit.value += 20
    }

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
    }
}