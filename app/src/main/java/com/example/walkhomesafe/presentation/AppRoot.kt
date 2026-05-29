package com.example.walkhomesafe.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.walkhomesafe.services.permissions.PermissionHost
import com.example.walkhomesafe.presentation.screens.AuthScreen
import com.example.walkhomesafe.presentation.screens.MainScreen
import com.example.walkhomesafe.presentation.screens.VerifyEmailScreen
import com.example.walkhomesafe.viewmodel.AuthViewModel
import com.example.walkhomesafe.viewmodel.PermissionsViewModel

@Composable
fun AppRoot() {

    val authViewModel: AuthViewModel = viewModel()
    val permissionsViewModel: PermissionsViewModel = viewModel()

    val authState by authViewModel.authState.collectAsState()

    val startupPermissions = remember {
        permissionsViewModel.getPendingStartupPermissions()
    }

    PermissionHost(
        permissionFlow = permissionsViewModel.permissionRequests,
        startupPermissions = startupPermissions,
        onResult = { granted ->
            permissionsViewModel.onPermissionResult(granted)
        }
    )

    if (authState.firebaseUser == null) {
        AuthScreen(
            onLogin = { email, password, callback ->
                authViewModel.login(email, password, callback)
            },
            onRegister = { email, password, callback ->
                authViewModel.register(email, password, callback)
            },
            onResetPassword = { email, callback ->
                authViewModel.resetPassword(email, callback)
            }
        )
    } else if (!authState.isEmailVerified) {
        VerifyEmailScreen(
            email = authState.firebaseUser!!.email ?: "",
            onResend = { callback -> authViewModel.resendVerificationEmail(callback) },
            onRefresh = { callback -> authViewModel.checkEmailVerified(callback) },
            onLogout = { authViewModel.logout() }
        )
    } else {
        MainScreen(
            onLogout = { authViewModel.logout() }
        )
    }
}