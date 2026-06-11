package dev.paraspatil.luminaai.data.sync

import android.content.Context
import androidx.work.WorkInfo
import androidx.work.WorkManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class SyncStatus {
    object Idle : SyncStatus()
    object Syncing : SyncStatus()
    data class Success(val timestamp: Long) : SyncStatus()
    data class Error(val message: String) : SyncStatus()
}

class SyncManager(private val context: Context) {
    private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    init {
        observeSyncStatus()
    }

    private fun observeSyncStatus() {
        val workManager = WorkManager.getInstance(context)
        try {
            workManager.getWorkInfosForUniqueWorkLiveData("LuminaSyncWork")
                .observeForever { workInfoList ->
                    workInfoList.forEach { workInfo ->
                        when (workInfo.state) {
                            WorkInfo.State.RUNNING -> {
                                _syncStatus.value = SyncStatus.Syncing
                            }
                            WorkInfo.State.SUCCEEDED -> {
                                _syncStatus.value = SyncStatus.Success(System.currentTimeMillis())
                            }
                            WorkInfo.State.FAILED -> {
                                _syncStatus.value = SyncStatus.Error("Sync failed. Retrying...")
                            }
                            WorkInfo.State.CANCELLED -> {
                                _syncStatus.value = SyncStatus.Idle
                            }
                            else -> {
                                _syncStatus.value = SyncStatus.Idle
                            }
                        }
                    }
                }
        } catch (e: Exception) {
            _syncStatus.value = SyncStatus.Error("Unable to track sync status")
        }
    }
}