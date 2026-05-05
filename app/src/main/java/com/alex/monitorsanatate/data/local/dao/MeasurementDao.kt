package com.alex.monitorsanatate.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.alex.monitorsanatate.data.local.entity.EcgDataPointEntity
import com.alex.monitorsanatate.data.local.entity.MeasurementEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MeasurementDao {

    @Query("SELECT * FROM measurements WHERE userId = :userId ORDER BY startTime DESC")
    fun getMeasurementsByUserId(userId: String): Flow<List<MeasurementEntity>>

    @Query("SELECT * FROM measurements WHERE id = :id")
    suspend fun getMeasurementById(id: Long): MeasurementEntity?

    @Query("SELECT * FROM ecg_data_points WHERE measurementId = :measurementId ORDER BY timestampOffset")
    suspend fun getEcgDataForMeasurement(measurementId: Long): List<EcgDataPointEntity>

    @Insert
    suspend fun insertMeasurement(measurement: MeasurementEntity): Long

    @Insert
    suspend fun insertEcgDataPoints(points: List<EcgDataPointEntity>)

    @Query("DELETE FROM measurements WHERE id = :id")
    suspend fun deleteMeasurementById(id: Long)
}
