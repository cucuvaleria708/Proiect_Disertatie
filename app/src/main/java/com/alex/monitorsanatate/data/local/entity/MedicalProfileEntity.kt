package com.alex.monitorsanatate.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "medical_profiles")
data class MedicalProfileEntity(
    @PrimaryKey
    val userId: String,
    val gender: String = "M",
    val age: Int = 0,
    val weight: Float = 0f,
    val heightCm: Float = 0f,
    val bloodType: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
