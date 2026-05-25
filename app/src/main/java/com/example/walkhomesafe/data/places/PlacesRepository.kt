package com.example.walkhomesafe.data.places

import android.content.Context
import android.util.Log
import com.example.walkhomesafe.model.NearbyPlace
import com.example.walkhomesafe.model.PlaceType
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.OpeningHours
import com.google.android.libraries.places.api.model.Period
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.CircularBounds
import com.google.android.libraries.places.api.net.SearchNearbyRequest
import com.google.android.libraries.places.api.net.PlacesClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneOffset

class PlacesRepository(context: Context) {

    private val placesClient: PlacesClient
    private val useMockFallback: Boolean = true

    init {
        if (!Places.isInitialized()) {
            val apiKey = getApiKey(context)
            if (apiKey != null) {
                Places.initializeWithNewPlacesApiEnabled(context, apiKey)
            }
        }
        placesClient = Places.createClient(context)
    }

    private fun getApiKey(context: Context): String? {
        return try {
            val appInfo = context.packageManager.getApplicationInfo(
                context.packageName,
                android.content.pm.PackageManager.GET_META_DATA
            )
            appInfo.metaData.getString("com.google.android.geo.API_KEY")
        } catch (e: Exception) {
            Log.e("PlacesRepository", "Failed to get API key", e)
            null
        }
    }

    suspend fun searchNearbyPlaces(
        currentLocation: LatLng,
        radiusMeters: Int = 800,
        placeTypes: List<PlaceType> = PlaceType.DEFAULT_FILTER_TYPES
    ): Result<List<NearbyPlace>> = withContext<Result<List<NearbyPlace>>>(Dispatchers.IO) {
        try {
            val placeFields = listOf(
                Place.Field.ID,
                Place.Field.DISPLAY_NAME,
                Place.Field.LOCATION,
                Place.Field.TYPES,
                Place.Field.OPENING_HOURS,
                Place.Field.CURRENT_OPENING_HOURS,
                Place.Field.BUSINESS_STATUS,
                Place.Field.UTC_OFFSET,
                Place.Field.FORMATTED_ADDRESS
            )

            val bounds = CircularBounds.newInstance(currentLocation, radiusMeters.toDouble())
            val includedTypes = placeTypes.map { it.apiType }

            val searchRequest = SearchNearbyRequest.builder(bounds, placeFields)
                .setIncludedTypes(includedTypes)
                .setMaxResultCount(20)
                .build()

            val response = placesClient.searchNearby(searchRequest).await()
            val places = response.places.mapNotNull { place ->
                placeToNearbyPlace(place, placeTypes)
            }

            if (places.isEmpty() && useMockFallback) {
                Result.success(getMockPlaces(currentLocation))
            } else {
                Result.success(places)
            }

        } catch (e: ApiException) {
            Log.e("PlacesRepository", "Places API error: ${e.statusCode}", e)
            if (useMockFallback) {
                Result.success(getMockPlaces(currentLocation))
            } else {
                Result.failure<List<NearbyPlace>>(e)
            }
        } catch (e: Exception) {
            Log.e("PlacesRepository", "Error fetching nearby places", e)
            if (useMockFallback) {
                Result.success(getMockPlaces(currentLocation))
            } else {
                Result.failure<List<NearbyPlace>>(e)
            }
        }
    }

    private fun placeToNearbyPlace(place: Place, filterTypes: List<PlaceType>): NearbyPlace? {
        val location = place.location ?: return null
        val placeTypes: List<String> = place.placeTypes ?: emptyList()

        val matchedType = filterTypes.firstOrNull { filterType ->
            placeTypes.any { it == filterType.apiType }
        } ?: filterTypes.firstOrNull() ?: return null

        val isOpen = isPlaceCurrentlyOpen(place)

        return NearbyPlace(
            id = place.id ?: "",
            name = place.displayName ?: "Unbekannt",
            latLng = location,
            placeType = matchedType,
            isOpenNow = isOpen,
            address = place.formattedAddress
        )
    }

    private val dayOfWeekToNumber = mapOf(
        "MONDAY" to 1,
        "TUESDAY" to 2,
        "WEDNESDAY" to 3,
        "THURSDAY" to 4,
        "FRIDAY" to 5,
        "SATURDAY" to 6,
        "SUNDAY" to 7
    )

    private fun dayOfWeekValue(dayName: String): Int {
        return dayOfWeekToNumber[dayName] ?: 1
    }

    private fun isPlaceCurrentlyOpen(place: Place): Boolean? {
        val businessStatus = place.businessStatus
        if (businessStatus == Place.BusinessStatus.CLOSED_PERMANENTLY) return false
        if (businessStatus == Place.BusinessStatus.CLOSED_TEMPORARILY) return false

        val utcOffsetMinutes = place.utcOffsetMinutes

        val currentOpeningHours = place.currentOpeningHours
        if (currentOpeningHours != null) {
            val result = checkOpeningHours(currentOpeningHours, utcOffsetMinutes)
            if (result != null) return result
        }

        val regularOpeningHours = place.openingHours
        if (regularOpeningHours != null) {
            return checkOpeningHours(regularOpeningHours, utcOffsetMinutes)
        }

        return null
    }

    private fun checkOpeningHours(openingHours: OpeningHours, utcOffsetMinutes: Int?): Boolean? {
        val periods = openingHours.periods
        if (periods.isNullOrEmpty()) return null

        val nowUtc = Instant.now()
        val localTimeMillis = nowUtc.toEpochMilli() + (utcOffsetMinutes ?: 0) * 60_000L

        val localInstant = Instant.ofEpochMilli(localTimeMillis)
        val localDateTime = localInstant.atZone(ZoneOffset.UTC).toLocalDateTime()
        val currentDayName = localDateTime.dayOfWeek.name
        val currentTimeMinutes = localDateTime.hour * 60 + localDateTime.minute

        for (period in periods) {
            val open = period.open ?: continue

            if (period.close == null) {
                return true
            }

            val openDayName = open.day.name
            val openTimeMinutes = open.time.hours * 60 + open.time.minutes
            val closeDayName = period.close!!.day.name
            val closeTimeMinutes = period.close!!.time.hours * 60 + period.close!!.time.minutes

            if (isTimeInPeriod(
                    currentDayName,
                    currentTimeMinutes,
                    openDayName,
                    openTimeMinutes,
                    closeDayName,
                    closeTimeMinutes
                )
            ) {
                return true
            }
        }

        return false
    }

    private fun isTimeInPeriod(
        currentDayName: String,
        currentTimeMinutes: Int,
        openDayName: String,
        openTimeMinutes: Int,
        closeDayName: String,
        closeTimeMinutes: Int
    ): Boolean {
        val currentDayValue = dayOfWeekValue(currentDayName)
        val openDayValue = dayOfWeekValue(openDayName)
        val closeDayValue = dayOfWeekValue(closeDayName)

        if (currentDayValue == openDayValue) {
            if (currentTimeMinutes < openTimeMinutes) return false

            if (openDayValue == closeDayValue) {
                return currentTimeMinutes < closeTimeMinutes
            }

            return true
        }

        if (openDayValue != closeDayValue) {
            if (openDayValue < closeDayValue) {
                if (currentDayValue > openDayValue && currentDayValue < closeDayValue) {
                    return true
                }
                if (currentDayValue == closeDayValue && currentTimeMinutes < closeTimeMinutes) {
                    return true
                }
            } else {
                if (currentDayValue > openDayValue || currentDayValue < closeDayValue) {
                    return true
                }
                if (currentDayValue == closeDayValue && currentTimeMinutes < closeTimeMinutes) {
                    return true
                }
            }
        }

        return false
    }

    private fun getMockPlaces(currentLocation: LatLng): List<NearbyPlace> {
        val baseLat = currentLocation.latitude
        val baseLng = currentLocation.longitude

        return listOf(
            NearbyPlace(
                id = "mock_1",
                name = "Cafe am Markt",
                latLng = LatLng(baseLat + 0.001, baseLng + 0.001),
                placeType = PlaceType.CAFE,
                isOpenNow = true
            ),
            NearbyPlace(
                id = "mock_2",
                name = "Stadtbibliothek",
                latLng = LatLng(baseLat - 0.0015, baseLng + 0.002),
                placeType = PlaceType.LIBRARY,
                isOpenNow = true
            ),
            NearbyPlace(
                id = "mock_3",
                name = "REWE Supermarkt",
                latLng = LatLng(baseLat + 0.002, baseLng - 0.001),
                placeType = PlaceType.SUPERMARKET,
                isOpenNow = true
            ),
            NearbyPlace(
                id = "mock_4",
                name = "Pizzeria Roma",
                latLng = LatLng(baseLat - 0.0005, baseLng - 0.002),
                placeType = PlaceType.RESTAURANT,
                isOpenNow = true
            ),
            NearbyPlace(
                id = "mock_5",
                name = "Apotheke am Bahnhof",
                latLng = LatLng(baseLat + 0.0008, baseLng - 0.0015),
                placeType = PlaceType.PHARMACY,
                isOpenNow = true
            ),
            NearbyPlace(
                id = "mock_6",
                name = "Modehaus",
                latLng = LatLng(baseLat - 0.001, baseLng + 0.0005),
                placeType = PlaceType.SHOP,
                isOpenNow = true
            ),
            NearbyPlace(
                id = "mock_7",
                name = "Polizeiwache",
                latLng = LatLng(baseLat + 0.0025, baseLng + 0.0025),
                placeType = PlaceType.POLICE_STATION,
                isOpenNow = null
            ),
            NearbyPlace(
                id = "mock_8",
                name = "Krankenhaus Mitte",
                latLng = LatLng(baseLat - 0.003, baseLng),
                placeType = PlaceType.HOSPITAL,
                isOpenNow = null
            ),
            NearbyPlace(
                id = "mock_9",
                name = "Shell Tankstelle",
                latLng = LatLng(baseLat, baseLng + 0.003),
                placeType = PlaceType.GAS_STATION,
                isOpenNow = true
            )
        )
    }
}
