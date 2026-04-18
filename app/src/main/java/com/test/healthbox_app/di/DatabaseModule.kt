package com.test.healthbox_app.di

import android.content.Context
import androidx.room.Room
import com.test.healthbox_app.data.local.dao.PatientDao
import com.test.healthbox_app.data.local.db.AppDatabase
import com.test.healthbox_app.data.repository.PatientDBRepositoryImpl
import com.test.healthbox_app.domain.repository.PatientDBRepository
import com.test.healthbox_app.domain.use_cases.PatientDBUseCases
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    fun provideDatabase(@ApplicationContext context: Context) =
        Room.databaseBuilder(context, AppDatabase::class.java, "app_db").build()

    @Provides
    fun providePatientDao(db: AppDatabase) = db.patientDao()

    @Provides
    fun providePatientRepository(dao: PatientDao): PatientDBRepository = PatientDBRepositoryImpl(dao)

    @Provides
    fun provideGetPatientsUseCase(repo: PatientDBRepository) = PatientDBUseCases(repo)
}