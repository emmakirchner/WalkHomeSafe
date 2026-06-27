package com.example.walkhomesafe.model

import com.google.android.gms.maps.model.LatLng

/**
 * Enum of place types supported for public location markers on the map.
 *
 * Each type has a corresponding Google Places API type string and a German display name.
 *
 * @property apiType Google Places API type string
 * @property displayName German display name shown in the UI
 */
enum class PlaceType(val apiType: String, val displayName: String) {
    RESTAURANT("restaurant", "Restaurant"),
    CAFE("cafe", "Cafe"),
    SHOP("store", "Shop"),
    LIBRARY("library", "Bibliothek"),
    SUPERMARKET("supermarket", "Supermarkt"),
    PHARMACY("pharmacy", "Apotheke"),
    POLICE_STATION("police", "Polizeistation"),
    HOSPITAL("hospital", "Krankenhaus"),
    GAS_STATION("gas_station", "Tankstelle");

    companion object {
        /**
         * Group A filter types used in the first parallel Places API request.
         */
        val FILTER_GROUP_A = listOf(
            SUPERMARKET,
            SHOP,
            GAS_STATION,
            CAFE
        )

        /**
         * Group B filter types used in the second parallel Places API request.
         */
        val FILTER_GROUP_B = listOf(
            RESTAURANT,
            LIBRARY,
            PHARMACY,
            POLICE_STATION,
            HOSPITAL
        )

        /**
         * Combined default filter types including all known place types.
         */
        val DEFAULT_FILTER_TYPES = FILTER_GROUP_A + FILTER_GROUP_B
    }
}

/**
 * Data class representing a nearby public place returned by the Google Places API.
 *
 * @property id Unique place ID from Google Places
 * @property name Display name of the place
 * @property latLng Geographic coordinates
 * @property placeType Type classification of the place
 * @property isOpenNow Whether the place is currently open, null if unknown
 * @property address Optional formatted address
 * @property closingTime Optional closing time as a formatted string (e.g., "22:00")
 */
data class NearbyPlace(
    val id: String,
    val name: String,
    val latLng: LatLng,
    val placeType: PlaceType,
    val isOpenNow: Boolean? = null,
    val address: String? = null,
    val closingTime: String? = null
)
