package com.test.healthbox_app.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.test.healthbox_app.data.local.dao.PatientDao
import com.test.healthbox_app.data.local.dao.ParameterDao
import com.test.healthbox_app.data.local.entity.PatientEntity
import com.test.healthbox_app.data.local.entity.ParametersEntity

@Database(
    entities = [PatientEntity::class, ParametersEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun patientDao(): PatientDao
    abstract fun parameterDao(): ParameterDao
}