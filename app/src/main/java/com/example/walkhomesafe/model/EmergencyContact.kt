package com.example.walkhomesafe.model

import kotlinx.serialization.Serializable

@Serializable
data class EmergencyContact(
    val id: Long,
    val name: String,
    val phone: String
)
