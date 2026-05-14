package com.example.walkhomesafe.viewmodel

import android.app.Application
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
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

    fun fetchLocation() {
        viewModelScope.launch {
            _uiState.value = MapUiState.Loading
            try {
                fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        _uiState.value = MapUiState.Location(
                            LatLng(location.latitude, location.longitude)
                        )
                    } else {
                        _uiState.value = MapUiState.Location(defaultLocation)
                    }
                }.addOnFailureListener {
                    _uiState.value = MapUiState.Location(defaultLocation)
                }
            } catch (e: SecurityException) {
                _uiState.value = MapUiState.Location(defaultLocation)
            }
        }
    }
}
