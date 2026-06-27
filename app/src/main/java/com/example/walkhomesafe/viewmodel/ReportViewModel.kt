package com.example.walkhomesafe.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.walkhomesafe.api.ReportCategoryDto
import com.example.walkhomesafe.api.ReportCategoryService
import com.example.walkhomesafe.api.ReportDto
import com.example.walkhomesafe.api.ReportRatingDto
import com.example.walkhomesafe.api.ReportService
import com.example.walkhomesafe.api.SaveReportDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * UI state for the report creation/editing and user reports screens.
 *
 * @property categories Available rating categories from the API
 * @property ratings Map of category ID to star rating
 * @property title Report title input
 * @property description Report description input
 * @property loading Whether categories are being loaded
 * @property saving Whether the report is being saved
 * @property showErrors Whether validation errors should be displayed
 * @property showSuccess Whether the last save operation was successful
 * @property showError Error message to display, or null
 * @property reportsByUser List of reports created by the current user
 * @property reportsByUserLoading Whether user reports are being loaded
 * @property editingReport The report being edited, or null for new reports
 */
data class ReportUiState(
    val categories: List<ReportCategoryDto> = emptyList(),
    val ratings: Map<Int, Int> = emptyMap(),
    val title: String = "",
    val description: String = "",
    val loading: Boolean = true,
    val saving: Boolean = false,
    val showErrors: Boolean = false,
    val showSuccess: Boolean = false,
    val showError: String? = null,
    val reportsByUser: List<ReportDto> = emptyList(),
    val reportsByUserLoading: Boolean = true,
    val editingReport: ReportDto? = null
)

/**
 * ViewModel for creating, editing, viewing, and deleting safety reports.
 *
 * @property uiState StateFlow of the report UI state
 */
class ReportViewModel() : ViewModel() {

    private val _uiState = MutableStateFlow(ReportUiState())
    val uiState: StateFlow<ReportUiState> = _uiState.asStateFlow()

    init {
        loadCategories()
    }

    /**
     * Loads the available report rating categories from the API.
     */
    private fun loadCategories() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                categories = ReportCategoryService.getAll(),
                loading = false
            )
        }
    }

    /**
     * Updates the report title in the UI state.
     *
     * @param title The new title text
     */
    fun updateTitle(title: String) {
        _uiState.value = _uiState.value.copy(title = title)
    }

    /**
     * Updates the report description in the UI state.
     *
     * @param description The new description text
     */
    fun updateDescription(description: String) {
        _uiState.value = _uiState.value.copy(description = description)
    }

    /**
     * Sets the rating for a specific category.
     *
     * @param categoryId The category ID
     * @param rating The star rating value
     */
    fun updateRating(categoryId: Int, rating: Int) {
        _uiState.value = _uiState.value.copy(
            ratings = _uiState.value.ratings + (categoryId to rating)
        )
    }

    /**
     * Resets the report form to its initial empty state.
     */
    fun resetForm() {
        _uiState.value = _uiState.value.copy(
            title = "",
            description = "",
            ratings = emptyMap(),
            saving = false,
            showErrors = false,
            showSuccess = false,
            showError = null,
            editingReport = null
        )
    }

    /**
     * Saves the report (creates a new one or updates an existing one).
     * Validates that title, description, and all ratings are filled.
     *
     * @param latitude Latitude of the report location
     * @param longitude Longitude of the report location
     */
    fun save(latitude: Double, longitude: Double) {
        val state = _uiState.value
        if (state.title.isBlank() || state.description.isBlank() || state.categories.any { (state.ratings[it.id] ?: 0) == 0 }) {
            _uiState.value = state.copy(showErrors = true)
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(saving = true, showError = null)
            val dto = SaveReportDto(
                title = state.title,
                description = state.description,
                latitude = latitude,
                longitude = longitude,
                ratingCategories = state.categories.map { cat ->
                    ReportRatingDto(
                        name = cat.name,
                        rating = state.ratings[cat.id] ?: 0
                    )
                }
            )
            val responseCode = if (state.editingReport != null) {
                ReportService.update(state.editingReport.id, dto)
            } else {
                ReportService.create(dto)
            }
            _uiState.value = _uiState.value.copy(
                saving = false,
                showSuccess = responseCode == 200,
                showError = if (responseCode == 200) null
                    else "Fehler beim Speichern (${responseCode ?: "–"})"
            )
        }
    }

    /**
     * Loads all reports created by the current user from the API.
     */
    fun loadReportsByUser() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(reportsByUserLoading = true)
            val reports = ReportService.getForCurrentUser()
            _uiState.value = _uiState.value.copy(
                reportsByUser = reports,
                reportsByUserLoading = false
            )
        }
    }

    /**
     * Deletes a report by its ID and removes it from the local list on success.
     *
     * @param id The ID of the report to delete
     */
    fun deleteReport(id: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(showError = null)
            val success = ReportService.delete(id)
            if (success) {
                _uiState.value = _uiState.value.copy(
                    reportsByUser = _uiState.value.reportsByUser.filter { it.id != id }
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    showError = "Report konnte nicht gelöscht werden"
                )
            }
        }
    }

    /**
     * Loads a report's data into the form for editing.
     *
     * @param report The report to edit
     */
    fun editReport(report: ReportDto) {
        _uiState.value = _uiState.value.copy(
            editingReport = report,
            title = report.title,
            description = report.description,
            ratings = report.ratingCategories?.associate { it.id to it.rating } ?: emptyMap()
        )
    }
}
