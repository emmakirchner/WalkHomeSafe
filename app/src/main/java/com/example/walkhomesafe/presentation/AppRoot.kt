package com.example.walkhomesafe.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.walkhomesafe.presentation.permissions.PermissionHost
import com.example.walkhomesafe.presentation.permissions.PermissionIntent
import com.example.walkhomesafe.presentation.screens.MainScreen
import com.example.walkhomesafe.viewmodel.PermissionsViewModel

@Composable
fun AppRoot() {

    val context = LocalContext.current
    val permissionsViewModel: PermissionsViewModel = viewModel()

    LaunchedEffect(Unit) {
        permissionsViewModel.requestStartupPermissions(context)
    }

    PermissionHost(
        permissionFlow = permissionsViewModel.permissionRequests,
        onGranted = { granted ->
            when (granted) {
                PermissionIntent.SendSms ->
                    permissionsViewModel.onSendSmsRequested()

                PermissionIntent.Notifications ->
                    permissionsViewModel.onAlarmRequested()

                PermissionIntent.ReadContacts ->
                    permissionsViewModel.onReadContactsRequested()
            }
        }
    )

    MainScreen()
}