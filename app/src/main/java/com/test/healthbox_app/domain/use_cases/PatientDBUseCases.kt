package com.test.healthbox_app.domain.use_cases

import com.test.healthbox_app.data.model.response.Patient
import com.test.healthbox_app.domain.repository.PatientDBRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

class PatientDBUseCases @Inject constructor(private val patientDBRepository: PatientDBRepository) {

    suspend fun saveLoggedInPatient(patient: Patient) = patientDBRepository.saveLoggedInPatient(patient)

    suspend fun observeCurrentPatient() = patientDBRepository.observeCurrentPatient().flowOn(Dispatchers.IO)

}