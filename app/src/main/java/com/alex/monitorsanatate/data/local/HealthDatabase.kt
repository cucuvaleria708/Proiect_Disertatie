package com.alex.monitorsanatate.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.alex.monitorsanatate.data.local.dao.JournalEntryDao
import com.alex.monitorsanatate.data.local.dao.MeasurementDao
import com.alex.monitorsanatate.data.local.dao.MedicalProfileDao
import com.alex.monitorsanatate.data.local.entity.EcgDataPointEntity
import com.alex.monitorsanatate.data.local.entity.JournalEntryEntity
import com.alex.monitorsanatate.data.local.entity.MeasurementEntity
import com.alex.monitorsanatate.data.local.entity.MedicalProfileEntity

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            "ALTER TABLE measurements ADD COLUMN measurementType TEXT NOT NULL DEFAULT 'PULS'"
        )
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE measurements ADD COLUMN aiResult TEXT")
        database.execSQL("ALTER TABLE measurements ADD COLUMN aiProbabilities TEXT")
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE measurements ADD COLUMN userId TEXT NOT NULL DEFAULT ''")
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // IMPORTANT: Nu folosi DEFAULT în coloane fără @ColumnInfo(defaultValue=...) în entitate.
        // Room 2.6+ validează strict valorile default și aruncă IllegalStateException la nepotrivire.
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `medical_profiles` (
                `userId`    TEXT    NOT NULL,
                `gender`    TEXT    NOT NULL,
                `age`       INTEGER NOT NULL,
                `weight`    REAL    NOT NULL,
                `heightCm`  REAL    NOT NULL,
                `bloodType` TEXT    NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                PRIMARY KEY(`userId`)
            )
            """.trimIndent()
        )
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `journal_entries` (
                `id`            INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `userId`        TEXT    NOT NULL,
                `measurementId` INTEGER,
                `entryDate`     INTEGER NOT NULL,
                `title`         TEXT    NOT NULL,
                `notes`         TEXT    NOT NULL,
                `mood`          INTEGER NOT NULL,
                `tags`          TEXT    NOT NULL,
                `createdAt`     INTEGER NOT NULL
            )
            """.trimIndent()
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_journal_entries_userId` ON `journal_entries`(`userId`)"
        )
    }
}

@Database(
    entities = [
        MeasurementEntity::class,
        EcgDataPointEntity::class,
        MedicalProfileEntity::class,
        JournalEntryEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class HealthDatabase : RoomDatabase() {
    abstract fun measurementDao(): MeasurementDao
    abstract fun medicalProfileDao(): MedicalProfileDao
    abstract fun journalEntryDao(): JournalEntryDao
}
