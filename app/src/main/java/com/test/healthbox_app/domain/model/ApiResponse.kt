package com.test.healthbox_app.domain.model

sealed class ApiResponse<out T> {

    data class ApiSuccess<T>(val data: T) : ApiResponse<T>()

    data class ApiError<T>(
        val message: String,
        val code: Int? = null
    ) : ApiResponse<T>()

    class ApiLoading<T> : ApiResponse<T>()
}
/*
sealed class ApiResponse<T>(val apiData: T? = null, val message: String? = null) {
    class ApiSuccess<T>(data: T?) : ApiResponse<T>(apiData = data)

    class ApiError<T>(message: String?) : ApiResponse<T>(message = message)

    class ApiLoading<T>() : ApiResponse<T>()
}*/
