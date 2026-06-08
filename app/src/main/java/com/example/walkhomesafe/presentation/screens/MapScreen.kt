package com.example.walkhomesafe.presentation.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.location.LocationManager
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.outlined.Place
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.walkhomesafe.model.NearbyPlace
import com.example.walkhomesafe.model.PlaceType
import com.example.walkhomesafe.viewmodel.MapUiState
import com.example.walkhomesafe.viewmodel.MapViewModel
import android.util.Log
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.example.walkhomesafe.R
import com.example.walkhomesafe.viewmodel.PermissionsViewModel
import kotlinx.coroutines.delay

@Composable
fun MapScreen(
    mapViewModel: MapViewModel = viewModel(),
    permissionsViewModel: PermissionsViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by mapViewModel.uiState.collectAsState()
    val savedCameraPosition by mapViewModel.savedCameraPosition.collectAsState()

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

    val showPublicLocations by mapViewModel.showPublicLocations.collectAsState()
    val showClosedPlaces by mapViewModel.showClosedPlaces.collectAsState()
    val nearbyPlaces by mapViewModel.nearbyPlaces.collectAsState()

    val mapStyleOptions = remember(context) {
        try {
            MapStyleOptions.loadRawResourceStyle(context, R.raw.map_style_dark_no_pois)
        } catch (e: Exception) {
            Log.e("MapScreen", "Error loading map style", e)
            null
        }
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
            uiSettings = MapUiSettings(
                myLocationButtonEnabled = permissionsViewModel.hasFineLocationPermission(),
                mapToolbarEnabled = false
            ),
            properties = MapProperties(
                isMyLocationEnabled = permissionsViewModel.hasFineLocationPermission(),
                mapStyleOptions = mapStyleOptions
            ),
        ) {
            if (showPublicLocations) {
                nearbyPlaces.forEach { place ->
                    PlaceMarker(place = place)
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
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 180.dp)
        ) {
            PublicLocationsFilterButton(
                isEnabled = showPublicLocations,
                isDebugMode = showClosedPlaces,
                onClick = { mapViewModel.togglePublicLocationsFilter() },
                onLongClick = { mapViewModel.toggleClosedPlacesFilter() }
            )
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PublicLocationsFilterButton(
    isEnabled: Boolean,
    isDebugMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(56.dp)
            .clip(CircleShape)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .background(
                when {
                    isDebugMode -> MaterialTheme.colorScheme.tertiary
                    isEnabled -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.surfaceContainerHighest
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isEnabled || isDebugMode) Icons.Filled.Place else Icons.Outlined.Place,
            contentDescription = when {
                isDebugMode -> "Debug: Geschlossene Orte ausblenden"
                isEnabled -> "Filter: Offene Orte verstecken"
                else -> "Filter: Offene Orte anzeigen"
            },
            tint = when {
                isDebugMode -> MaterialTheme.colorScheme.onTertiary
                isEnabled -> MaterialTheme.colorScheme.onPrimary
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}

@Composable
private fun PlaceMarker(
    place: NearbyPlace
) {
    val markerState = remember(place.id) {
        MarkerState(position = place.latLng)
    }

    val statusText = when {
        place.isOpenNow == false -> "Geschlossen"
        place.closingTime == "24/7" -> "Geöffnet 24/7"
        place.closingTime != null -> "Geöffnet bis ${place.closingTime} Uhr"
        else -> "Geöffnet"
    }

    val snippetText = "${place.placeType.displayName} | $statusText"

    Marker(
        state = markerState,
        title = place.name,
        snippet = snippetText,
        icon = if (place.isOpenNow == false) {
            greyMarker
        } else {
            getMarkerIconForType(place.placeType)
        }
    )
}

private val greyMarker: BitmapDescriptor by lazy {
    val size = 40
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint().apply {
        color = android.graphics.Color.GRAY
        alpha = 200
    }
    canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
    BitmapDescriptorFactory.fromBitmap(bitmap)
}

private fun getMarkerIconForType(placeType: PlaceType): BitmapDescriptor {
    val hue = when (placeType) {
        PlaceType.RESTAURANT -> BitmapDescriptorFactory.HUE_ORANGE
        PlaceType.CAFE -> BitmapDescriptorFactory.HUE_YELLOW
        PlaceType.SHOP -> BitmapDescriptorFactory.HUE_AZURE
        PlaceType.LIBRARY -> BitmapDescriptorFactory.HUE_CYAN
        PlaceType.SUPERMARKET -> BitmapDescriptorFactory.HUE_GREEN
        PlaceType.PHARMACY -> BitmapDescriptorFactory.HUE_ROSE
        PlaceType.POLICE_STATION -> BitmapDescriptorFactory.HUE_BLUE
        PlaceType.HOSPITAL -> BitmapDescriptorFactory.HUE_RED
        PlaceType.GAS_STATION -> BitmapDescriptorFactory.HUE_VIOLET
    }
    return BitmapDescriptorFactory.defaultMarker(hue)
}
