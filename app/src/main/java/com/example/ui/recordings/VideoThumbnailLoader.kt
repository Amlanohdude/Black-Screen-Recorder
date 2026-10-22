package com.example.ui.recordings

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.os.Build
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Memory-safe asynchronous video thumbnail loader and in-memory LRU cache.
 */
object VideoThumbnailLoader {

    private val maxMemoryKb = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val cacheSizeKb = (maxMemoryKb / 8).coerceIn(4096, 16384)

    private val memoryCache = object : LruCache<String, Bitmap>(cacheSizeKb) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            return bitmap.byteCount / 1024
        }
    }

    suspend fun loadThumbnail(filePath: String): Bitmap? {
        val cached = memoryCache.get(filePath)
        if (cached != null && !cached.isRecycled) {
            return cached
        }

        return withContext(Dispatchers.IO) {
            val file = File(filePath)
            if (!file.exists() || !file.canRead() || file.length() == 0L) {
                return@withContext null
            }

            var retriever: MediaMetadataRetriever? = null
            try {
                retriever = MediaMetadataRetriever()
                retriever.setDataSource(filePath)

                val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                    retriever.getScaledFrameAtTime(
                        1_000_000L,
                        MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                        240,
                        160
                    ) ?: retriever.getScaledFrameAtTime(
                        0L,
                        MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                        240,
                        160
                    ) ?: retriever.frameAtTime
                } else {
                    retriever.getFrameAtTime(1_000_000L) ?: retriever.getFrameAtTime(0L) ?: retriever.frameAtTime
                }

                if (bitmap != null) {
                    val scaled = if (bitmap.width <= 260 && bitmap.height <= 180) {
                        bitmap
                    } else {
                        val s = Bitmap.createScaledBitmap(bitmap, 240, 160, true)
                        if (s != bitmap) {
                            bitmap.recycle()
                        }
                        s
                    }
                    memoryCache.put(filePath, scaled)
                    return@withContext scaled
                }
            } catch (_: Throwable) {
            } finally {
                try {
                    retriever?.release()
                } catch (_: Throwable) {
                }
            }
            null
        }
    }

    fun remove(filePath: String) {
        memoryCache.remove(filePath)
    }

    fun clearCache() {
        memoryCache.evictAll()
    }
}
