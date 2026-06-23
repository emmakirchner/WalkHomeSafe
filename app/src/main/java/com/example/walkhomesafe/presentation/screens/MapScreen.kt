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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
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
import com.google.android.gms.maps.model.LatLng
import com.example.walkhomesafe.R
import com.example.walkhomesafe.api.ReportDto
import com.example.walkhomesafe.api.ReportRatingDto
import com.example.walkhomesafe.viewmodel.PermissionsViewModel
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.ui.draw.rotate
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.TextButton
import androidx.compose.ui.window.Dialog
import androidx.compose.material.icons.filled.Info
import kotlinx.coroutines.delay

@Composable
fun MapScreen(
    mapViewModel: MapViewModel = viewModel(),
    permissionsViewModel: PermissionsViewModel = viewModel()
) {
    var showCreateReport by remember { mutableStateOf(false) }
    var collapsed by remember { mutableStateOf(false) }
    var visuallyCollapsed by remember { mutableStateOf(false) }
    var selectedReportId by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(collapsed) {
        if (collapsed) {
            delay(600)
            visuallyCollapsed = true
        } else {
            visuallyCollapsed = false
        }
    }
    val arrowRotation by animateFloatAsState(
        targetValue = if (collapsed) -90f else 0f,
        animationSpec = tween(durationMillis = 600),
        label = "arrowRotation"
    )

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
    val userVotes by mapViewModel.userVotes.collectAsState()
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
        Box(modifier = Modifier.weight(if (visuallyCollapsed) 1f else 0.75f)) {
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

            selectedReportId?.let { id ->
                val report = reports.find { it.id == id }
                report?.let { r ->
                    key("selected_report_${r.id}") {
                        Marker(
                            state = remember { MarkerState(position = LatLng(r.latitude, r.longitude)) },
                            title = r.title,
                            icon = selectedReportMarker
                        )
                    }
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
                .then(
                    if (visuallyCollapsed) Modifier.wrapContentHeight()
                    else Modifier.weight(0.25f)
                )
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { collapsed = !collapsed },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowDown,
                    contentDescription = if (collapsed) "Erweitern" else "Einklappen",
                    modifier = Modifier.rotate(arrowRotation),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(4.dp))
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

            AnimatedVisibility(
                visible = !collapsed,
                enter = expandVertically(
                    expandFrom = Alignment.Top,
                    animationSpec = tween(durationMillis = 600)
                ),
                exit = shrinkVertically(
                    shrinkTowards = Alignment.Top,
                    animationSpec = tween(durationMillis = 600)
                )
            ) {
                Column {
                    Spacer(modifier = Modifier.height(8.dp))

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    ) {
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
                            ReportCard(
                                report = report,
                                userVote = userVotes[report.id],
                                onVote = { isUpvote -> mapViewModel.voteOnReport(report.id, isUpvote) },
                                isSelected = selectedReportId == report.id,
                                onToggleSelect = {
                                    selectedReportId = if (selectedReportId == report.id) null else report.id
                                }
                            )
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

private val selectedReportMarker: BitmapDescriptor by lazy {
    val size = 60
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val fillPaint = Paint().apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    canvas.drawCircle(size / 2f, size / 2f, size / 2f - 2, fillPaint)
    val borderPaint = Paint().apply {
        color = android.graphics.Color.RED
        style = Paint.Style.STROKE
        strokeWidth = 5f
        isAntiAlias = true
    }
    canvas.drawCircle(size / 2f, size / 2f, size / 2f - 2, borderPaint)
    val textPaint = Paint().apply {
        color = android.graphics.Color.RED
        textSize = 36f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
        isAntiAlias = true
    }
    canvas.drawText("!", size / 2f, size / 2f + 14f, textPaint)
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
private fun ReportCard(report: ReportDto, userVote: Boolean?, onVote: (Boolean) -> Unit, isSelected: Boolean, onToggleSelect: () -> Unit) {
    var showDetails by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = report.title,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${report.userName} · ${report.createdAt.take(10)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { onVote(true) },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (userVote == true) Icons.Filled.ThumbUp
                        else Icons.Outlined.ThumbUp,
                        contentDescription = "Upvote",
                        tint = if (userVote == true) Color(0xFF4CAF50)
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(
                    text = report.upvoteCount.toString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(12.dp))
                IconButton(
                    onClick = { onVote(false) },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (userVote == false) Icons.Filled.ThumbDown
                        else Icons.Outlined.ThumbDown,
                        contentDescription = "Downvote",
                        tint = if (userVote == false) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(
                    text = report.downvoteCount.toString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = onToggleSelect) {
                    Text(
                        text = "Auf Karte",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isSelected) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(onClick = { showDetails = true }) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Details")
                }
            }
        }
    }

    if (showDetails) {
        ReportDetailsDialog(
            report = report,
            userVote = userVote,
            onVote = onVote,
            onDismiss = { showDetails = false }
        )
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

@Composable
private fun ReportDetailsDialog(
    report: ReportDto,
    userVote: Boolean?,
    onVote: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = report.title,
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Schließen")
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${report.userName} · ${report.createdAt.take(10)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (report.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = report.description,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { onVote(true) }) {
                        Icon(
                            imageVector = if (userVote == true) Icons.Filled.ThumbUp
                            else Icons.Outlined.ThumbUp,
                            contentDescription = "Upvote",
                            tint = if (userVote == true) Color(0xFF4CAF50)
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = report.upvoteCount.toString(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    IconButton(onClick = { onVote(false) }) {
                        Icon(
                            imageVector = if (userVote == false) Icons.Filled.ThumbDown
                            else Icons.Outlined.ThumbDown,
                            contentDescription = "Downvote",
                            tint = if (userVote == false) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = report.downvoteCount.toString(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (!report.ratingCategories.isNullOrEmpty()) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                    report.ratingCategories.forEach { category ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${category.name}: ",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            StarRating(rating = category.rating)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }
        }
    }
}
