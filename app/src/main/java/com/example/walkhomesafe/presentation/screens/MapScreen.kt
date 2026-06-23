package com.example.walkhomesafe.presentation.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.location.LocationManager
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Place
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
import com.example.walkhomesafe.model.NearbyPlace
import com.example.walkhomesafe.model.PlaceType
import com.example.walkhomesafe.presentation.screens.report.CreateOrEditScreen
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
import com.example.walkhomesafe.api.ReportDto
import com.example.walkhomesafe.api.ReportRatingDto
import com.example.walkhomesafe.viewmodel.PermissionsViewModel
import androidx.compose.animation.animateContentSize
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.HorizontalDivider
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
            CreateOrEditScreen(
                latitude = loc.latitude,
                longitude = loc.longitude,
                address = addr,
                onBack = {
                    mapViewModel.clearSelection()
                    mapViewModel.refreshReports()
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

    val showPublicLocations by mapViewModel.showPublicLocations.collectAsState()
    val showClosedPlaces by mapViewModel.showClosedPlaces.collectAsState()
    val nearbyPlaces by mapViewModel.nearbyPlaces.collectAsState()
    val reports by mapViewModel.reports.collectAsState()
    val isLoadingReports by mapViewModel.isLoadingReports.collectAsState()

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

    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(0.75f)) {
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
            onMapClick = { latLng ->
                mapViewModel.selectLocation(latLng, "${latLng.latitude}, ${latLng.longitude}")
            }
        ) {
            selectedLocation?.let { location ->
                key(location) {
                    Marker(
                        state = remember { MarkerState(position = location) },
                        title = selectedAddress.ifBlank { null },
                        snippet = null
                    )
                }
            }

            if (showPublicLocations) {
                val filteredPlaces = if (showClosedPlaces) {
                    nearbyPlaces
                } else {
                    nearbyPlaces.filter { it.isOpenNow == true || it.closingTime != null }
                }
                filteredPlaces.forEach { place ->
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
                .align(Alignment.TopStart)
                .padding(start = 8.dp, top = 8.dp)
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
                            text = "GPS ist deaktiviert. Aktiviere GPS f\u00fcr eine genaue Standortbestimmung.",
                            modifier = Modifier.padding(start = 8.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            Card(
                modifier = Modifier.fillMaxWidth(0.80f),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { mapViewModel.searchLocation(it) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Ort oder Adresse suchen...") },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = "Suchen")
                        },
                        trailingIcon = if (selectedLocation != null) {
                            {
                                IconButton(onClick = { mapViewModel.clearSelection() }) {
                                    Icon(Icons.Default.Close, contentDescription = "Auswahl l\u00f6schen")
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

        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 10.dp, bottom = 100.dp)
        ) {
            PublicLocationsFilterButton(
                isEnabled = showPublicLocations,
                isDebugMode = showClosedPlaces,
                onClick = { mapViewModel.togglePublicLocationsFilter() },
                onLongClick = { mapViewModel.toggleClosedPlacesFilter() }
            )
        }

        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.25f)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState())
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

            Spacer(modifier = Modifier.height(8.dp))

            if (isLoadingReports) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Lade Berichte...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (reports.isNotEmpty()) {
                reports.forEach { report ->
                    key(report.id) {
                        ReportCard(report = report)
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
            } else {
                Text(
                    text = "Keine Berichte in der Nähe gefunden.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
    Surface(
        modifier = modifier
            .size(40.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        shadowElevation = 6.dp
    ) {
        Icon(
            imageVector = if (isEnabled || isDebugMode) Icons.Filled.Place else Icons.Outlined.Place,
            contentDescription = when {
                isDebugMode -> "Debug: Geschlossene Orte ausblenden"
                isEnabled -> "Filter: Offene Orte verstecken"
                else -> "Filter: Offene Orte anzeigen"
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
        place.closingTime == "24/7" -> "Ge\u00f6ffnet 24/7"
        place.closingTime != null -> "Ge\u00f6ffnet bis ${place.closingTime} Uhr"
        place.isOpenNow == true -> "Ge\u00f6ffnet"
        else -> "Unbekannt"
    }

    val snippetText = "${place.placeType.displayName} | $statusText"

    Marker(
        state = markerState,
        title = place.name,
        snippet = snippetText,
        icon = when {
            place.isOpenNow == false -> greyMarker
            place.isOpenNow == null && place.closingTime == null -> unknownMarker
            else -> getMarkerIconForType(place.placeType)
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

private val unknownMarker: BitmapDescriptor by lazy {
    val size = 40
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint().apply {
        color = android.graphics.Color.LTGRAY
        alpha = 200
    }
    canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
    BitmapDescriptorFactory.fromBitmap(bitmap)
}

private val markerCache = mutableMapOf<PlaceType, BitmapDescriptor>()

private fun getMarkerIconForType(placeType: PlaceType): BitmapDescriptor {
    return markerCache.getOrPut(placeType) {
        val size = 40
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint().apply {
            color = placeType.darkerColor
            alpha = 255
        }
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
        BitmapDescriptorFactory.fromBitmap(bitmap)
    }
}

private val PlaceType.darkerColor: Int
    get() = when (this) {
        PlaceType.RESTAURANT -> android.graphics.Color.rgb(200, 80, 0)
        PlaceType.CAFE -> android.graphics.Color.rgb(200, 140, 0)
        PlaceType.SHOP -> android.graphics.Color.rgb(0, 100, 200)
        PlaceType.LIBRARY -> android.graphics.Color.rgb(0, 130, 130)
        PlaceType.SUPERMARKET -> android.graphics.Color.rgb(0, 130, 0)
        PlaceType.PHARMACY -> android.graphics.Color.rgb(190, 0, 120)
        PlaceType.POLICE_STATION -> android.graphics.Color.rgb(0, 45, 180)
        PlaceType.HOSPITAL -> android.graphics.Color.rgb(180, 0, 0)
        PlaceType.GAS_STATION -> android.graphics.Color.rgb(100, 0, 180)
    }

@Composable
private fun ReportCard(report: ReportDto) {
    var expanded by remember { mutableStateOf(false) }

    val netScore = report.upvoteCount - report.downvoteCount
    val scoreColor = when {
        netScore > 0 -> Color(0xFF4CAF50)
        netScore < 0 -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val scoreText = if (netScore > 0) "+$netScore" else netScore.toString()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .animateContentSize(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = report.title,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = scoreText,
                    style = MaterialTheme.typography.titleSmall,
                    color = scoreColor
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${report.userName} · ${report.createdAt.take(10)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (expanded && !report.ratingCategories.isNullOrEmpty()) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                report.ratingCategories.forEach { category ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${category.name}: ",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        StarRating(rating = category.rating)
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                }
            }
        }
    }
}

@Composable
private fun StarRating(rating: Int) {
    Row {
        repeat(5) { index ->
            Icon(
                imageVector = if (index < rating) Icons.Filled.Star else Icons.Outlined.Star,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = if (index < rating)
                    Color(0xFFFFC107)
                else
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
        }
    }
}
