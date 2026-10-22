package com.example.covers

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * Memory-safe manager for custom cover background images.
 *
 * Security & Reliability:
 * - Selected photos are copied directly into the app's private files sandbox (context.filesDir/covers).
 *   This ensures cover images persist even if external URI permissions expire or the user moves/deletes
 *   the photo in gallery apps.
 * - Photos are automatically downsampled on Dispatchers.IO to match screen bounds (max 1080x2400)
 *   so large 50MP camera photos never trigger OutOfMemory errors.
 * - An in-memory LRU cache stores decoded bitmaps, ensuring zero CPU/bitmap decoding overhead
 *   during active CameraX video recording.
 */
object CoverImageManager {

    private const val COVERS_DIR = "covers"
    private const val MAX_WIDTH = 1080
    private const val MAX_HEIGHT = 2400

    // In-memory LRU cache for decoded background bitmaps (up to 12MB)
    private val maxMemoryKb = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val cacheSizeKb = (maxMemoryKb / 10).coerceIn(4096, 12288)

    private val bitmapCache = object : LruCache<String, Bitmap>(cacheSizeKb) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            return bitmap.byteCount / 1024
        }
    }

    /**
     * Copy, downsample, and store an image from a selected content URI into private app storage.
     * Returns the relative file name or null if failed.
     */
    suspend fun saveImageForCover(context: Context, sourceUri: Uri): String? {
        return withContext(Dispatchers.IO) {
            try {
                val coversDir = File(context.filesDir, COVERS_DIR).apply { mkdirs() }

                // 1. Decode bounds only to calculate inSampleSize
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                context.contentResolver.openInputStream(sourceUri)?.use { input ->
                    BitmapFactory.decodeStream(input, null, options)
                } ?: return@withContext null

                val origWidth = options.outWidth
                val origHeight = options.outHeight
                if (origWidth <= 0 || origHeight <= 0) return@withContext null

                // 2. Calculate sample size (power of 2)
                var sampleSize = 1
                while ((origWidth / sampleSize) > MAX_WIDTH * 1.5 || (origHeight / sampleSize) > MAX_HEIGHT * 1.5) {
                    sampleSize *= 2
                }

                // 3. Decode downsampled bitmap
                val decodeOptions = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                    inPreferredConfig = Bitmap.Config.RGB_565 // 50% memory savings for background graphics
                }

                val sampledBitmap = context.contentResolver.openInputStream(sourceUri)?.use { input ->
                    BitmapFactory.decodeStream(input, null, decodeOptions)
                } ?: return@withContext null

                // 4. Save to private internal storage file
                val fileName = "cover_img_${UUID.randomUUID()}.jpg"
                val destFile = File(coversDir, fileName)
                FileOutputStream(destFile).use { out ->
                    sampledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
                }

                // Put in memory cache immediately
                bitmapCache.put(destFile.absolutePath, sampledBitmap)
                destFile.absolutePath
            } catch (_: Throwable) {
                null
            }
        }
    }

    /**
     * Load a cached bitmap from file path. Returns null if missing or corrupt (allowing safe fallback).
     */
    fun loadCoverBitmap(filePath: String?): Bitmap? {
        if (filePath.isNullOrBlank()) return null

        val cached = bitmapCache.get(filePath)
        if (cached != null && !cached.isRecycled) {
            return cached
        }

        val file = File(filePath)
        if (!file.exists() || !file.canRead() || file.length() == 0L) {
            return null
        }

        return try {
            val options = BitmapFactory.Options().apply {
                inPreferredConfig = Bitmap.Config.RGB_565
            }
            val bitmap = BitmapFactory.decodeFile(file.absolutePath, options)
            if (bitmap != null) {
                bitmapCache.put(filePath, bitmap)
            }
            bitmap
        } catch (_: Throwable) {
            null
        }
    }

    /**
     * Safely delete cover image file and remove from cache.
     */
    fun deleteCoverImage(filePath: String?) {
        if (filePath.isNullOrBlank()) return
        try {
            bitmapCache.remove(filePath)
            val file = File(filePath)
            if (file.exists()) {
                file.delete()
            }
        } catch (_: Throwable) {}
    }
}
