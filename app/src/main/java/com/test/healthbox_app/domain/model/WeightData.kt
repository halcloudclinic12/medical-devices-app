package com.test.healthbox_app.domain.model

data class WeightData(
    val weight: String,
    val unit: String,
    val impedance: Int,
//    val status: String?,
    val algorithmId: Int,
//    val result: String = getResult(weight)
)


/*
fun getResult(weight: FloatTweenSpec): String {
    return when (weight) {
        in 0..10 -> "저체중"
        in 11..20 -> "정상"
        in 21..30 -> "과체중"
//        else -> "
    }*/
