package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MeasurementDao {

    @Query("SELECT * FROM measurement_history ORDER BY timestamp DESC")
    fun getAllMeasurements(): Flow<List<MeasurementRecord>>

    @Query("SELECT * FROM measurement_history WHERE type = :type ORDER BY timestamp DESC")
    fun getMeasurementsByType(type: String): Flow<List<MeasurementRecord>>

    @Query("SELECT * FROM measurement_history WHERE id = :id LIMIT 1")
    suspend fun getMeasurementById(id: Long): MeasurementRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: MeasurementRecord): Long

    @Delete
    suspend fun delete(record: MeasurementRecord)

    @Query("DELETE FROM measurement_history WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM measurement_history")
    suspend fun clearAll()
}
