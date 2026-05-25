package com.example.walkhomesafe.viewmodel

import android.app.Application
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.walkhomesafe.data.places.PlacesRepository
import com.example.walkhomesafe.model.NearbyPlace
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

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
    private val placesRepository: PlacesRepository = PlacesRepository(application)

    private val _uiState = MutableStateFlow<MapUiState>(MapUiState.Loading)
    private val defaultLocation = LatLng(52.5200, 13.4050)
    private val _savedCameraPosition = MutableStateFlow(
        CameraPosition.fromLatLngZoom(defaultLocation, 12f)
    )
    private var hasFetchedOnce = false
    private val _autoFocusTrigger = MutableStateFlow(0L)

    private val _showPublicLocations = MutableStateFlow(true)
    val showPublicLocations: StateFlow<Boolean> = _showPublicLocations.asStateFlow()

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

    fun togglePublicLocationsFilter() {
        val newValue = !_showPublicLocations.value
        _showPublicLocations.value = newValue

        if (newValue && _nearbyPlaces.value.isEmpty()) {
            currentLocation?.let { fetchNearbyPlaces(it) }
        }
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

    private fun fetchNearbyPlaces(location: LatLng) {
        placesFetchJob?.cancel()

        placesFetchJob = viewModelScope.launch {
            _isLoadingPlaces.value = true
            try {
                val result = placesRepository.searchNearbyPlaces(
                    currentLocation = location,
                    radiusMeters = 800
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
