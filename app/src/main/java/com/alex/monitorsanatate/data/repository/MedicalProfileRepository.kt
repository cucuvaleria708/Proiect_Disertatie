package com.alex.monitorsanatate.data.repository

import com.alex.monitorsanatate.data.local.dao.MedicalProfileDao
import com.alex.monitorsanatate.data.local.entity.MedicalProfileEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MedicalProfileRepository @Inject constructor(
    private val medicalProfileDao: MedicalProfileDao
) {

    fun getProfile(userId: String): Flow<MedicalProfileEntity?> =
        medicalProfileDao.getProfile(userId)

    suspend fun getProfileOnce(userId: String): MedicalProfileEntity? =
        medicalProfileDao.getProfileOnce(userId)

    suspend fun saveProfile(
        userId: String,
        gender: String,
        age: Int,
        weight: Float,
        heightCm: Float = 0f,
        bloodType: String = ""
    ) {
        val existing = medicalProfileDao.getProfileOnce(userId)
        medicalProfileDao.upsertProfile(
            MedicalProfileEntity(
                userId    = userId,
                gender    = gender,
                age       = age,
                weight    = weight,
                heightCm  = heightCm,
                bloodType = bloodType,
                createdAt = existing?.createdAt ?: System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteProfile(userId: String) =
        medicalProfileDao.deleteProfile(userId)
}
