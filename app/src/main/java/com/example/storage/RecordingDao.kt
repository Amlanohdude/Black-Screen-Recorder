package com.example.storage

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordingDao {
    @Query("SELECT * FROM recordings ORDER BY timestamp DESC")
    fun getAllRecordings(): Flow<List<RecordingEntity>>

    @Query("SELECT * FROM recordings WHERE id = :id LIMIT 1")
    suspend fun getRecordingById(id: Long): RecordingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecording(recording: RecordingEntity): Long

    @Update
    suspend fun updateRecording(recording: RecordingEntity)

    @Delete
    suspend fun deleteRecording(recording: RecordingEntity)

    @Query("DELETE FROM recordings WHERE id = :id")
    suspend fun deleteRecordingById(id: Long)

    @Query("DELETE FROM recordings WHERE filePath = :filePath")
    suspend fun deleteRecordingByFilePath(filePath: String)

    @Query("SELECT * FROM recordings WHERE filePath = :filePath LIMIT 1")
    suspend fun getRecordingByFilePath(filePath: String): RecordingEntity?

    @Query("SELECT COUNT(*) FROM recordings WHERE filePath = :filePath")
    suspend fun hasRecordingWithPath(filePath: String): Int

    @Query("SELECT * FROM recordings WHERE timestamp < :cutoffTimestamp")
    suspend fun getRecordingsOlderThan(cutoffTimestamp: Long): List<RecordingEntity>
}
