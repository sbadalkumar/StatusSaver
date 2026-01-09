package com.stackstocks.statussaver.presentation.ui.status

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import com.stackstocks.statussaver.core.logging.AppLogger
import com.stackstocks.statussaver.core.logging.LogTags
import com.stackstocks.statussaver.core.utils.FileUtils
import com.stackstocks.statussaver.core.utils.PreferenceUtils
import com.stackstocks.statussaver.core.utils.StatusSaver
import com.stackstocks.statussaver.data.model.StatusModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

// Mutex to prevent concurrent loading operations
private val statusLoadMutex = Mutex()
private val savedStatusLoadMutex = Mutex()

data class StatusGalleryState(
    val statusList: List<StatusModel> = emptyList(),
    val savedStatusList: List<StatusModel> = emptyList(),
    val favoriteList: List<StatusModel> = emptyList(),
    val isLoading: Boolean = true,
    val isLoadingSaved: Boolean = false,
    val errorMessage: String? = null,
    val showStatusView: Boolean = false,
    val selectedStatusIndex: Int = 0,
    val currentTab: Int = 0, // 0 = Statuses, 1 = Saved
    val lastSavedStatusesHash: Int = 0,
    val lastStatusesHash: Int = 0,
    val savedStatusesLoaded: Boolean = false,
    val statusesLoaded: Boolean = false,
    val showDeleteConfirmation: Boolean = false,
    val statusToDelete: StatusModel? = null,
    val statusFilterTab: Int = 0, // 0 = All, 1 = Image, 2 = Video
    val displayStatusList: List<StatusModel> = emptyList(),
    val savedFilterTab: Int = 0, // 0 = Saved, 1 = Favourites
    val showSettingsBottomSheet: Boolean = false,
    val gridColumns: Int = 3,
    val sortOrder: Int = 0 // 0 = Latest first, 1 = Oldest first
)

// Helper functions for filtering and sorting
fun filterStatuses(statuses: List<StatusModel>, filterTab: Int): List<StatusModel> {
    return when (filterTab) {
        0 -> statuses // All
        1 -> statuses.filter { !it.isVideo } // Image only
        2 -> statuses.filter { it.isVideo } // Video only
        else -> statuses
    }
}

fun sortStatuses(statuses: List<StatusModel>, sortOrder: Int): List<StatusModel> {
    return when (sortOrder) {
        0 -> statuses.sortedByDescending { it.lastModified } // Latest first
        1 -> statuses.sortedBy { it.lastModified } // Oldest first
        else -> statuses.sortedByDescending { it.lastModified } // Default: Latest first
    }
}

// Helper functions for calculating hashes
fun calculateSavedStatusesHash(statuses: List<StatusModel>): Int {
    return statuses.hashCode()
}

fun calculateStatusesHash(statuses: List<StatusModel>): Int {
    return statuses.hashCode()
}

// Helper function for video thumbnail generation with timeout protection
suspend fun getVideoThumbnailIO(context: Context, path: String): Bitmap? {
    return withContext(Dispatchers.IO) {
        AppLogger.d(LogTags.THUMBNAIL, "Generating thumbnail for: ${path.takeLast(30)}")
        try {
            // Add timeout to prevent hanging on corrupted videos
            withTimeout(5000) { // 5 second timeout per video thumbnail
                try {
                    val retriever = MediaMetadataRetriever()
                    if (path.startsWith("content://")) {
                        AppLogger.v(LogTags.THUMBNAIL, "Using content:// URI for thumbnail")
                        retriever.setDataSource(context, Uri.parse(path))
                    } else {
                        AppLogger.v(LogTags.THUMBNAIL, "Using file path for thumbnail")
                        retriever.setDataSource(path)
                    }
                    val bitmap = retriever.frameAtTime
                    retriever.release()
                    AppLogger.d(LogTags.THUMBNAIL, "✅ Thumbnail generated successfully")
                    bitmap
                } catch (e: Exception) {
                    AppLogger.e(LogTags.THUMBNAIL, "Error generating video thumbnail", e)
                    null
                }
            }
        } catch (e: TimeoutCancellationException) {
            AppLogger.logTimeout(LogTags.THUMBNAIL, "thumbnail generation for ${path.takeLast(30)}", 5000)
            null
        }
    }
}

// Data loading functions with mutex synchronization and timeout
suspend fun loadSavedStatuses(
    context: Context,
    currentState: StatusGalleryState,
    onStateUpdate: (StatusGalleryState) -> Unit
) {
    val startTime = System.currentTimeMillis()
    AppLogger.logMethodEntry(LogTags.STATUS_LOADING, "loadSavedStatuses")
    
    // Prevent concurrent loading operations
    AppLogger.logMutexLockAttempt(LogTags.STATUS_LOADING, "savedStatusLoadMutex")
    if (!savedStatusLoadMutex.tryLock()) {
        AppLogger.logMutexLockFailed(LogTags.STATUS_LOADING, "savedStatusLoadMutex")
        return
    }
    
    AppLogger.logMutexLockAcquired(LogTags.STATUS_LOADING, "savedStatusLoadMutex")
    
    try {
        AppLogger.logSeparator(LogTags.STATUS_LOADING, "LOADING SAVED STATUSES")
        AppLogger.logLoadingStart(LogTags.STATUS_LOADING, "saved statuses")
        AppLogger.logStateChange(LogTags.STATUS_LOADING, "isLoadingSaved: false", "isLoadingSaved: true")
        onStateUpdate(currentState.copy(isLoadingSaved = true))

        // Add timeout to prevent hanging
        AppLogger.logTimeoutStart(LogTags.STATUS_LOADING, "loadSavedStatuses", 30000)
        withTimeout(30000) { // 30 second timeout
            try {
                AppLogger.d(LogTags.FILE_SYSTEM, "Fetching saved statuses from DCIM folder...")
                val savedStatuses = FileUtils.getSavedStatusesFromFolder(context)
                AppLogger.logLoadingSuccess(LogTags.FILE_SYSTEM, "saved statuses", savedStatuses.size)
                
                AppLogger.d(LogTags.FILE_SYSTEM, "Fetching favorites from favourites folder...")
                val favorites = FileUtils.getFavoriteStatusesFromFolder(context)
                AppLogger.logLoadingSuccess(LogTags.FILE_SYSTEM, "favorites", favorites.size)
                
                // Calculate hash of new statuses
                val newHash = calculateSavedStatusesHash(savedStatuses + favorites)
                AppLogger.d(LogTags.STATUS_LOADING, "Calculated hash: $newHash | Previous hash: ${currentState.lastSavedStatusesHash}")
                
                // Only update state if there are actual changes
                if (newHash != currentState.lastSavedStatusesHash) {
                    AppLogger.i(LogTags.STATUS_LOADING, "Saved statuses changed, updating UI")
                    onStateUpdate(currentState.copy(
                        savedStatusList = savedStatuses,
                        favoriteList = favorites,
                        lastSavedStatusesHash = newHash,
                        isLoadingSaved = false
                    ))
                } else {
                    AppLogger.d(LogTags.STATUS_LOADING, "No changes detected, skipping UI update")
                    onStateUpdate(currentState.copy(isLoadingSaved = false))
                }
                
                AppLogger.logPerformance(LogTags.STATUS_LOADING, "loadSavedStatuses", startTime)
                AppLogger.logMethodExit(LogTags.STATUS_LOADING, "loadSavedStatuses", "success")
            } catch (e: Exception) {
                AppLogger.e(LogTags.ERROR, "Error loading saved statuses", e)
                AppLogger.logLoadingFailure(LogTags.STATUS_LOADING, "saved statuses", e.message ?: "Unknown error")
                onStateUpdate(currentState.copy(isLoadingSaved = false))
            }
        }
    } catch (e: TimeoutCancellationException) {
        AppLogger.logTimeout(LogTags.STATUS_LOADING, "loadSavedStatuses", 30000)
        AppLogger.e(LogTags.TIMEOUT, "Saved status loading timeout after 30 seconds", e)
        onStateUpdate(currentState.copy(
            isLoadingSaved = false,
            errorMessage = "Loading timeout - please try again"
        ))
    } finally {
        savedStatusLoadMutex.unlock()
        AppLogger.logMutexLockReleased(LogTags.STATUS_LOADING, "savedStatusLoadMutex")
    }
}

suspend fun loadStatuses(
    context: Context,
    currentState: StatusGalleryState,
    onStateUpdate: (StatusGalleryState) -> Unit
) {
    val startTime = System.currentTimeMillis()
    AppLogger.logMethodEntry(LogTags.STATUS_LOADING, "loadStatuses")
    
    // Prevent concurrent loading operations
    AppLogger.logMutexLockAttempt(LogTags.STATUS_LOADING, "statusLoadMutex")
    if (!statusLoadMutex.tryLock()) {
        AppLogger.logMutexLockFailed(LogTags.STATUS_LOADING, "statusLoadMutex")
        return
    }
    
    AppLogger.logMutexLockAcquired(LogTags.STATUS_LOADING, "statusLoadMutex")
    
    try {
        AppLogger.logSeparator(LogTags.STATUS_LOADING, "LOADING WHATSAPP STATUSES")
        AppLogger.logLoadingStart(LogTags.STATUS_LOADING, "WhatsApp statuses")
        AppLogger.logStateChange(LogTags.STATUS_LOADING, "isLoading: false", "isLoading: true")
        onStateUpdate(currentState.copy(isLoading = true, errorMessage = null))

        AppLogger.d(LogTags.PREFERENCES, "Retrieving SAF URI from preferences...")
        val pref = PreferenceUtils(context.applicationContext as android.app.Application)
        val safUri = pref.getUriFromPreference()
        AppLogger.i(LogTags.PREFERENCES, "SAF URI retrieved: ${safUri?.take(50) ?: "null"}")

        // Add timeout to prevent hanging
        AppLogger.logTimeoutStart(LogTags.STATUS_LOADING, "loadStatuses", 30000)
        withTimeout(30000) { // 30 second timeout
            try {
                // Only check if SAF URI is present
                if (safUri.isNullOrBlank()) {
                    AppLogger.e(LogTags.ERROR, "❌ NO SAF URI - Cannot load statuses")
                    AppLogger.w(LogTags.PERMISSION, "SAF permission not granted or URI not saved")
                    onStateUpdate(currentState.copy(
                        errorMessage = "Please grant access to the WhatsApp .Statuses folder in onboarding/settings.",
                        isLoading = false
                    ))
                    return@withTimeout
                }

                AppLogger.d(LogTags.FILE_SYSTEM, "Calling FileUtils.getStatus() with URI...")
                val statuses = FileUtils.getStatus(context, safUri ?: "")
                AppLogger.logLoadingSuccess(LogTags.FILE_SYSTEM, "raw statuses", statuses.size)

                AppLogger.d(LogTags.STATUS_LOADING, "Filtering statuses with non-empty paths...")
                val filteredStatuses = statuses.filter { it.filePath.isNotEmpty() }
                    .sortedByDescending { it.lastModified } // Sort by date, latest first
                AppLogger.i(LogTags.STATUS_LOADING, "Filtered to ${filteredStatuses.size} valid statuses")
                
                // Calculate hash of new statuses
                val newHash = calculateStatusesHash(filteredStatuses)
                AppLogger.d(LogTags.STATUS_LOADING, "Calculated hash: $newHash | Previous hash: ${currentState.lastStatusesHash}")
                
                // Only update state if there are actual changes
                if (newHash != currentState.lastStatusesHash) {
                    AppLogger.i(LogTags.STATUS_LOADING, "Statuses changed, updating UI")
                    onStateUpdate(currentState.copy(
                        statusList = filteredStatuses,
                        lastStatusesHash = newHash,
                        statusesLoaded = true,
                        isLoading = false
                    ))
                } else {
                    AppLogger.d(LogTags.STATUS_LOADING, "No changes detected, skipping UI update")
                    onStateUpdate(currentState.copy(
                        statusesLoaded = true,
                        isLoading = false
                    ))
                }
                
                AppLogger.logPerformance(LogTags.STATUS_LOADING, "loadStatuses", startTime)
                AppLogger.logMethodExit(LogTags.STATUS_LOADING, "loadStatuses", "success")

            } catch (e: Exception) {
                AppLogger.e(LogTags.ERROR, "Error loading statuses", e)
                AppLogger.logLoadingFailure(LogTags.STATUS_LOADING, "statuses", e.message ?: "Unknown error")
                onStateUpdate(currentState.copy(
                    errorMessage = e.message,
                    isLoading = false
                ))
            }
        }
    } catch (e: TimeoutCancellationException) {
        AppLogger.logTimeout(LogTags.STATUS_LOADING, "loadStatuses", 30000)
        AppLogger.e(LogTags.TIMEOUT, "Status loading timeout after 30 seconds", e)
        onStateUpdate(currentState.copy(
            errorMessage = "Loading timeout - please try again",
            isLoading = false
        ))
    } finally {
        statusLoadMutex.unlock()
        AppLogger.logMutexLockReleased(LogTags.STATUS_LOADING, "statusLoadMutex")
    }
}

// Force refresh functions
suspend fun forceRefreshStatuses(
    context: Context,
    currentState: StatusGalleryState,
    onStateUpdate: (StatusGalleryState) -> Unit
) {
    val newState = currentState.copy(statusesLoaded = false)
    loadStatuses(context, newState, onStateUpdate)
}

suspend fun forceRefreshSavedStatuses(
    context: Context,
    currentState: StatusGalleryState,
    onStateUpdate: (StatusGalleryState) -> Unit
) {
    val newState = currentState.copy(savedStatusesLoaded = false)
    loadSavedStatuses(context, newState, onStateUpdate)
}

// Favorite management functions
suspend fun markAsFavorite(
    context: Context,
    status: StatusModel,
    currentState: StatusGalleryState,
    onStateUpdate: (StatusGalleryState) -> Unit
) {
    try {
        AppLogger.d(LogTags.STATUS_SAVING, "Marking as favorite: ${status.filePath}")
        
        // Optimistic UI update - immediately move the item from saved to favorites
        val updatedStatus = status.copy(
            filePath = status.filePath.replace(StatusSaver.SAVED_DIRECTORY, StatusSaver.FAVOURITES_DIRECTORY)
        )
        
        // Update the lists optimistically
        val updatedSavedList = currentState.savedStatusList.filter { it.filePath != status.filePath }
        val updatedFavoriteList = currentState.favoriteList + updatedStatus
        
        onStateUpdate(currentState.copy(
            savedStatusList = updatedSavedList,
            favoriteList = updatedFavoriteList
        ))
        
        // Perform the actual file operation
        val success = FileUtils.markAsFavorite(context, status.filePath)
        if (success) {
            AppLogger.i(LogTags.STATUS_SAVING, "✅ Status marked as favorite successfully")
            // No need to refresh - optimistic update already handled it
        } else {
            AppLogger.e(LogTags.ERROR, "❌ Failed to mark as favorite")
            // Revert optimistic update on failure
            onStateUpdate(currentState.copy(
                savedStatusList = currentState.savedStatusList,
                favoriteList = currentState.favoriteList
            ))
        }
    } catch (e: Exception) {
        AppLogger.e(LogTags.ERROR, "❌ Error marking as favorite", e)
        // Revert optimistic update on error
        onStateUpdate(currentState.copy(
            savedStatusList = currentState.savedStatusList,
            favoriteList = currentState.favoriteList
        ))
    }
}

suspend fun unmarkAsFavorite(
    context: Context,
    status: StatusModel,
    currentState: StatusGalleryState,
    onStateUpdate: (StatusGalleryState) -> Unit
) {
    try {
        AppLogger.d(LogTags.STATUS_SAVING, "Unmarking as favorite: ${status.filePath}")
        
        // Optimistic UI update - immediately move the item from favorites to saved
        val updatedStatus = status.copy(
            filePath = status.filePath.replace(StatusSaver.FAVOURITES_DIRECTORY, StatusSaver.SAVED_DIRECTORY)
        )
        
        // Update the lists optimistically
        val updatedSavedList = currentState.savedStatusList + updatedStatus
        val updatedFavoriteList = currentState.favoriteList.filter { it.filePath != status.filePath }
        
        onStateUpdate(currentState.copy(
            savedStatusList = updatedSavedList,
            favoriteList = updatedFavoriteList
        ))
        
        // Perform the actual file operation
        val success = FileUtils.unmarkAsFavorite(context, status.filePath)
        if (success) {
            AppLogger.i(LogTags.STATUS_SAVING, "✅ Status unmarked as favorite successfully")
            // No need to refresh - optimistic update already handled it
        } else {
            AppLogger.e(LogTags.ERROR, "❌ Failed to unmark as favorite")
            // Revert optimistic update on failure
            onStateUpdate(currentState.copy(
                savedStatusList = currentState.savedStatusList,
                favoriteList = currentState.favoriteList
            ))
        }
    } catch (e: Exception) {
        AppLogger.e(LogTags.ERROR, "❌ Error unmarking as favorite", e)
        // Revert optimistic update on error
        onStateUpdate(currentState.copy(
            savedStatusList = currentState.savedStatusList,
            favoriteList = currentState.favoriteList
        ))
    }
} 