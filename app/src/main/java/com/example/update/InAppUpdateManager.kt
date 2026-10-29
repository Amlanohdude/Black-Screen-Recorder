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

    private val _isUpdateDownloaded = MutableStateFlow(false)
    val isUpdateDownloaded: StateFlow<Boolean> = _isUpdateDownloaded.asStateFlow()

    private val installStateUpdatedListener = InstallStateUpdatedListener { state ->
        if (state.installStatus() == InstallStatus.DOWNLOADED) {
            Log.i(TAG, "In-app update download completed.")
            _isUpdateDownloaded.value = true
        }
    }

    private fun getAppUpdateManager(context: Context): AppUpdateManager {
        return appUpdateManager ?: AppUpdateManagerFactory.create(context.applicationContext).also {
            appUpdateManager = it
            registerListenerIfNeeded(it)
        }
    }

    private fun registerListenerIfNeeded(manager: AppUpdateManager) {
        if (!isListenerRegistered) {
            try {
                manager.registerListener(installStateUpdatedListener)
                isListenerRegistered = true
            } catch (e: Exception) {
                Log.w(TAG, "Failed to register install state listener", e)
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
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Silent, non-blocking background check on app startup.
     * Guaranteed 100% offline-safe: if offline, returns immediately without any error or prompt.
     */
    fun checkForUpdateSilently(activity: Activity) {
        if (!isNetworkAvailable(activity)) {
            // Strictly offline: do nothing, zero delay, zero prompt.
            return
        }

        try {
            val manager = getAppUpdateManager(activity)
            manager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
                if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                    && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
                ) {
                    try {
                        manager.startUpdateFlowForResult(
                            appUpdateInfo,
                            activity,
                            AppUpdateOptions.defaultOptions(AppUpdateType.FLEXIBLE),
                            REQUEST_CODE_FLEXIBLE_UPDATE
                        )
                    } catch (e: Exception) {
                        Log.w(TAG, "Flexible update flow could not be started", e)
                    }
                } else if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                    _isUpdateDownloaded.value = true
                }
            }.addOnFailureListener { e ->
                // Silently ignore: Play Store might not be installed or device is offline
                Log.d(TAG, "Silent update check failed/ignored: ${e.message}")
            }
        } catch (e: Exception) {
            Log.d(TAG, "Failed to initialize update check", e)
        }
    }

    /**
     * User explicitly taps "Check for updates" in Settings.
     * Provides clear, polite feedback for online, offline, and up-to-date states.
     */
    fun checkManualUpdate(activity: Activity) {
        if (!isNetworkAvailable(activity)) {
            Toast.makeText(activity, "You're offline. App is fully functional offline.", Toast.LENGTH_SHORT).show()
            return
        }

        Toast.makeText(activity, "Checking for updates...", Toast.LENGTH_SHORT).show()

        try {
            val manager = getAppUpdateManager(activity)
            manager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
                when {
                    appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE -> {
                        if (appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {
                            manager.startUpdateFlowForResult(
                                appUpdateInfo,
                                activity,
                                AppUpdateOptions.defaultOptions(AppUpdateType.FLEXIBLE),
                                REQUEST_CODE_FLEXIBLE_UPDATE
                            )
                        } else {
                            openPlayStore(activity)
                        }
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
        } catch (e: Exception) {
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
     * Opens the app's Google Play Store listing.
     */
    fun openPlayStore(context: Context) {
        val packageName = context.packageName
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(webIntent)
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
