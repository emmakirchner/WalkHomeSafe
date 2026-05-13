package com.example.walkhomesafe.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.walkhomesafe.services.permissions.PermissionHost
import com.example.walkhomesafe.presentation.screens.MainScreen
import com.example.walkhomesafe.viewmodel.PermissionsViewModel

@Composable
fun AppRoot() {

    val permissionsViewModel: PermissionsViewModel = viewModel()

    PermissionHost(
        permissionFlow = permissionsViewModel.permissionRequests,
        onResult = { granted ->
            permissionsViewModel.onPermissionResult(granted)
        }
    )

    LaunchedEffect(Unit) {
        permissionsViewModel.requestStartupPermissions()
    }

    MainScreen()
}