package com.example.walkhomesafe.viewmodel

import android.app.Application
import android.location.Location
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

data class Suggestion(
    val placeId: String,
    val text: String
)

sealed interface MapUiState {
    data object Loading : MapUiState
    data class Location(val latLng: LatLng) : MapUiState
    data class Error(val message: String) : MapUiState
}

class MapViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(application)

    private val placesClient: PlacesClient = Places.createClient(application)

    private val defaultLocation = LatLng(52.5200, 13.4050)

    private val _uiState = MutableStateFlow<MapUiState>(MapUiState.Loading)
    private val _savedCameraPosition = MutableStateFlow(
        CameraPosition.fromLatLngZoom(defaultLocation, 12f)
    )
    private var hasFetchedOnce = false
    private val _autoFocusTrigger = MutableStateFlow(0L)
    private val _searchQuery = MutableStateFlow("")
    private val _suggestions = MutableStateFlow<List<Suggestion>>(emptyList())
    private val _selectedLocation = MutableStateFlow<LatLng?>(null)
    private val _selectedAddress = MutableStateFlow("")

    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()
    val savedCameraPosition: StateFlow<CameraPosition> = _savedCameraPosition.asStateFlow()
    var hasAnimated = false
    val autoFocusTrigger: StateFlow<Long> = _autoFocusTrigger.asStateFlow()
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    val suggestions: StateFlow<List<Suggestion>> = _suggestions.asStateFlow()
    val selectedLocation: StateFlow<LatLng?> = _selectedLocation.asStateFlow()
    val selectedAddress: StateFlow<String> = _selectedAddress.asStateFlow()

    fun fetchLocation() {
        if (hasFetchedOnce) return
        hasFetchedOnce = true
        viewModelScope.launch {
            _uiState.value = MapUiState.Loading
            if (!tryFetchCurrentLocation(true)) {
                hasFetchedOnce = false
                _uiState.value = MapUiState.Loading
            }
        }
    }

    fun updateSavedCameraPosition(cameraPosition: CameraPosition) {
        _savedCameraPosition.value = cameraPosition
    }

    fun requestLocationRefresh() {
        hasAnimated = false
        viewModelScope.launch {
            tryFetchCurrentLocation(false)
        }
    }

    fun searchLocation(query: String) {
        _searchQuery.value = query
        if (query.isBlank()) {
            _suggestions.value = emptyList()
            return
        }
        val request = FindAutocompletePredictionsRequest.builder()
            .setQuery(query)
            .setCountries(listOf("DE"))
            .build()
        placesClient.findAutocompletePredictions(request)
            .addOnSuccessListener { response ->
                _suggestions.value = response.autocompletePredictions.map {
                    Suggestion(it.placeId, it.getFullText(null).toString())
                }
            }
            .addOnFailureListener {
                _suggestions.value = emptyList()
            }
    }

    fun selectSuggestion(suggestion: Suggestion) {
        val fields = listOf(
            Place.Field.ID,
            Place.Field.DISPLAY_NAME,
            Place.Field.FORMATTED_ADDRESS,
            Place.Field.LOCATION
        )
        val request = FetchPlaceRequest.builder(suggestion.placeId, fields).build()
        placesClient.fetchPlace(request)
            .addOnSuccessListener { response ->
                val place = response.place
                place.location?.let {
                    selectLocation(
                        LatLng(it.latitude, it.longitude),
                        place.formattedAddress ?: place.displayName ?: suggestion.text
                    )
                }
            }
    }

    fun selectLocation(latLng: LatLng, address: String) {
        _selectedLocation.value = latLng
        _selectedAddress.value = address
        _searchQuery.value = address
        _suggestions.value = emptyList()
    }

    fun clearSelection() {
        _selectedLocation.value = null
        _selectedAddress.value = ""
        _searchQuery.value = ""
        _suggestions.value = emptyList()
    }

    private fun tryFetchCurrentLocation(fallbackToDefault: Boolean): Boolean {
        return try {
            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                CancellationTokenSource().token
            ).addOnSuccessListener { location: Location? ->
                handleLocationResult(location, fallbackToDefault = fallbackToDefault)
            }.addOnFailureListener {
                handleLocationResult(null, fallbackToDefault = fallbackToDefault)
            }
            true
        } catch (e: SecurityException) {
            if (fallbackToDefault) {
                updateUiWithLocation(defaultLocation)
            }
            false
        }
    }

    private fun handleLocationResult(location: Location?, fallbackToDefault: Boolean) {
        if (location != null) {
            updateUiWithLocation(LatLng(location.latitude, location.longitude))
            _autoFocusTrigger.value = System.nanoTime()
        } else {
            try {
                fusedLocationClient.lastLocation.addOnSuccessListener { lastLocation ->
                    if (lastLocation != null) {
                        updateUiWithLocation(LatLng(lastLocation.latitude, lastLocation.longitude))
                        _autoFocusTrigger.value = System.nanoTime()
                    } else if (fallbackToDefault) {
                        updateUiWithLocation(defaultLocation)
                        _autoFocusTrigger.value = System.nanoTime()
                    }
                }
            } catch (e: SecurityException) {
                if (fallbackToDefault) {
                    updateUiWithLocation(defaultLocation)
                    _autoFocusTrigger.value = System.nanoTime()
                }
            }
        }
    }

    private fun updateUiWithLocation(latLng: LatLng) {
        _savedCameraPosition.value = CameraPosition.fromLatLngZoom(latLng, 15f)
        _uiState.value = MapUiState.Location(latLng)
    }

    suspend fun requestLocationForSms(): LatLng? {
        return suspendCancellableCoroutine { cont ->
            try {
                fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    CancellationTokenSource().token
                ).addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        cont.resume(LatLng(location.latitude, location.longitude))
                    } else {
                        fetchLastForSms(cont)
                    }
                }.addOnFailureListener {
                    fetchLastForSms(cont)
                }
            } catch (e: SecurityException) {
                cont.resume(null)
            }
        }
    }

    private fun fetchLastForSms(cont: kotlinx.coroutines.CancellableContinuation<LatLng?>) {
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                cont.resume(if (location != null) LatLng(location.latitude, location.longitude) else null)
            }.addOnFailureListener {
                cont.resume(null)
            }
        } catch (e: SecurityException) {
            cont.resume(null)
        }
    }
}
