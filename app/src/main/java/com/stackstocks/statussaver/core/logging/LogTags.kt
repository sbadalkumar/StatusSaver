package com.stackstocks.statussaver.core.logging

/**
 * Centralized log tags for Status Saver app
 * All tags start with SS_ prefix for easy filtering in Logcat
 * 
 * Usage in Logcat: Filter by "SS_" to see all app logs
 */
object LogTags {
    
    // Application Level
    const val APP = "SS_APP"
    const val APP_LIFECYCLE = "SS_APP_LIFECYCLE"
    const val LIFECYCLE = "SS_LIFECYCLE"
    
    // Presentation Layer
    const val ACTIVITY = "SS_ACTIVITY"
    const val SCREEN = "SS_SCREEN"
    const val VIEWMODEL = "SS_VIEWMODEL"
    const val UI_STATE = "SS_UI_STATE"
    const val UI_EVENT = "SS_UI_EVENT"
    const val COMPOSE = "SS_COMPOSE"
    
    // Domain Layer
    const val USE_CASE = "SS_USE_CASE"
    const val REPOSITORY = "SS_REPOSITORY"
    const val ENTITY = "SS_ENTITY"
    
    // Data Layer
    const val DATA_SOURCE = "SS_DATA_SOURCE"
    const val STORAGE = "SS_STORAGE"
    const val SAF = "SS_SAF"
    const val MEDIASTORE = "SS_MEDIASTORE"
    const val FILE_SYSTEM = "SS_FILE_SYSTEM"
    
    // Features
    const val STATUS_LOADING = "SS_STATUS_LOADING"
    const val STATUS_SAVING = "SS_STATUS_SAVING"
    const val STATUS_SHARING = "SS_STATUS_SHARING"
    const val STATUS_DELETING = "SS_STATUS_DELETING"
    
    // Media Processing
    const val VIDEO = "SS_VIDEO"
    const val IMAGE = "SS_IMAGE"
    const val THUMBNAIL = "SS_THUMBNAIL"
    const val MEDIA_PROCESSING = "SS_MEDIA_PROCESSING"
    
    // System
    const val PERMISSION = "SS_PERMISSION"
    const val NAVIGATION = "SS_NAVIGATION"
    const val PREFERENCES = "SS_PREFERENCES"
    const val FILE_OPS = "SS_FILE_OPS"
    
    // Performance & Concurrency
    const val PERFORMANCE = "SS_PERFORMANCE"
    const val MUTEX = "SS_MUTEX"
    const val TIMEOUT = "SS_TIMEOUT"
    const val COROUTINE = "SS_COROUTINE"
    
    // Error Handling
    const val ERROR = "SS_ERROR"
    const val EXCEPTION = "SS_EXCEPTION"
    const val CRASH = "SS_CRASH"
    
    // Network & Firebase
    const val NETWORK = "SS_NETWORK"
    const val FIREBASE = "SS_FIREBASE"
    const val ANALYTICS = "SS_ANALYTICS"
    
    // Debug & Testing
    const val DEBUG = "SS_DEBUG"
    const val TEST = "SS_TEST"
}

