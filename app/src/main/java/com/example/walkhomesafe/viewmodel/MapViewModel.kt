package com.example.walkhomesafe.viewmodel

import android.app.Application
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.walkhomesafe.api.ReportDto
import com.example.walkhomesafe.api.ReportService
import com.example.walkhomesafe.api.ReportVoteService
import com.example.walkhomesafe.api.SaveReportVoteDto
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Data class for an autocomplete suggestion from the Google Places API.
 *
 * @property placeId Unique ID of the place at Google Places
 * @property text Display text of the suggestion
 */
data class Suggestion(
    val placeId: String,
    val text: String
)

/**
 * Sealed interface for the map screen UI states.
 *
 * @property Loading Displayed during location determination
 * @property Location Location successfully determined, contains [LatLng]
 * @property Error Error during location determination with error message
 */
sealed interface MapUiState {
    data object Loading : MapUiState
    data class Location(val latLng: LatLng) : MapUiState
    data class Error(val message: String) : MapUiState
}

/**
 * ViewModel for the map screen.
 *
 * Manages location, camera position, reports, places, search, and heatmap overlay.
 *
 * @property uiState Current UI state
 * @property savedCameraPosition Saved camera position
 * @property autoFocusTrigger Trigger for automatic camera focusing
 * @property searchQuery Current search query
 * @property suggestions Autocomplete suggestions
 * @property selectedLocation Position selected by the user
 * @property selectedAddress Address of the selected position
 * @property showPublicLocations Whether public places are shown
 * @property showClosedPlaces Whether closed places are included in debug mode
 * @property showHeatmap Whether the heatmap overlay is visible
 * @property nearbyPlaces Nearby public places
 * @property isLoadingPlaces Loading state of places
 * @property reports Safety reports
 * @property isLoadingReports Loading state of reports
 * @property userVotes User's voting state (report ID → Boolean)
 */
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

    private val _showHeatmap = MutableStateFlow(true)
    val showHeatmap: StateFlow<Boolean> = _showHeatmap.asStateFlow()

    private val _nearbyPlaces = MutableStateFlow<List<NearbyPlace>>(emptyList())
    val nearbyPlaces: StateFlow<List<NearbyPlace>> = _nearbyPlaces.asStateFlow()

    private val _isLoadingPlaces = MutableStateFlow(false)
    val isLoadingPlaces: StateFlow<Boolean> = _isLoadingPlaces.asStateFlow()

    private val _reports = MutableStateFlow<List<ReportDto>>(emptyList())
    val reports: StateFlow<List<ReportDto>> = _reports.asStateFlow()

    private val _isLoadingReports = MutableStateFlow(false)
    val isLoadingReports: StateFlow<Boolean> = _isLoadingReports.asStateFlow()

    private val _userVotes = MutableStateFlow<Map<Int, Boolean>>(emptyMap())
    val userVotes: StateFlow<Map<Int, Boolean>> = _userVotes.asStateFlow()

    private var currentLocation: LatLng? = null
    private var placesFetchJob: Job? = null
    private var autoRefreshJob: Job? = null

    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()
    val savedCameraPosition: StateFlow<CameraPosition> = _savedCameraPosition.asStateFlow()
    var hasAnimated = false
    val autoFocusTrigger: StateFlow<Long> = _autoFocusTrigger.asStateFlow()
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    val suggestions: StateFlow<List<Suggestion>> = _suggestions.asStateFlow()
    val selectedLocation: StateFlow<LatLng?> = _selectedLocation.asStateFlow()
    val selectedAddress: StateFlow<String> = _selectedAddress.asStateFlow()

    /** Starts one-time location determination on first call. */
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

    /**
     * @param cameraPosition Current position of the map camera
     */
    fun updateSavedCameraPosition(cameraPosition: CameraPosition) {
        _savedCameraPosition.value = cameraPosition
    }

    /** Resets camera animation and requests a location update. */
    fun requestLocationRefresh() {
        hasAnimated = false
        viewModelScope.launch {
            tryFetchCurrentLocation(false)
        }
    }

    /**
     * Searches places via Google Places Autocomplete.
     * @param query The user's search query
     */
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

    /**
     * Selects an autocomplete suggestion and focuses the map on it.
     * @param suggestion The selected suggestion
     */
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

    /**
     * Sets a position manually and updates search field and selection.
     * @param latLng Coordinates of the position
     * @param address Address text for display
     */
    fun selectLocation(latLng: LatLng, address: String) {
        _selectedLocation.value = latLng
        _selectedAddress.value = address
        _searchQuery.value = address
        _suggestions.value = emptyList()
    }

    /** Resets the manual selection and clears the search field. */
    fun clearSelection() {
        _selectedLocation.value = null
        _selectedAddress.value = ""
        _searchQuery.value = ""
        _suggestions.value = emptyList()
    }

    /** Triggers a new report fetch for the current location. */
    fun refreshReports() {
        currentLocation?.let { fetchReports(it) }
    }

    /**
     * Votes on a report (upvote/downvote) or removes the vote.
     * @param reportId ID of the report
     * @param isUpvote true = upvote, false = downvote
     */
    fun voteOnReport(reportId: Int, isUpvote: Boolean) {
        viewModelScope.launch {
            val currentVote = _userVotes.value[reportId]
            val dto = SaveReportVoteDto(
                reportId = reportId,
                isUpvote = if (currentVote == isUpvote) null else isUpvote
            )
            val success = ReportVoteService.vote(listOf(dto))
            if (success) {
                val newVote = dto.isUpvote
                _userVotes.value = _userVotes.value.toMutableMap().apply {
                    if (newVote == null) remove(reportId)
                    else put(reportId, newVote)
                }
                _reports.value = _reports.value.map { report ->
                    if (report.id != reportId) return@map report
                    var up = report.upvoteCount
                    var down = report.downvoteCount
                    when (currentVote to newVote) {
                        true to null -> up--
                        true to false -> { up--; down++ }
                        false to null -> down--
                        false to true -> { down--; up++ }
                        null to true -> up++
                        null to false -> down++
                    }
                    report.copy(upvoteCount = up, downvoteCount = down)
                }
            }
        }
    }

    /** Toggles the display of public places on/off. */
    fun togglePublicLocationsFilter() {
        val newValue = !_showPublicLocations.value
        _showPublicLocations.value = newValue

        if (newValue && _nearbyPlaces.value.isEmpty()) {
            currentLocation?.let { fetchNearbyPlaces(it) }
        }
    }

    /**
     * Toggles the debug mode for closed places on/off.
     * Only takes effect when showPublicLocations is active.
     */
    fun toggleClosedPlacesFilter() {
        val newValue = !_showClosedPlaces.value
        _showClosedPlaces.value = newValue
        currentLocation?.let { fetchNearbyPlaces(it, includeClosed = newValue) }
    }

    /** Toggles the visibility of the heatmap overlay. */
    fun toggleHeatmap() {
        _showHeatmap.value = !_showHeatmap.value
    }

    /**
     * Attempts to determine the current location.
     * @param fallbackToDefault If true, falls back to default coordinates
     * @return true if the operation could be started
     */
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
        } catch (_: SecurityException) {
            if (fallbackToDefault) {
                updateUiWithLocation(defaultLocation)
            }
            false
        }
    }

    /**
     * Processes the result of location determination.
     * @param location Found location or null
     * @param fallbackToDefault Fall back to default coordinates if no location
     */
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
            } catch (_: SecurityException) {
                if (fallbackToDefault) {
                    updateUiWithLocation(defaultLocation)
                    _autoFocusTrigger.value = System.nanoTime()
                    onLocationUpdated(defaultLocation)
                }
            }
        }
    }

    /**
     * Called on every location update.
     * Starts queries for places and reports on significant position changes.
     * @param latLng New location
     */
    private fun onLocationUpdated(latLng: LatLng) {
        val isFirstLocation = currentLocation == null
        val shouldFetchPlaces = _showPublicLocations.value && hasLocationChangedSignificantly(currentLocation, latLng)
        val shouldFetchReports = hasLocationChangedSignificantly(currentLocation, latLng)
        currentLocation = latLng

        if (shouldFetchPlaces) {
            fetchNearbyPlaces(latLng)
        }
        if (shouldFetchReports) {
            fetchReports(latLng)
        }
        if (isFirstLocation) {
            startAutoRefresh()
        }
    }

    /**
     * Checks whether the location has changed by more than 100m.
     * @param oldLocation Previous location (null = significant change)
     * @param newLocation New location
     * @return true on significant change
     */
    private fun hasLocationChangedSignificantly(oldLocation: LatLng?, newLocation: LatLng): Boolean {
        if (oldLocation == null) return true

        val results = FloatArray(1)
        Location.distanceBetween(
            oldLocation.latitude, oldLocation.longitude,
            newLocation.latitude, newLocation.longitude,
            results
        )
        return results[0] > 100.0f
    }

    /**
     * Loads public places within a 800m radius.
     * @param location Center of the search
     * @param includeClosed Whether closed places should be included
     */
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
            } catch (_: Exception) {
                _nearbyPlaces.value = emptyList()
            } finally {
                _isLoadingPlaces.value = false
            }
        }
    }

    /**
     * Loads safety reports within a 800m radius and updates votes and heatmap.
     * @param location Center of the search
     */
    private fun fetchReports(location: LatLng) {
        viewModelScope.launch {
            _isLoadingReports.value = true
            try {
                _reports.value = ReportService.get(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    radiusInMeters = 800
                )
                val votes = ReportVoteService.getMyVotes()
                _userVotes.value = votes.associate { it.reportId to it.isUpvote }
            } catch (_: Exception) {
                _reports.value = emptyList()
            } finally {
                _isLoadingReports.value = false
            }
        }
    }

    /** Starts periodic report refresh every 60 seconds. */
    private fun startAutoRefresh() {
        if (autoRefreshJob?.isActive == true) return
        autoRefreshJob = viewModelScope.launch {
            while (true) {
                delay(60_000)
                currentLocation?.let { fetchReports(it) }
            }
        }
    }

    /** Stops the periodic refresh when the ViewModel is destroyed. */
    override fun onCleared() {
        super.onCleared()
        autoRefreshJob?.cancel()
    }

    /**
     * Updates UI state and camera position to the new location.
     * @param latLng New location
     */
    private fun updateUiWithLocation(latLng: LatLng) {
        _savedCameraPosition.value = CameraPosition.fromLatLngZoom(latLng, 15f)
        _uiState.value = MapUiState.Location(latLng)
    }

    /**
     * Determines the location for SMS functions (suspend).
     * @return Location or null on error
     */
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
            } catch (_: SecurityException) {
                cont.resume(null)
            }
        }
    }

    /**
     * Fallback for SMS location: returns the last known location.
     * @param cont Continuation for the asynchronous return value
     */
    private fun fetchLastForSms(cont: kotlinx.coroutines.CancellableContinuation<LatLng?>) {
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                cont.resume(if (location != null) LatLng(location.latitude, location.longitude) else null)
            }.addOnFailureListener {
                cont.resume(null)
            }
        } catch (_: SecurityException) {
            cont.resume(null)
        }
    }
}
