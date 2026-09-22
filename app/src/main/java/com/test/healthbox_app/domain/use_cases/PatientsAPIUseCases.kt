package com.test.healthbox_app.domain.use_cases

import com.test.healthbox_app.domain.model.request.BasicTestRequest
import com.test.healthbox_app.domain.model.request.Hba1cTestRequest
import com.test.healthbox_app.domain.model.request.PatientCreateRequest
import com.test.healthbox_app.domain.model.request.PatientLoginRequest
import com.test.healthbox_app.domain.model.request.PatientUpdateRequest
import com.test.healthbox_app.domain.repository.PatientsAPIRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

class PatientsAPIUseCases @Inject constructor(
    private val patientsAPIRepository: PatientsAPIRepository
) {

    suspend fun patientLogin(patientLoginRequest: PatientLoginRequest) = patientsAPIRepository.patientLogin(
        patientLoginRequest = patientLoginRequest
    ).flowOn(Dispatchers.IO)

    suspend fun patientUpdate(patientId: String, patientUpdateRequest: PatientUpdateRequest) = patientsAPIRepository.patientUpdate(
        patientId = patientId, patientUpdateRequest = patientUpdateRequest
    ).flowOn(Dispatchers.IO)

    suspend fun patientCreate(patientCreateRequest: PatientCreateRequest) = patientsAPIRepository.patientCreate(
        patientCreateRequest = patientCreateRequest
    ).flowOn(Dispatchers.IO)

    suspend fun createBasicTest(basicTestRequest: BasicTestRequest, authToken: String) = patientsAPIRepository.createBasicTest(
        basicTestRequest = basicTestRequest,
        token = authToken,
    ).flowOn(Dispatchers.IO)

    suspend fun getBasicTest(patientId: String, authToken: String) = patientsAPIRepository.getBasicTests(
        patientId = patientId,
        token = authToken,
    ).flowOn(Dispatchers.IO)

    suspend fun createHba1cTest(hba1cTestRequest: Hba1cTestRequest, authToken: String) = patientsAPIRepository.createHba1cTest(
        hba1cTestRequest = hba1cTestRequest,
        token = authToken,
    ).flowOn(Dispatchers.IO)

    suspend fun getHba1cTest(patientId: String, authToken: String) = patientsAPIRepository.getHba1cTests(
        patientId = patientId,
        token = authToken,
    ).flowOn(Dispatchers.IO)

}