package com.test.healthbox_app.data.network

import com.test.healthbox_app.data.model.response.BasicTestsResponse
import com.test.healthbox_app.data.model.response.ClinicLoginResponse
import com.test.healthbox_app.data.model.response.CreateBasicTestResponse
import com.test.healthbox_app.data.model.response.PatientLoginResponse
import com.test.healthbox_app.data.model.response.RefreshTokenResponse
import com.test.healthbox_app.data.model.response.VerifyTokenResponse
import com.test.healthbox_app.domain.model.request.BasicTestRequest
import com.test.healthbox_app.domain.model.request.ClinicLoginRequest
import com.test.healthbox_app.domain.model.request.PatientCreateRequest
import com.test.healthbox_app.domain.model.request.PatientLoginRequest
import com.test.healthbox_app.domain.model.request.PatientUpdateRequest
import com.test.healthbox_app.domain.model.request.RefreshTokenRequest
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {
    @POST("/api/v1/auth/clinic/login")
    suspend fun clinicLogin(
        @Body clinicLoginRequest: ClinicLoginRequest
    ): ClinicLoginResponse

    @POST("/api/v1/auth/patient/login")
    suspend fun patientLogin(
        @Body patientLoginRequest: PatientLoginRequest
    ): PatientLoginResponse

    @PUT("api/v1/patients/{patientId}")
    suspend fun patientUpdate(
        @Path("patientId") patientId: String,
        @Body request: PatientUpdateRequest
    ): PatientLoginResponse

    @POST("/api/v1/patients")
    suspend fun patientCreate(
        @Body createPatientRequest: PatientCreateRequest
    ): PatientLoginResponse

    @POST("api/v1/test/basic")
    suspend fun createBasicTest(
//        @Header("Authorization") token: String,
        @Body request: BasicTestRequest
    ): CreateBasicTestResponse

    @GET("api/v1/test/basic")
    suspend fun getBasicTests(
//        @Header("Authorization") token: String,
        @Query("patient_id") patientId: String
    ): BasicTestsResponse

    @POST("api/v1/auth/refresh")
    suspend fun refreshToken(
//        @Header("Authorization") token: String,
        @Body refreshTokenRequest: RefreshTokenRequest
    ): RefreshTokenResponse

    @GET("api/v1/auth/verify/{token}")
    suspend fun verifyToken(
//        @Header("Authorization") token: String,
        @Path("token") token: String
    ): VerifyTokenResponse
}