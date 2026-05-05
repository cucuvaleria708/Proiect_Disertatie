package com.alex.monitorsanatate.data.local.dao

import androidx.room.*
import com.alex.monitorsanatate.data.local.entity.MedicalProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicalProfileDao {

    @Query("SELECT * FROM medical_profiles WHERE userId = :userId")
    fun getProfile(userId: String): Flow<MedicalProfileEntity?>

    @Query("SELECT * FROM medical_profiles WHERE userId = :userId")
    suspend fun getProfileOnce(userId: String): MedicalProfileEntity?

    @Upsert
    suspend fun upsertProfile(profile: MedicalProfileEntity)

    @Query("DELETE FROM medical_profiles WHERE userId = :userId")
    suspend fun deleteProfile(userId: String)
}
