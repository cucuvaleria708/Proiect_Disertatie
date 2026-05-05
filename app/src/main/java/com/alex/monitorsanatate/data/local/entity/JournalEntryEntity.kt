package com.alex.monitorsanatate.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "journal_entries",
    indices = [Index(value = ["userId"])]
)
data class JournalEntryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String,
    val measurementId: Long? = null,   // legătură opțională cu o măsurătoare
    val entryDate: Long = System.currentTimeMillis(),
    val title: String = "",
    val notes: String = "",
    val mood: Int = 3,                 // 1 (rău) – 5 (excelent)
    val tags: String = "",             // virgulă-separate: "sport,dimineață"
    val createdAt: Long = System.currentTimeMillis()
)
