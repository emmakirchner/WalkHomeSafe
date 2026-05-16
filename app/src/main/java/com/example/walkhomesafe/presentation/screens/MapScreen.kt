package com.example.walkhomesafe.presentation.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.walkhomesafe.viewmodel.MapUiState
import com.example.walkhomesafe.viewmodel.MapViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.delay

@Composable
fun MapScreen(
    mapViewModel: MapViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by mapViewModel.uiState.collectAsState()
    val savedCameraPosition by mapViewModel.savedCameraPosition.collectAsState()
    var isLocationGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        isLocationGranted = granted
        if (granted) {
            mapViewModel.fetchLocation()
        }
    }

    LaunchedEffect(Unit) {
        if (isLocationGranted) {
            mapViewModel.fetchLocation()
        } else {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    val autoFocusTrigger by mapViewModel.autoFocusTrigger.collectAsState()

    val cameraPositionState = rememberCameraPositionState {
        position = savedCameraPosition
    }

    LaunchedEffect(cameraPositionState.position) {
        mapViewModel.updateSavedCameraPosition(cameraPositionState.position)
    }

    val locationManager = remember { context.getSystemService(Context.LOCATION_SERVICE) as LocationManager }
    var isGpsEnabled by remember { mutableStateOf(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) }

    var wasGpsOff by remember { mutableStateOf(!isGpsEnabled) }

    LaunchedEffect(Unit) {
        while (true) {
            val current = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            isGpsEnabled = current
            if (current && wasGpsOff) {
                mapViewModel.requestLocationRefresh()
            }
            wasGpsOff = !current
            delay(3000)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            uiSettings = MapUiSettings(myLocationButtonEnabled = isLocationGranted),
            properties = MapProperties(
                isMyLocationEnabled = isLocationGranted
            ),
        )

        when (val state = uiState) {
            is MapUiState.Loading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            is MapUiState.Location -> {
                LaunchedEffect(state.latLng, autoFocusTrigger) {
                    if (!mapViewModel.hasAnimated) {
                        cameraPositionState.animate(
                            CameraUpdateFactory.newLatLngZoom(state.latLng, 15f)
                        )
                        mapViewModel.hasAnimated = true
                    }
                }
            }
            is MapUiState.Error -> {
                Text(
                    text = state.message,
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        if (!isGpsEnabled) {
            Card(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Text(
                        text = "GPS ist deaktiviert. Aktiviere GPS für eine genaue Standortbestimmung.",
                        modifier = Modifier.padding(start = 8.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}