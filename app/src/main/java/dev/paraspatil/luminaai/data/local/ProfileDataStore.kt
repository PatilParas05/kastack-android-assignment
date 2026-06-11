package dev.paraspatil.luminaai.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

// Extension property to create DataStore instance
val Context.dataStore by preferencesDataStore(name = "user_profile_prefs")

class ProfileDataStore(private val context: Context) {

    private val gson = Gson()

    companion object {
        val NAME_KEY = stringPreferencesKey("user_name")
        val AGE_KEY = stringPreferencesKey("user_age")
        val PHONE_KEY = stringPreferencesKey("user_phone")
        val TRAITS_KEY = stringPreferencesKey("user_traits") // Saved as JSON string
    }

    // Save profile ensuring data is not lost during back navigation (Part 1 Requirement)
    suspend fun saveProfile(profile: UserProfile) {
        context.dataStore.edit { prefs ->
            prefs[NAME_KEY] = profile.name
            prefs[AGE_KEY] = profile.age
            prefs[PHONE_KEY] = profile.phone
            prefs[TRAITS_KEY] = gson.toJson(profile.selectedTraits)
        }
    }

    // Read profile as a Flow to easily bind to Compose UI
    val userProfileFlow: Flow<UserProfile> = context.dataStore.data.map { prefs ->
        val traitsJson = prefs[TRAITS_KEY] ?: "[]"
        val type = object : TypeToken<List<String>>() {}.type
        val traits: List<String> = gson.fromJson(traitsJson, type)

        UserProfile(
            name = prefs[NAME_KEY] ?: "",
            age = prefs[AGE_KEY] ?: "",
            phone = prefs[PHONE_KEY] ?: "",
            selectedTraits = traits
        )
    }
}