package com.example.walkhomesafe.services

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
import com.google.android.libraries.places.api.net.SearchNearbyRequest.RankPreference
import com.google.android.libraries.places.api.net.SearchNearbyRequest
import com.google.android.libraries.places.api.net.PlacesClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneOffset

class PlacesRepository(context: Context) {

    private val placesClient: PlacesClient

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
        placeTypes: List<PlaceType> = PlaceType.DEFAULT_FILTER_TYPES,
        includeClosed: Boolean = false
    ): Result<List<NearbyPlace>> = withContext<Result<List<NearbyPlace>>>(Dispatchers.IO) {
        try {
            val placeFields = listOf(
                Place.Field.ID,
                Place.Field.DISPLAY_NAME,
                Place.Field.LOCATION,
                Place.Field.PRIMARY_TYPE,
                Place.Field.TYPES,
                Place.Field.OPENING_HOURS,
                Place.Field.CURRENT_OPENING_HOURS,
                Place.Field.BUSINESS_STATUS,
                Place.Field.UTC_OFFSET,
                Place.Field.FORMATTED_ADDRESS
            )

            val bounds = CircularBounds.newInstance(currentLocation, radiusMeters.toDouble())

            val groupA = placeTypes.filter { it in PlaceType.FILTER_GROUP_A }
            val groupB = placeTypes.filter { it in PlaceType.FILTER_GROUP_B }

            fun buildRequest(apiTypes: List<String>) =
                SearchNearbyRequest.builder(bounds, placeFields)
                    .setIncludedTypes(apiTypes)
                    .setMaxResultCount(20)
                    .setRankPreference(RankPreference.DISTANCE)
                    .build()

            val allPlaces = mutableListOf<NearbyPlace>()

            coroutineScope {
                val deferredA = if (groupA.isNotEmpty()) {
                    async { placesClient.searchNearby(buildRequest(groupA.map { it.apiType })).await() }
                } else null

                val deferredB = if (groupB.isNotEmpty()) {
                    async { placesClient.searchNearby(buildRequest(groupB.map { it.apiType })).await() }
                } else null

                deferredA?.let {
                    try {
                        val responseA = it.await()
                        responseA.places.mapNotNullTo(allPlaces) { place ->
                            placeToNearbyPlace(place, groupA, includeClosed)
                        }
                    } catch (e: Exception) {
                        Log.e("PlacesRepository", "Group A search failed", e)
                    }
                }

                deferredB?.let {
                    try {
                        val responseB = it.await()
                        responseB.places.mapNotNullTo(allPlaces) { place ->
                            placeToNearbyPlace(place, groupB, includeClosed)
                        }
                    } catch (e: Exception) {
                        Log.e("PlacesRepository", "Group B search failed", e)
                    }
                }
            }

            val deduplicated = allPlaces.distinctBy { it.id }
            Result.success(deduplicated)

        } catch (e: ApiException) {
            Log.e("PlacesRepository", "Places API error: ${e.statusCode}", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e("PlacesRepository", "Error fetching nearby places", e)
            Result.failure(e)
        }
    }

    private fun placeToNearbyPlace(place: Place, filterTypes: List<PlaceType>, includeClosed: Boolean = false): NearbyPlace? {
        val location = place.location ?: return null
        val placeTypeList: List<String> = place.placeTypes ?: emptyList()

        val matchedApiType = placeTypeList.firstOrNull { apiType ->
            filterTypes.any { it.apiType == apiType }
        }

        val matchedType = matchedApiType?.let { apiType ->
            filterTypes.first { it.apiType == apiType }
        } ?: filterTypes.firstOrNull() ?: return null

        val isOpen = isPlaceCurrentlyOpen(place)

        if (!includeClosed && isOpen == false) {
            return null
        }

        val closingTime = if (isOpen == true) {
            getTodayClosingTime(place)
        } else {
            null
        }

        return NearbyPlace(
            id = place.id ?: "",
            name = place.displayName ?: "Unbekannt",
            latLng = location,
            placeType = matchedType,
            isOpenNow = isOpen,
            address = place.formattedAddress,
            closingTime = closingTime
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

    private fun formatLocalTime(hours: Int, minutes: Int): String {
        val hourStr = hours.toString().padStart(2, '0')
        val minuteStr = minutes.toString().padStart(2, '0')
        return "$hourStr:$minuteStr"
    }

    private fun getTodayClosingTime(place: Place): String? {
        val utcOffsetMinutes = place.utcOffsetMinutes

        val currentOpeningHours = place.currentOpeningHours
        if (currentOpeningHours != null) {
            val result = findActivePeriodClosingTime(currentOpeningHours, utcOffsetMinutes)
            if (result != null) return result
        }

        val regularOpeningHours = place.openingHours
        if (regularOpeningHours != null) {
            return findActivePeriodClosingTime(regularOpeningHours, utcOffsetMinutes)
        }

        return null
    }

    private fun findActivePeriodClosingTime(
        openingHours: OpeningHours,
        utcOffsetMinutes: Int?
    ): String? {
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
                val openDayName = open.day.name
                val openTimeMinutes = open.time.hours * 60 + open.time.minutes

                if (isTimeInPeriod(
                        currentDayName,
                        currentTimeMinutes,
                        openDayName,
                        openTimeMinutes,
                        openDayName,
                        24 * 60
                    )
                ) {
                    return "24/7"
                }
                continue
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
                val closeHour = period.close!!.time.hours
                val closeMin = period.close!!.time.minutes
                return formatLocalTime(closeHour, closeMin)
            }
        }

        return null
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
}
