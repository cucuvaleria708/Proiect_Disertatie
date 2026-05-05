package com.alex.monitorsanatate.data.repository

import com.alex.monitorsanatate.data.local.dao.MeasurementDao
import com.alex.monitorsanatate.data.local.datastore.SettingsDataStore
import com.alex.monitorsanatate.data.local.entity.EcgDataPointEntity
import com.alex.monitorsanatate.data.local.entity.MeasurementEntity
import com.alex.monitorsanatate.domain.model.ConnectionMethod
import com.alex.monitorsanatate.domain.model.Measurement
import com.alex.monitorsanatate.domain.repository.MeasurementRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MeasurementRepositoryImpl @Inject constructor(
    private val measurementDao: MeasurementDao,
    private val settingsDataStore: SettingsDataStore
) : MeasurementRepository {

    override fun getAllMeasurements(): Flow<List<Measurement>> {
        return settingsDataStore.userId.flatMapLatest { userId ->
            if (userId.isNullOrEmpty()) {
                flowOf(emptyList())
            } else {
                measurementDao.getMeasurementsByUserId(userId).map { entities ->
                    entities.map { it.toDomain() }
                }
            }
        }
    }

    override suspend fun getMeasurementById(id: Long): Measurement? {
        return measurementDao.getMeasurementById(id)?.toDomain()
    }

    override suspend fun getEcgDataForMeasurement(id: Long): List<Float> {
        return measurementDao.getEcgDataForMeasurement(id).map { it.value }
    }

    override suspend fun saveMeasurement(measurement: Measurement) {
        val currentUserId = settingsDataStore.userId.first() ?: return
        val entity = MeasurementEntity(
            userId = currentUserId,
            startTime = measurement.startTime,
            endTime = measurement.endTime,
            averageBpm = measurement.averageBpm,
            minBpm = measurement.minBpm,
            maxBpm = measurement.maxBpm,
            connectionMethod = measurement.connectionMethod.name,
            measurementType = measurement.measurementType,
            notes = measurement.notes,
            aiResult = measurement.aiResult,
            aiProbabilities = measurement.aiProbabilities
        )
        val measurementId = measurementDao.insertMeasurement(entity)

        if (measurement.ecgData.isNotEmpty()) {
            val ecgEntities = measurement.ecgData.mapIndexed { index, value ->
                EcgDataPointEntity(
                    measurementId = measurementId,
                    timestampOffset = index.toLong() * 4L,
                    value = value
                )
            }
            ecgEntities.chunked(1000).forEach { batch ->
                measurementDao.insertEcgDataPoints(batch)
            }
        }
    }

    override suspend fun deleteMeasurement(id: Long) {
        measurementDao.deleteMeasurementById(id)
    }

    private fun MeasurementEntity.toDomain() = Measurement(
        id = id,
        startTime = startTime,
        endTime = endTime,
        averageBpm = averageBpm,
        minBpm = minBpm,
        maxBpm = maxBpm,
        measurementType = measurementType,
        connectionMethod = try {
            ConnectionMethod.valueOf(connectionMethod)
        } catch (e: IllegalArgumentException) {
            ConnectionMethod.WIFI
        },
        notes = notes,
        aiResult = aiResult,
        aiProbabilities = aiProbabilities
    )
}
