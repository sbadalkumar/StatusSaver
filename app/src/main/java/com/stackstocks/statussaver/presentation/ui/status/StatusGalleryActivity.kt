package com.stackstocks.statussaver.presentation.ui.status

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.stackstocks.statussaver.core.logging.AppLogger
import com.stackstocks.statussaver.core.logging.LogTags
import com.stackstocks.statussaver.core.utils.NavigationManager
import com.stackstocks.statussaver.core.utils.PreferenceUtils
import com.stackstocks.statussaver.core.utils.StorageAccessHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StatusGalleryActivity : ComponentActivity() {

    companion object {
        private const val TAG = "StatusGalleryActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        AppLogger.logSeparator(LogTags.ACTIVITY, "STATUS GALLERY ACTIVITY STARTED")
        AppLogger.i(LogTags.ACTIVITY, "Android Version: SDK ${android.os.Build.VERSION.SDK_INT}")
        AppLogger.i(LogTags.ACTIVITY, "Device: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
        
        // Show loading UI immediately
        AppLogger.d(LogTags.ACTIVITY, "Setting initial loading content...")
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black
                ) {
                    // Show a simple loading screen while checking navigation
                    androidx.compose.material3.CircularProgressIndicator(
                        modifier = Modifier.fillMaxSize(),
                        color = androidx.compose.ui.graphics.Color.White
                    )
                }
            }
        }
        
        // Check navigation requirements in background
        AppLogger.d(LogTags.NAVIGATION, "Launching navigation check coroutine...")
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Check if we should show privacy policy
                AppLogger.d(LogTags.NAVIGATION, "Checking privacy policy acceptance...")
                val shouldShowPrivacyPolicy = NavigationManager.shouldShowPrivacyPolicy(this@StatusGalleryActivity)
                if (shouldShowPrivacyPolicy) {
                    AppLogger.w(LogTags.NAVIGATION, "Privacy policy not accepted - navigating to PrivacyPolicyActivity")
                    withContext(Dispatchers.Main) {
                        NavigationManager.navigateToNextActivity(this@StatusGalleryActivity)
                        finish()
                    }
                    return@launch
                }
                AppLogger.d(LogTags.NAVIGATION, "✅ Privacy policy accepted")
                
                // Check if we should show onboarding
                AppLogger.d(LogTags.NAVIGATION, "Checking onboarding completion...")
                val shouldShowOnboarding = NavigationManager.shouldShowOnboarding(this@StatusGalleryActivity)
                if (shouldShowOnboarding) {
                    AppLogger.w(LogTags.NAVIGATION, "Onboarding not completed - navigating to OnBoardingActivity")
                    withContext(Dispatchers.Main) {
                        NavigationManager.navigateToNextActivity(this@StatusGalleryActivity)
                        finish()
                    }
                    return@launch
                }
                AppLogger.d(LogTags.NAVIGATION, "✅ Onboarding completed")

                AppLogger.i(LogTags.ACTIVITY, "✅ All navigation checks passed - showing status gallery")
                
                // All checks passed, show status gallery
                withContext(Dispatchers.Main) {
                    AppLogger.d(LogTags.ACTIVITY, "Setting StatusGallery content...")
                    setContent {
                        MaterialTheme {
                            Surface(
                                modifier = Modifier.fillMaxSize(),
                                color = Color.Black
                            ) {
                                StandaloneStatusGallery(this@StatusGalleryActivity)
                            }
                        }
                    }
                    AppLogger.i(LogTags.ACTIVITY, "✅ StatusGallery content set successfully")
                }
            } catch (e: Exception) {
                AppLogger.e(LogTags.ERROR, "Error during navigation check", e)
                withContext(Dispatchers.Main) {
                    // Show error state or fallback
                    setContent {
                        MaterialTheme {
                            Surface(
                                modifier = Modifier.fillMaxSize(),
                                color = Color.Black
                            ) {
                                androidx.compose.foundation.layout.Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = androidx.compose.ui.Alignment.Center
                                ) {
                                    androidx.compose.material3.Text(
                                        text = "Error loading app",
                                        color = androidx.compose.ui.graphics.Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
} 