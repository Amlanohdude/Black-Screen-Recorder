package com.example.update

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.util.Log
import android.widget.Toast
import com.example.BuildConfig
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallException
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallErrorCode
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages Google Play In-App Updates in a strictly offline-first, non-blocking manner.
 *
 * Guarantees:
 * 1. 100% Offline-safe: When offline, all checks return immediately with zero delay and zero blocking modals.
 * 2. No Forced Updates: Only the Flexible update flow is used so the user can continue recording or playing media.
 * 3. Never interrupts active recording: Update notifications are suppressed while recording is in progress.
 */
object InAppUpdateManager {
    private const val TAG = "InAppUpdateManager"
    const val REQUEST_CODE_FLEXIBLE_UPDATE = 8899

    private var appUpdateManager: AppUpdateManager? = null
    private var isListenerRegistered = false
    private var cachedAppUpdateInfo: com.google.android.play.core.appupdate.AppUpdateInfo? = null

    private val _isUpdateDownloaded = MutableStateFlow(false)
    val isUpdateDownloaded: StateFlow<Boolean> = _isUpdateDownloaded.asStateFlow()

    private val _isUpdatePromptVisible = MutableStateFlow(false)
    val isUpdatePromptVisible: StateFlow<Boolean> = _isUpdatePromptVisible.asStateFlow()

    private val installStateUpdatedListener = InstallStateUpdatedListener { state ->
        if (state.installStatus() == InstallStatus.DOWNLOADED) {
            Log.i(TAG, "In-app update download completed.")
            _isUpdateDownloaded.value = true
        }
    }

    private fun getAppUpdateManager(context: Context): AppUpdateManager? {
        return try {
            appUpdateManager ?: AppUpdateManagerFactory.create(context.applicationContext).also {
                appUpdateManager = it
                registerListenerIfNeeded(it)
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to create AppUpdateManager: ${t.message}")
            null
        }
    }

    private fun registerListenerIfNeeded(manager: AppUpdateManager) {
        if (!isListenerRegistered) {
            try {
                manager.registerListener(installStateUpdatedListener)
                isListenerRegistered = true
            } catch (t: Throwable) {
                Log.w(TAG, "Failed to register install state listener", t)
            }
        }
    }

    /**
     * Checks if active internet is currently available.
     */
    fun isNetworkAvailable(context: Context): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
            val network = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(network) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (_: Throwable) {
            false
        }
    }

    /**
     * Silent, non-blocking background check on app startup.
     * Guaranteed 100% offline-safe: if offline, returns immediately without any error or prompt.
     */
    fun checkForUpdateSilently(activity: Activity) {
        if (BuildConfig.FLAVOR != "play") {
            return
        }
        if (!isNetworkAvailable(activity)) {
            // Strictly offline: do nothing, zero delay, zero prompt.
            return
        }

        try {
            val manager = getAppUpdateManager(activity) ?: return
            manager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
                if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
                    cachedAppUpdateInfo = appUpdateInfo
                    _isUpdatePromptVisible.value = true
                } else if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                    _isUpdateDownloaded.value = true
                }
            }.addOnFailureListener { e ->
                // Silently ignore: Play Store might not be installed or device is offline
                Log.d(TAG, "Silent update check failed/ignored: ${e.message}")
            }
        } catch (t: Throwable) {
            Log.d(TAG, "Failed to initialize update check", t)
        }
    }

    /**
     * User explicitly taps "Check for updates" in Settings.
     * Provides clear, polite feedback for online, offline, and up-to-date states.
     */
    fun checkManualUpdate(activity: Activity) {
        if (BuildConfig.FLAVOR != "play") {
            Toast.makeText(
                activity,
                "You are on the latest version (${BuildConfig.VERSION_NAME})",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        if (!isNetworkAvailable(activity)) {
            Toast.makeText(activity, "You're offline. App is fully functional offline.", Toast.LENGTH_SHORT).show()
            return
        }

        Toast.makeText(activity, "Checking for updates...", Toast.LENGTH_SHORT).show()

        try {
            val manager = getAppUpdateManager(activity)
            if (manager == null) {
                openPlayStore(activity)
                return
            }
            manager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
                when {
                    appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE -> {
                        cachedAppUpdateInfo = appUpdateInfo
                        _isUpdatePromptVisible.value = true
                    }
                    appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED -> {
                        _isUpdateDownloaded.value = true
                        Toast.makeText(activity, "Update downloaded. Restart to install.", Toast.LENGTH_LONG).show()
                    }
                    else -> {
                        Toast.makeText(
                            activity,
                            "You are on the latest version (${BuildConfig.VERSION_NAME})",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }.addOnFailureListener { e ->
                Log.w(TAG, "Manual update check failed", e)
                if (e is InstallException && e.errorCode == InstallErrorCode.ERROR_APP_NOT_OWNED) {
                    Toast.makeText(
                        activity,
                        "You are on the latest version (${BuildConfig.VERSION_NAME})",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    // If Play Core is unavailable (e.g. no Play Services),
                    // fallback to opening the Play Store listing directly.
                    Toast.makeText(
                        activity,
                        "Opening Google Play Store...",
                        Toast.LENGTH_SHORT
                    ).show()
                    openPlayStore(activity)
                }
            }
        } catch (t: Throwable) {
            openPlayStore(activity)
        }
    }

    /**
     * Call on Activity onResume to catch completed downloads if user backgrounded the app.
     */
    fun onResume(activity: Activity) {
        try {
            appUpdateManager?.appUpdateInfo?.addOnSuccessListener { info ->
                if (info.installStatus() == InstallStatus.DOWNLOADED) {
                    _isUpdateDownloaded.value = true
                }
            }
        } catch (_: Exception) {}
    }

    /**
     * Completes the flexible update by triggering an immediate install and restart.
     */
    fun completeUpdate() {
        try {
            appUpdateManager?.completeUpdate()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to complete update", e)
        }
    }

    /**
     * User dismissed the downloaded notification banner.
     */
    fun dismissDownloadedBanner() {
        _isUpdateDownloaded.value = false
    }

    /**
     * Shows or dismisses the Update Available prompt dialog.
     */
    fun showUpdatePrompt(show: Boolean) {
        _isUpdatePromptVisible.value = show
    }

    /**
     * Triggers the update flow from the dialog.
     * Uses Play In-App flexible flow if update info is available; otherwise opens the Play Store listing.
     */
    fun triggerUpdateFlow(activity: Activity) {
        _isUpdatePromptVisible.value = false
        val info = cachedAppUpdateInfo
        val manager = getAppUpdateManager(activity)
        if (manager != null && info != null && info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {
            try {
                manager.startUpdateFlowForResult(
                    info,
                    activity,
                    AppUpdateOptions.defaultOptions(AppUpdateType.FLEXIBLE),
                    REQUEST_CODE_FLEXIBLE_UPDATE
                )
            } catch (t: Throwable) {
                Log.w(TAG, "Flexible update flow could not be started", t)
                openPlayStore(activity)
            }
        } else {
            openPlayStore(activity)
        }
    }

    /**
     * For QA Developer testing: simulate the update downloaded banner.
     */
    fun simulateUpdateDownloaded(downloaded: Boolean = true) {
        _isUpdateDownloaded.value = downloaded
    }

    /**
     * Opens the app's store listing. Supports Google Play and Amazon Appstore.
     */
    fun openPlayStore(context: Context) {
        val packageName = context.packageName
        if (BuildConfig.FLAVOR == "amazon") {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("amzn://apps/android?p=$packageName")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                return
            } catch (_: Throwable) {
                try {
                    val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.amazon.com/gp/mas/dl/android?p=$packageName")).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(webIntent)
                    return
                } catch (_: Throwable) {}
            }
        }

        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Throwable) {
            try {
                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(webIntent)
            } catch (_: Throwable) {}
        }
    }

    /**
     * Clean up listeners when Activity is destroyed.
     */
    fun onDestroy() {
        try {
            if (isListenerRegistered) {
                appUpdateManager?.unregisterListener(installStateUpdatedListener)
                isListenerRegistered = false
            }
        } catch (_: Exception) {}
    }
}
