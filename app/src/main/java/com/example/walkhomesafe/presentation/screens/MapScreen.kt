package com.example.walkhomesafe.presentation.screens

import android.content.Context
import android.location.LocationManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.walkhomesafe.presentation.screens.report.CreateScreen
import com.example.walkhomesafe.viewmodel.MapUiState
import com.example.walkhomesafe.viewmodel.MapViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import com.example.walkhomesafe.viewmodel.PermissionsViewModel
import kotlinx.coroutines.delay

@Composable
fun MapScreen(
    mapViewModel: MapViewModel = viewModel(),
    permissionsViewModel: PermissionsViewModel = viewModel()
) {
    var showCreateReport by remember { mutableStateOf(false) }

    if (showCreateReport) {
        val loc = mapViewModel.selectedLocation.collectAsState().value
        val addr = mapViewModel.selectedAddress.collectAsState().value
        if (loc != null) {
            CreateScreen(
                latitude = loc.latitude,
                longitude = loc.longitude,
                address = addr,
                onBack = {
                    mapViewModel.clearSelection()
                    showCreateReport = false
                }
            )
            return
        }
        showCreateReport = false
    }

    val context = LocalContext.current
    val uiState by mapViewModel.uiState.collectAsState()
    val savedCameraPosition by mapViewModel.savedCameraPosition.collectAsState()
    val searchQuery by mapViewModel.searchQuery.collectAsState()
    val suggestions by mapViewModel.suggestions.collectAsState()
    val selectedLocation by mapViewModel.selectedLocation.collectAsState()
    val selectedAddress by mapViewModel.selectedAddress.collectAsState()

    LaunchedEffect(Unit) {
        permissionsViewModel.requestAccessFineLocation {
            mapViewModel.fetchLocation()
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

    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(0.75f)) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                uiSettings = MapUiSettings(myLocationButtonEnabled = permissionsViewModel.hasFineLocationPermission()),
                properties = MapProperties(
                    isMyLocationEnabled = permissionsViewModel.hasFineLocationPermission()
                ),
                onMapClick = { latLng ->
                    mapViewModel.selectLocation(latLng, "${latLng.latitude}, ${latLng.longitude}")
                }
            ) {
                selectedLocation?.let { location ->
                    key(location) {
                        Marker(
                            state = rememberMarkerState(position = location),
                            title = selectedAddress.ifBlank { null },
                            snippet = null
                        )
                    }
                }
            }

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

            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
            ) {
                if (!isGpsEnabled) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
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
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { mapViewModel.searchLocation(it) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            placeholder = { Text("Ort oder Adresse suchen...") },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = "Suchen"
                                )
                            },
                            trailingIcon = if (selectedLocation != null) {
                                {
                                    IconButton(onClick = { mapViewModel.clearSelection() }) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Auswahl l\u00f6schen"
                                        )
                                    }
                                }
                            } else null,
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0f)
                            )
                        )

                        if (suggestions.isNotEmpty()) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.surface
                            ) {
                                Column {
                                    suggestions.forEach { suggestion ->
                                        Text(
                                            text = suggestion.text,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { mapViewModel.selectSuggestion(suggestion) }
                                                .padding(horizontal = 16.dp, vertical = 12.dp),
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.25f)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Place,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Sicherheitsberichte der Nutzer",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                FilledIconButton(
                    onClick = {
                        selectedLocation?.let { showCreateReport = true }
                    },
                    enabled = selectedLocation != null,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        disabledContainerColor = Color.Transparent,
                        disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    )
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Report erstellen")
                }
            }

            if (selectedLocation == null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Wähle einen Ort auf der Karte oder suche nach einer Adresse, um Berichte zu sehen.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}