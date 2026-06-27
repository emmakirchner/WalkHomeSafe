package com.example.walkhomesafe.model

import kotlinx.serialization.Serializable

/**
 * Data class representing an emergency contact person.
 *
 * @property id Unique identifier (typically generated via System.currentTimeMillis())
 * @property name Display name of the contact
 * @property phone Phone number of the contact
 */
@Serializable
data class EmergencyContact(
    val id: Long,
    val name: String,
    val phone: String
)
