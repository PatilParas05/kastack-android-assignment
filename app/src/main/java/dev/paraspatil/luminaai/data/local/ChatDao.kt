package dev.paraspatil.luminaai.data.local


import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    // Flow-based query with pagination support (20 at a time) as requested in Part 2
    @Query("SELECT * FROM chat_messages ORDER BY timestamp DESC LIMIT :limit OFFSET :offset")
    fun getChatHistory(limit: Int = 20, offset: Int = 0): Flow<List<ChatMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessage)

    // Used for the SyncManager WorkManager (Part 4)
    @Query("SELECT * FROM chat_messages WHERE timestamp > :lastSyncedAt")
    suspend fun getUnsyncedMessages(lastSyncedAt: Long): List<ChatMessage>
}