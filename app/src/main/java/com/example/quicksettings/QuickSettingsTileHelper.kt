package com.example.quicksettings

import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.service.quicksettings.TileService
import android.util.Log

object QuickSettingsTileHelper {
    private const val TAG = "QuickSettingsTileHelper"

    fun updateAllTiles(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                TileService.requestListeningState(
                    context,
                    ComponentName(context, QuickRecordTileService::class.java)
                )
                TileService.requestListeningState(
                    context,
                    ComponentName(context, QuickAudioTileService::class.java)
                )
            } catch (e: Exception) {
                Log.w(TAG, "Could not request tile listening state", e)
            }
        }
    }
}
