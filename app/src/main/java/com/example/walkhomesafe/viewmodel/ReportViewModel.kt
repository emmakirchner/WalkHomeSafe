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

class ReportViewModel() : ViewModel() {

    private val _uiState = MutableStateFlow(ReportUiState())
    val uiState: StateFlow<ReportUiState> = _uiState.asStateFlow()

    init {
        loadCategories()
    }

    private fun loadCategories() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                categories = ReportCategoryService.getAll(),
                loading = false
            )
        }
    }

    fun updateTitle(title: String) {
        _uiState.value = _uiState.value.copy(title = title)
    }

    fun updateDescription(description: String) {
        _uiState.value = _uiState.value.copy(description = description)
    }

    fun updateRating(categoryId: Int, rating: Int) {
        _uiState.value = _uiState.value.copy(
            ratings = _uiState.value.ratings + (categoryId to rating)
        )
    }

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

    fun deleteReport(id: Int) {
        viewModelScope.launch {
            val success = ReportService.delete(id)
            if (success) {
                _uiState.value = _uiState.value.copy(
                    reportsByUser = _uiState.value.reportsByUser.filter { it.id != id }
                )
            }
        }
    }

    fun editReport(report: ReportDto) {
        _uiState.value = _uiState.value.copy(
            editingReport = report,
            title = report.title,
            description = report.description,
            ratings = report.ratingCategories?.associate { it.id to it.rating } ?: emptyMap()
        )
    }
}
