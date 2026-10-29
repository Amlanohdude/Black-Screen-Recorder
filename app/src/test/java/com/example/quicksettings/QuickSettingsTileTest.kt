package com.example.quicksettings

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.service.quicksettings.TileService
import androidx.test.core.app.ApplicationProvider
import com.example.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class QuickSettingsTileTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun updateAllTiles_runsWithoutCrashing() {
        QuickSettingsTileHelper.updateAllTiles(context)
    }

    @Test
    fun tileStringResources_areValid() {
        assertEquals("Quick Record", context.getString(R.string.tile_quick_record_label))
        assertEquals("Quick Audio", context.getString(R.string.tile_quick_audio_label))
        assertEquals("Tap to record", context.getString(R.string.tile_subtitle_tap_to_record))
        assertEquals("Recording", context.getString(R.string.tile_subtitle_recording))
        assertEquals("Audio REC", context.getString(R.string.tile_subtitle_audio_recording))
        assertEquals("Tap to stop", context.getString(R.string.tile_subtitle_tap_to_stop))
    }

    @Test
    fun quickRecordTileService_isRegisteredInManifestWithPermission() {
        val pm = context.packageManager
        val intent = Intent(TileService.ACTION_QS_TILE).apply {
            setPackage(context.packageName)
        }
        val resolveInfos = pm.queryIntentServices(intent, PackageManager.GET_META_DATA)
        val recordTile = resolveInfos.firstOrNull {
            it.serviceInfo.name == QuickRecordTileService::class.java.name
        }

        assertNotNull("QuickRecordTileService should be registered in AndroidManifest", recordTile)
        assertEquals(
            "android.permission.BIND_QUICK_SETTINGS_TILE",
            recordTile?.serviceInfo?.permission
        )
        assertTrue("QuickRecordTileService must be exported", recordTile?.serviceInfo?.exported == true)
    }

    @Test
    fun quickAudioTileService_isRegisteredInManifestWithPermission() {
        val pm = context.packageManager
        val intent = Intent(TileService.ACTION_QS_TILE).apply {
            setPackage(context.packageName)
        }
        val resolveInfos = pm.queryIntentServices(intent, PackageManager.GET_META_DATA)
        val audioTile = resolveInfos.firstOrNull {
            it.serviceInfo.name == QuickAudioTileService::class.java.name
        }

        assertNotNull("QuickAudioTileService should be registered in AndroidManifest", audioTile)
        assertEquals(
            "android.permission.BIND_QUICK_SETTINGS_TILE",
            audioTile?.serviceInfo?.permission
        )
        assertTrue("QuickAudioTileService must be exported", audioTile?.serviceInfo?.exported == true)
    }

    @Test
    fun quickTrampolineActivity_isRegisteredInManifest() {
        val pm = context.packageManager
        val intent = Intent(context, QuickTrampolineActivity::class.java)
        val resolveInfo = pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)

        assertNotNull("QuickTrampolineActivity should be registered in AndroidManifest", resolveInfo)
    }
}
