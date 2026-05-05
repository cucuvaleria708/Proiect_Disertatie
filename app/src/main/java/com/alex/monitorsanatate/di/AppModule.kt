package com.alex.monitorsanatate.di

import android.content.Context
import androidx.room.Room
import com.alex.monitorsanatate.data.local.HealthDatabase
import com.alex.monitorsanatate.data.local.MIGRATION_1_2
import com.alex.monitorsanatate.data.local.MIGRATION_2_3
import com.alex.monitorsanatate.data.local.MIGRATION_3_4
import com.alex.monitorsanatate.data.local.MIGRATION_4_5
import com.alex.monitorsanatate.data.local.dao.JournalEntryDao
import com.alex.monitorsanatate.data.local.dao.MeasurementDao
import com.alex.monitorsanatate.data.local.dao.MedicalProfileDao
import com.alex.monitorsanatate.data.local.datastore.SettingsDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideHealthDatabase(@ApplicationContext context: Context): HealthDatabase {
        return Room.databaseBuilder(
            context,
            HealthDatabase::class.java,
            "health_monitor_db"
        )
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
            .build()
    }

    @Provides
    fun provideMeasurementDao(database: HealthDatabase): MeasurementDao =
        database.measurementDao()

    @Provides
    fun provideMedicalProfileDao(database: HealthDatabase): MedicalProfileDao =
        database.medicalProfileDao()

    @Provides
    fun provideJournalEntryDao(database: HealthDatabase): JournalEntryDao =
        database.journalEntryDao()

    @Provides
    @Singleton
    fun provideSettingsDataStore(@ApplicationContext context: Context): SettingsDataStore =
        SettingsDataStore(context)
}
