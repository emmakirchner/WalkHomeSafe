package com.example.walkhomesafe.model

import com.google.android.gms.maps.model.LatLng

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
        val FILTER_GROUP_A = listOf(
            SUPERMARKET,
            SHOP,
            GAS_STATION,
            CAFE
        )

        val FILTER_GROUP_B = listOf(
            RESTAURANT,
            LIBRARY,
            PHARMACY,
            POLICE_STATION,
            HOSPITAL
        )

        val DEFAULT_FILTER_TYPES = FILTER_GROUP_A + FILTER_GROUP_B
    }
}

data class NearbyPlace(
    val id: String,
    val name: String,
    val latLng: LatLng,
    val placeType: PlaceType,
    val isOpenNow: Boolean? = null,
    val address: String? = null,
    val closingTime: String? = null
)
