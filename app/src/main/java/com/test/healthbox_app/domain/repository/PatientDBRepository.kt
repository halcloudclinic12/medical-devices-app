package com.test.healthbox_app.domain.repository

import com.test.healthbox_app.data.model.response.Patient
import kotlinx.coroutines.flow.Flow

interface PatientDBRepository {
    suspend fun saveLoggedInPatient(patient: Patient)
    fun observeCurrentPatient(): Flow<Patient?>
    suspend fun getCurrentPatient(): Patient?
    suspend fun clearCurrentPatient()
}