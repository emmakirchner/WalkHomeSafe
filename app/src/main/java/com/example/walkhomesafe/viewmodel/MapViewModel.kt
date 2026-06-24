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
import com.example.walkhomesafe.model.Report
import com.example.walkhomesafe.model.Severity
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
 * Datenklasse für einen Autocomplete-Vorschlag der Google Places API.
 *
 * @property placeId Eindeutige ID des Ortes bei Google Places
 * @property text Anzeigetext des Vorschlags
 */
data class Suggestion(
    val placeId: String,
    val text: String
)

/**
 * Versiegelte Oberfläche für die UI-Zustände des Kartenbildschirms.
 *
 * @property Loading Wird während der Standortermittlung angezeigt
 * @property Location Standort erfolgreich ermittelt, enthält [LatLng]
 * @property Error Fehler bei der Standortermittlung mit Fehlermeldung
 */
sealed interface MapUiState {
    data object Loading : MapUiState
    data class Location(val latLng: LatLng) : MapUiState
    data class Error(val message: String) : MapUiState
}

/**
 * ViewModel für den Kartenbildschirm.
 *
 * Verwaltet Standort, Kameraposition, Berichte, Orte, Suche und Heatmap-Überlagerung.
 *
 * @property uiState Aktueller UI-Zustand
 * @property savedCameraPosition Gespeicherte Kameraposition
 * @property autoFocusTrigger Auslöser für automatische Kamerafokussierung
 * @property searchQuery Aktuelle Suchanfrage
 * @property suggestions Autocomplete-Vorschläge
 * @property selectedLocation Vom Nutzer ausgewählte Position
 * @property selectedAddress Adresse der ausgewählten Position
 * @property showPublicLocations Ob öffentliche Orte angezeigt werden
 * @property showClosedPlaces Ob geschlossene Orte im Debug-Modus einbezogen werden
 * @property showHeatmap Ob die Heatmap-Überlagerung sichtbar ist
 * @property nearbyPlaces Nahegelegene öffentliche Orte
 * @property isLoadingPlaces Ladezustand der Orte
 * @property reports Sicherheitsberichte
 * @property isLoadingReports Ladezustand der Berichte
 * @property userVotes Voting-Stand des Nutzers (Report-ID → Boolean)
 * @property heatmapReports Aufbereitete Berichte für die Heatmap
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

    private val _heatmapReports = MutableStateFlow<List<Report>>(emptyList())
    val heatmapReports: StateFlow<List<Report>> = _heatmapReports.asStateFlow()

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

    /** Startet die einmalige Standortermittlung beim ersten Aufruf. */
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
     * @param cameraPosition Aktuelle Position der Kartenkamera
     */
    fun updateSavedCameraPosition(cameraPosition: CameraPosition) {
        _savedCameraPosition.value = cameraPosition
    }

    /** Setzt Kamera-Animation zurück und fordert eine Standortaktualisierung an. */
    fun requestLocationRefresh() {
        hasAnimated = false
        viewModelScope.launch {
            tryFetchCurrentLocation(false)
        }
    }

    /**
     * Sucht Orte per Google Places Autocomplete.
     * @param query Die Suchanfrage des Nutzers
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
     * Wählt einen Autocomplete-Vorschlag aus und fokussiert die Karte darauf.
     * @param suggestion Der ausgewählte Vorschlag
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
     * Setzt eine Position manuell und aktualisiert Suchfeld und Auswahl.
     * @param latLng Koordinaten der Position
     * @param address Address-Text für die Anzeige
     */
    fun selectLocation(latLng: LatLng, address: String) {
        _selectedLocation.value = latLng
        _selectedAddress.value = address
        _searchQuery.value = address
        _suggestions.value = emptyList()
    }

    /** Setzt die manuelle Auswahl zurück und leert das Suchfeld. */
    fun clearSelection() {
        _selectedLocation.value = null
        _selectedAddress.value = ""
        _searchQuery.value = ""
        _suggestions.value = emptyList()
    }

    /** Löst eine erneute Berichtsabfrage für den aktuellen Standort aus. */
    fun refreshReports() {
        currentLocation?.let { fetchReports(it) }
    }

    /**
     * Stimmt für einen Bericht ab (Upvote/Downvote) oder hebt die Stimme auf.
     * @param reportId ID des Berichts
     * @param isUpvote true = Upvote, false = Downvote
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

    /** Schaltet die Anzeige öffentlicher Orte ein/aus. */
    fun togglePublicLocationsFilter() {
        val newValue = !_showPublicLocations.value
        _showPublicLocations.value = newValue

        if (newValue && _nearbyPlaces.value.isEmpty()) {
            currentLocation?.let { fetchNearbyPlaces(it) }
        }
    }

    /**
     * Schaltet den Debug-Modus für geschlossene Orte ein/aus.
     * Hat nur Wirkung, wenn showPublicLocations aktiv ist.
     */
    fun toggleClosedPlacesFilter() {
        val newValue = !_showClosedPlaces.value
        _showClosedPlaces.value = newValue
        currentLocation?.let { fetchNearbyPlaces(it, includeClosed = newValue) }
    }

    /** Schaltet die Sichtbarkeit der Heatmap-Überlagerung ein/aus. */
    fun toggleHeatmap() {
        _showHeatmap.value = !_showHeatmap.value
    }

    /**
     * Versucht den aktuellen Standort zu ermitteln.
     * @param fallbackToDefault Bei true wird auf Standardkoordinaten zurückgefallen
     * @return true, wenn der Vorgang gestartet werden konnte
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
     * Verarbeitet das Ergebnis der Standortermittlung.
     * @param location Gefundener Standort oder null
     * @param fallbackToDefault Auf Standardkoordinaten zurückfallen wenn kein Standort
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
     * Wird bei jeder Standortaktualisierung aufgerufen.
     * Startet Abfragen für Orte und Berichte bei signifikanter Positionsänderung.
     * @param latLng Neuer Standort
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
     * Prüft, ob sich der Standort um mehr als 100m geändert hat.
     * @param oldLocation Vorheriger Standort (null = signifikante Änderung)
     * @param newLocation Neuer Standort
     * @return true bei signifikanter Änderung
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
     * Lädt öffentliche Orte im Umkreis von 800m.
     * @param location Mittelpunkt der Suche
     * @param includeClosed Ob geschlossene Orte einbezogen werden
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
     * Lädt Sicherheitsberichte aus dem Umkreis von 800m und aktualisiert Votes und Heatmap.
     * @param location Mittelpunkt der Suche
     */
    private fun fetchReports(location: LatLng) {
        viewModelScope.launch {
            _isLoadingReports.value = true
            try {
                val allReports = ReportService.get()
                _reports.value = allReports.filter { report ->
                    val results = FloatArray(1)
                    Location.distanceBetween(
                        location.latitude, location.longitude,
                        report.latitude, report.longitude,
                        results
                    )
                    results[0] <= 800f
                }
                updateHeatmapReports(_reports.value)
                val votes = ReportVoteService.getMyVotes()
                _userVotes.value = votes.associate { it.reportId to it.isUpvote }
            } catch (_: Exception) {
                _reports.value = emptyList()
                updateHeatmapReports(emptyList())
            } finally {
                _isLoadingReports.value = false
            }
        }
    }

    /**
     * Wandelt Berichte in das Heatmap-Format um.
     * Berechnet Gewichtung, Polarität (positiv/negativ) und Schweregrad (Severity).
     * @param dtos Rohdaten der Berichte aus der API
     */
    private fun updateHeatmapReports(dtos: List<ReportDto>) {
        _heatmapReports.value = dtos.mapNotNull { dto ->
            val cats = dto.ratingCategories
            if (cats.isNullOrEmpty()) return@mapNotNull null

            val totalStars = cats.sumOf { it.rating }
            val maxStars = cats.size * 5
            val threshold = 9
            val minStars = cats.size

            val isPositive = totalStars > threshold
            val weight = if (isPositive) {
                (totalStars - threshold).toDouble() / (maxStars - threshold)
            } else {
                (threshold - totalStars).toDouble() / (threshold - minStars)
            }.coerceIn(0.0, 1.0)

            val severity = when {
                weight >= 0.75 -> Severity.CRITICAL
                weight >= 0.50 -> Severity.HIGH
                weight >= 0.25 -> Severity.MEDIUM
                else -> Severity.LOW
            }

            Report(
                id = dto.id.toString(),
                title = dto.title,
                latitude = dto.latitude,
                longitude = dto.longitude,
                severity = severity,
                isPositive = isPositive
            )
        }
    }

    /** Startet die periodische Berichtsaktualisierung alle 60 Sekunden. */
    private fun startAutoRefresh() {
        if (autoRefreshJob?.isActive == true) return
        autoRefreshJob = viewModelScope.launch {
            while (true) {
                delay(60_000)
                currentLocation?.let { fetchReports(it) }
            }
        }
    }

    /** Stoppt die periodische Aktualisierung beim Zerstören des ViewModels. */
    override fun onCleared() {
        super.onCleared()
        autoRefreshJob?.cancel()
    }

    /**
     * Aktualisiert UI-Zustand und Kameraposition auf den neuen Standort.
     * @param latLng Neuer Standort
     */
    private fun updateUiWithLocation(latLng: LatLng) {
        _savedCameraPosition.value = CameraPosition.fromLatLngZoom(latLng, 15f)
        _uiState.value = MapUiState.Location(latLng)
    }

    /**
     * Ermittelt den Standort für SMS-Funktionen (suspend).
     * @return Standort oder null bei Fehler
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
     * Fallback für SMS-Standort: liefert den letzten bekannten Standort.
     * @param cont Continuation für den asynchronen Rückgabewert
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
