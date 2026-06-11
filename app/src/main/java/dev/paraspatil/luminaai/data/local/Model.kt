package dev.paraspatil.luminaai.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.google.gson.Gson

// 1. DataStore Profile Model
data class UserProfile(
    val name: String = "",
    val age: String = "",
    val phone: String = "",
    val selectedTraits: List<String> = emptyList()
)

// 2. Room Entity
@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sender: MessageSender,
    val messageText: String,
    val timestamp: Long = System.currentTimeMillis(),
    val meta: MessageMeta
)

enum class MessageSender {
    USER, ASSISTANT
}

// Custom metadata requiring TypeConverter
data class MessageMeta(
    val isSentSuccessfully: Boolean = true,
    val processingTimeMs: Long = 0L
)

// 3. Room TypeConverter
class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromMessageMeta(meta: MessageMeta): String {
        return gson.toJson(meta)
    }

    @TypeConverter
    fun toMessageMeta(metaString: String): MessageMeta {
        return gson.fromJson(metaString, MessageMeta::class.java)
    }
}