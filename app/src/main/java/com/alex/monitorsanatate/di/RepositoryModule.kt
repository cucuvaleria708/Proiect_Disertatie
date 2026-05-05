package com.alex.monitorsanatate.di

import com.alex.monitorsanatate.data.repository.ConnectionRepositoryImpl
import com.alex.monitorsanatate.data.repository.MeasurementRepositoryImpl
import com.alex.monitorsanatate.data.repository.SensorRepositoryImpl
import com.alex.monitorsanatate.domain.repository.ConnectionRepository
import com.alex.monitorsanatate.domain.repository.MeasurementRepository
import com.alex.monitorsanatate.domain.repository.SensorRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindMeasurementRepository(
        impl: MeasurementRepositoryImpl
    ): MeasurementRepository

    @Binds
    @Singleton
    abstract fun bindConnectionRepository(
        impl: ConnectionRepositoryImpl
    ): ConnectionRepository

    @Binds
    @Singleton
    abstract fun bindSensorRepository(
        impl: SensorRepositoryImpl
    ): SensorRepository
}
