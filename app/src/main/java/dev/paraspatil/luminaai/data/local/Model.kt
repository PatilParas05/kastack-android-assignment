package dev.paraspatil.luminaai.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.google.gson.Gson

// ============ DATASTORE MODEL ============
// 1. DataStore Profile Model (used for onboarding persistence)
data class UserProfile(
    val name: String = "",
    val age: String = "",
    val phone: String = "",
    val selectedTraits: List<String> = emptyList()
)

// ============ ROOM ENTITIES ============
// 2. Room Entity for User Profile
@Entity(tableName = "user_profiles")
data class UserProfileEntity(
    @PrimaryKey val id: Int = 1,  // Single user profile
    val name: String,
    val age: String,
    val phone: String,
    val selectedTraits: String,  // JSON serialized list
    val lastUpdated: Long = System.currentTimeMillis()
)

// 3. Chat Message Entity
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

// 4. Reminder Entity
@Entity(tableName = "reminders")
data class Reminder(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val message: String,
    val scheduledTime: Long,
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

// ============ CUSTOM METADATA ============
// Custom metadata requiring TypeConverter
data class MessageMeta(
    val isSentSuccessfully: Boolean = true,
    val processingTimeMs: Long = 0L
)

// ============ TYPE CONVERTERS ============
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

    // ✅ MessageSender Enum Converter
    @TypeConverter
    fun fromMessageSender(sender: MessageSender): String {
        return sender.name
    }

    @TypeConverter
    fun toMessageSender(senderString: String): MessageSender {
        return MessageSender.valueOf(senderString)
    }

    // ✅ List<String> Converter for traits
    @TypeConverter
    fun fromStringList(list: List<String>): String {
        return gson.toJson(list)
    }

    @TypeConverter
    fun toStringList(json: String): List<String> {
        return gson.fromJson(json, Array<String>::class.java).toList()
    }
}