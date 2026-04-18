package com.test.healthbox_app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.test.healthbox_app.data.local.entity.PatientEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PatientDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(patient: PatientEntity)

    @Query("SELECT * FROM patients WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): PatientEntity?

    @Query("DELETE FROM patients")
    suspend fun clearAll()

    @Query(
        "SELECT * FROM patients WHERE id = " +
                "(SELECT currentPatientId FROM session WHERE id = 0) LIMIT 1"
    )

    fun observeCurrent(): Flow<PatientEntity?>
}