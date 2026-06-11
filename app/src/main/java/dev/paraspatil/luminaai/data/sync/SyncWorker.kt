package dev.paraspatil.luminaai.data.sync

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dev.paraspatil.luminaai.data.local.AppDatabase
import dev.paraspatil.luminaai.data.local.dataStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        val LAST_SYNCED_KEY = longPreferencesKey("last_synced_at")
    }

    override suspend fun doWork(): Result {
        val database = AppDatabase.getDatabase(applicationContext)
        val chatDao = database.chatDao()
        val dataStore = applicationContext.dataStore

        return try {
            // 1. Get last synced timestamp from DataStore
            val prefs = dataStore.data.first()
            val lastSyncedAt = prefs[LAST_SYNCED_KEY] ?: 0L

            // 2. Fetch only changed rows (unsynced messages) from Room
            val unsyncedMessages = chatDao.getUnsyncedMessages(lastSyncedAt)

            if (unsyncedMessages.isNotEmpty()) {
                // 3. Simulate network sync delay
                delay(2000)

                // Requirement: "On conflict: local wins"
                // Since this is a push to remote, local inherently wins.

                // 4. Update last synced timestamp
                val newSyncTime = System.currentTimeMillis()
                dataStore.edit { preferences ->
                    preferences[LAST_SYNCED_KEY] = newSyncTime
                }
            }

            Result.success()
        } catch (e: Exception) {
            // If network fails or crashes, retry later (WorkManager handles this)
            Result.retry()
        }
    }
}