package com.test.healthbox_app.data.repository

import com.test.healthbox_app.data.local.dao.PatientDao
import com.test.healthbox_app.data.model.response.Patient
import com.test.healthbox_app.domain.repository.PatientDBRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class PatientDBRepositoryImpl @Inject constructor(private val patientDao: PatientDao) : PatientDBRepository {
    override suspend fun saveLoggedInPatient(patient: Patient) {
        TODO("Not yet implemented")
    }

    override fun observeCurrentPatient(): Flow<Patient?> {
        TODO("Not yet implemented")
    }

    override suspend fun getCurrentPatient(): Patient? {
        TODO("Not yet implemented")
    }

    override suspend fun clearCurrentPatient() {
        TODO("Not yet implemented")
    }
}