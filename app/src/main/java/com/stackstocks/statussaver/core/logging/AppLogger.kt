package com.stackstocks.statussaver.core.logging

import android.util.Log

/**
 * Centralized logging utility for Status Saver app
 * Provides structured logging with performance tracking and method tracing
 * 
 * Features:
 * - Consistent log formatting
 * - Performance measurement
 * - Method entry/exit tracking
 * - State change logging
 * - Easy to disable in production builds
 */
object AppLogger {
    
    // Enable/disable logging globally
    private const val LOGGING_ENABLED = true
    
    // Enable/disable verbose logs
    private const val VERBOSE_ENABLED = true
    
    // Enable/disable performance logs
    private const val PERFORMANCE_ENABLED = true
    
    /**
     * Debug log
     */
    fun d(tag: String, message: String) {
        if (LOGGING_ENABLED) {
            Log.d(tag, message)
        }
    }
    
    /**
     * Info log
     */
    fun i(tag: String, message: String) {
        if (LOGGING_ENABLED) {
            Log.i(tag, message)
        }
    }
    
    /**
     * Warning log
     */
    fun w(tag: String, message: String) {
        if (LOGGING_ENABLED) {
            Log.w(tag, message)
        }
    }
    
    /**
     * Error log
     */
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        if (LOGGING_ENABLED) {
            if (throwable != null) {
                Log.e(tag, message, throwable)
            } else {
                Log.e(tag, message)
            }
        }
    }
    
    /**
     * Verbose log
     */
    fun v(tag: String, message: String) {
        if (LOGGING_ENABLED && VERBOSE_ENABLED) {
            Log.v(tag, message)
        }
    }
    
    /**
     * Log method entry with parameters
     */
    fun logMethodEntry(tag: String, methodName: String, params: String = "") {
        if (LOGGING_ENABLED && VERBOSE_ENABLED) {
            val paramsStr = if (params.isNotEmpty()) " | Params: $params" else ""
            v(tag, "→ ENTER: $methodName$paramsStr")
        }
    }
    
    /**
     * Log method exit with result
     */
    fun logMethodExit(tag: String, methodName: String, result: String = "") {
        if (LOGGING_ENABLED && VERBOSE_ENABLED) {
            val resultStr = if (result.isNotEmpty()) " | Result: $result" else ""
            v(tag, "← EXIT: $methodName$resultStr")
        }
    }
    
    /**
     * Log performance measurement
     */
    fun logPerformance(tag: String, operation: String, startTime: Long) {
        if (LOGGING_ENABLED && PERFORMANCE_ENABLED) {
            val duration = System.currentTimeMillis() - startTime
            i(LogTags.PERFORMANCE, "[$tag] $operation completed in ${duration}ms")
        }
    }
    
    /**
     * Log state change
     */
    fun logStateChange(tag: String, from: String, to: String) {
        if (LOGGING_ENABLED) {
            d(LogTags.UI_STATE, "[$tag] State: $from → $to")
        }
    }
    
    /**
     * Log data loading start
     */
    fun logLoadingStart(tag: String, dataType: String) {
        if (LOGGING_ENABLED) {
            i(tag, "▶ Loading $dataType started...")
        }
    }
    
    /**
     * Log data loading success
     */
    fun logLoadingSuccess(tag: String, dataType: String, count: Int) {
        if (LOGGING_ENABLED) {
            i(tag, "✅ Loading $dataType completed | Count: $count")
        }
    }
    
    /**
     * Log data loading failure
     */
    fun logLoadingFailure(tag: String, dataType: String, error: String) {
        if (LOGGING_ENABLED) {
            e(tag, "❌ Loading $dataType failed | Error: $error")
        }
    }
    
    /**
     * Log mutex lock attempt
     */
    fun logMutexLockAttempt(tag: String, mutexName: String) {
        if (LOGGING_ENABLED) {
            d(LogTags.MUTEX, "[$tag] Attempting to acquire mutex: $mutexName")
        }
    }
    
    /**
     * Log mutex lock acquired
     */
    fun logMutexLockAcquired(tag: String, mutexName: String) {
        if (LOGGING_ENABLED) {
            d(LogTags.MUTEX, "[$tag] ✅ Mutex acquired: $mutexName")
        }
    }
    
    /**
     * Log mutex lock failed
     */
    fun logMutexLockFailed(tag: String, mutexName: String) {
        if (LOGGING_ENABLED) {
            w(LogTags.MUTEX, "[$tag] ❌ Mutex already locked: $mutexName - operation skipped")
        }
    }
    
    /**
     * Log mutex lock released
     */
    fun logMutexLockReleased(tag: String, mutexName: String) {
        if (LOGGING_ENABLED) {
            d(LogTags.MUTEX, "[$tag] 🔓 Mutex released: $mutexName")
        }
    }
    
    /**
     * Log timeout started
     */
    fun logTimeoutStart(tag: String, operation: String, timeoutMs: Long) {
        if (LOGGING_ENABLED) {
            d(LogTags.TIMEOUT, "[$tag] ⏱️ Starting $operation with ${timeoutMs}ms timeout")
        }
    }
    
    /**
     * Log timeout occurred
     */
    fun logTimeout(tag: String, operation: String, timeoutMs: Long) {
        if (LOGGING_ENABLED) {
            e(LogTags.TIMEOUT, "[$tag] ⏰ TIMEOUT: $operation exceeded ${timeoutMs}ms")
        }
    }
    
    /**
     * Log separator for better readability
     */
    fun logSeparator(tag: String, title: String = "") {
        if (LOGGING_ENABLED) {
            val separator = "=" .repeat(50)
            if (title.isNotEmpty()) {
                d(tag, "$separator\n  $title\n$separator")
            } else {
                d(tag, separator)
            }
        }
    }
    
    /**
     * Log list/collection contents
     */
    fun logCollection(tag: String, collectionName: String, items: Collection<*>) {
        if (LOGGING_ENABLED) {
            d(tag, "$collectionName: [${items.size} items]")
            if (VERBOSE_ENABLED && items.isNotEmpty()) {
                items.forEachIndexed { index, item ->
                    v(tag, "  [$index] $item")
                }
            }
        }
    }
}

