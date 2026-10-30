package com.example.storage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StorageCapacityEstimatorTest {

    @Test
    fun estimateSeconds_for4KVideo_calculatesAccurateTime() {
        val oneGigabyte = 4_375_000L * 100 // 100 seconds worth of 4K UHD
        val estimated = StorageCapacityEstimator.getEstimatedRemainingSeconds(
            availableBytes = oneGigabyte,
            recordingType = "VIDEO",
            videoQuality = "4K"
        )
        assertEquals(100L, estimated)
    }

    @Test
    fun estimateSeconds_for1080pVideo_calculatesAccurateTime() {
        val bytes = 1_875_000L * 300 // 300 seconds worth of 1080p
        val estimated = StorageCapacityEstimator.getEstimatedRemainingSeconds(
            availableBytes = bytes,
            recordingType = "VIDEO",
            videoQuality = "1080p"
        )
        assertEquals(300L, estimated)
    }

    @Test
    fun estimateSeconds_forZeroAvailableStorage_returnsZero() {
        val estimated = StorageCapacityEstimator.getEstimatedRemainingSeconds(
            availableBytes = 0L,
            recordingType = "VIDEO",
            videoQuality = "4K"
        )
        assertEquals(0L, estimated)
    }

    @Test
    fun isStorageCriticallyLow_under200MB_returnsTrue() {
        assertTrue(StorageCapacityEstimator.isStorageCriticallyLow(150_000_000L))
    }
}
