package com.test.healthbox_app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "patients")
data class PatientEntity(
    @PrimaryKey val id: String,
    val name: String?,
    val age: Int?,
    val gender: String?,
    val phone: String?,
    val email: String?,
    val clinicId: String?,
)