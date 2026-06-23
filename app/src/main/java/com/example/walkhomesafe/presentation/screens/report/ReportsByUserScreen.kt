package com.example.walkhomesafe.presentation.screens.report

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.walkhomesafe.api.ReportDto
import com.example.walkhomesafe.presentation.components.DeleteReportDialog
import com.example.walkhomesafe.presentation.components.ReportCard
import com.example.walkhomesafe.viewmodel.ReportViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsByUserScreen(
    onBack: () -> Unit,
    reportViewModel: ReportViewModel = viewModel()
) {
    var editReport by remember { mutableStateOf<ReportDto?>(null) }
    val uiState by reportViewModel.uiState.collectAsState()

    if (editReport != null) {
        val report = editReport ?: return
        CreateOrEditScreen(
            latitude = report.latitude,
            longitude = report.longitude,
            address = "${report.latitude}, ${report.longitude}",
            reportToEdit = report,
            onBack = {
                editReport = null
                reportViewModel.loadReportsByUser()
            },
            reportViewModel = reportViewModel
        )
        return
    }

    LaunchedEffect(Unit) {
        reportViewModel.loadReportsByUser()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Meine Reporte") },
                windowInsets = WindowInsets(0, 0, 0, 0),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Zurück"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                uiState.reportsByUserLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                uiState.reportsByUser.isEmpty() -> {
                    Text(
                        text = "Du hast noch keine Berichte erstellt.",
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(32.dp),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.reportsByUser, key = { it.id }) { report ->
                            var showDelete by remember { mutableStateOf(false) }
                            ReportCard(
                                report = report,
                                onEdit = { editReport = report },
                                onDelete = { showDelete = true }
                            )
                            if (showDelete) {
                                DeleteReportDialog(
                                    onDismiss = { showDelete = false },
                                    onConfirm = { reportViewModel.deleteReport(report.id) },
                                    reportTitle = report.title
                                )
                            }
                        }
                        item { Spacer(modifier = Modifier.height(16.dp)) }
                    }
                }
            }
        }
    }
}


