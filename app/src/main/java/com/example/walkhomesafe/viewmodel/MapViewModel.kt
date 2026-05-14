package com.example.walkhomesafe.viewmodel

import android.app.Application
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    private val defaultLocation = LatLng(52.5200, 13.4050)

    private val _savedCameraPosition = MutableStateFlow(
        CameraPosition.fromLatLngZoom(defaultLocation, 12f)
    )
    val savedCameraPosition: StateFlow<CameraPosition> = _savedCameraPosition.asStateFlow()

    private var hasFetchedOnce = false
    var hasAnimated = false

    fun fetchLocation() {
        if (hasFetchedOnce) return
        hasFetchedOnce = true
        viewModelScope.launch {
            _uiState.value = MapUiState.Loading
            try {
                fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                    val latLng = if (location != null) {
                        LatLng(location.latitude, location.longitude)
                    } else {
                        defaultLocation
                    }
                    _savedCameraPosition.value = CameraPosition.fromLatLngZoom(latLng, 15f)
                    _uiState.value = MapUiState.Location(latLng)
                }.addOnFailureListener {
                    _uiState.value = MapUiState.Location(defaultLocation)
                }
            } catch (e: SecurityException) {
                _uiState.value = MapUiState.Location(defaultLocation)
            }
        }
    }

    fun updateSavedCameraPosition(cameraPosition: CameraPosition) {
        _savedCameraPosition.value = cameraPosition
    }
}
