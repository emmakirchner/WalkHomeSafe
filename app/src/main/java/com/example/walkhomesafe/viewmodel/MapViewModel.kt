package com.example.walkhomesafe.viewmodel

import android.app.Application
import android.location.Location
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.walkhomesafe.services.PlacesRepository
import com.example.walkhomesafe.model.NearbyPlace
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
import kotlinx.coroutines.Job
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
    private val placesRepository: PlacesRepository = PlacesRepository(application)
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

    private val _showPublicLocations = MutableStateFlow(true)
    val showPublicLocations: StateFlow<Boolean> = _showPublicLocations.asStateFlow()

    private val _showClosedPlaces = MutableStateFlow(false)
    val showClosedPlaces: StateFlow<Boolean> = _showClosedPlaces.asStateFlow()

    private val _nearbyPlaces = MutableStateFlow<List<NearbyPlace>>(emptyList())
    val nearbyPlaces: StateFlow<List<NearbyPlace>> = _nearbyPlaces.asStateFlow()

    private val _isLoadingPlaces = MutableStateFlow(false)
    val isLoadingPlaces: StateFlow<Boolean> = _isLoadingPlaces.asStateFlow()

    private var currentLocation: LatLng? = null
    private var placesFetchJob: Job? = null

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

    fun togglePublicLocationsFilter() {
        val newValue = !_showPublicLocations.value
        _showPublicLocations.value = newValue

        if (newValue && _nearbyPlaces.value.isEmpty()) {
            currentLocation?.let { fetchNearbyPlaces(it) }
        }
    }

    fun toggleClosedPlacesFilter() {
        val newValue = !_showClosedPlaces.value
        _showClosedPlaces.value = newValue
        currentLocation?.let { fetchNearbyPlaces(it, includeClosed = newValue) }
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
            val latLng = LatLng(location.latitude, location.longitude)
            updateUiWithLocation(latLng)
            _autoFocusTrigger.value = System.nanoTime()
            onLocationUpdated(latLng)
        } else {
            try {
                fusedLocationClient.lastLocation.addOnSuccessListener { lastLocation ->
                    if (lastLocation != null) {
                        val latLng = LatLng(lastLocation.latitude, lastLocation.longitude)
                        updateUiWithLocation(latLng)
                        _autoFocusTrigger.value = System.nanoTime()
                        onLocationUpdated(latLng)
                    } else if (fallbackToDefault) {
                        updateUiWithLocation(defaultLocation)
                        _autoFocusTrigger.value = System.nanoTime()
                        onLocationUpdated(defaultLocation)
                    }
                }
            } catch (e: SecurityException) {
                if (fallbackToDefault) {
                    updateUiWithLocation(defaultLocation)
                    _autoFocusTrigger.value = System.nanoTime()
                    onLocationUpdated(defaultLocation)
                }
            }
        }
    }

    private fun onLocationUpdated(latLng: LatLng) {
        val shouldFetch = _showPublicLocations.value && hasLocationChangedSignificantly(currentLocation, latLng)
        currentLocation = latLng

        if (shouldFetch) {
            fetchNearbyPlaces(latLng)
        }
    }

    private fun hasLocationChangedSignificantly(oldLocation: LatLng?, newLocation: LatLng): Boolean {
        if (oldLocation == null) return true

        val results = FloatArray(1)
        android.location.Location.distanceBetween(
            oldLocation.latitude, oldLocation.longitude,
            newLocation.latitude, newLocation.longitude,
            results
        )
        return results[0] > 100.0f
    }

    private fun fetchNearbyPlaces(location: LatLng, includeClosed: Boolean = false) {
        placesFetchJob?.cancel()

        placesFetchJob = viewModelScope.launch {
            _isLoadingPlaces.value = true
            try {
                val result = placesRepository.searchNearbyPlaces(
                    currentLocation = location,
                    radiusMeters = 800,
                    includeClosed = includeClosed
                )
                _nearbyPlaces.value = result.getOrDefault(emptyList())
            } catch (e: Exception) {
                _nearbyPlaces.value = emptyList()
            } finally {
                _isLoadingPlaces.value = false
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
