package com.test.healthbox_app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Single-row table to remember who is the current logged-in patient */
@Entity(tableName = "session")
data class ParametersEntity(
    @PrimaryKey val id: Int = 0,
    val currentPatientId: String? = null,
)