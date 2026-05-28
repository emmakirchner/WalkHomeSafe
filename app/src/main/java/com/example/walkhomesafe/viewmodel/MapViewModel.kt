package com.example.walkhomesafe.viewmodel

import android.app.Application
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
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
    private val _uiState = MutableStateFlow<MapUiState>(MapUiState.Loading)
    private val defaultLocation = LatLng(52.5200, 13.4050)
    private val _savedCameraPosition = MutableStateFlow(
        CameraPosition.fromLatLngZoom(defaultLocation, 12f)
    )
    private var hasFetchedOnce = false
    private val _autoFocusTrigger = MutableStateFlow(0L)

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
