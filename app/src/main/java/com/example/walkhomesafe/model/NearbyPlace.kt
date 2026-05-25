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
        val DEFAULT_FILTER_TYPES = listOf(
            RESTAURANT,
            CAFE,
            SHOP,
            LIBRARY,
            SUPERMARKET,
            PHARMACY,
            POLICE_STATION,
            HOSPITAL,
            GAS_STATION
        )
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
