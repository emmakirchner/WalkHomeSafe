package com.example.walkhomesafe.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.walkhomesafe.presentation.components.PasswordResetDialog
import com.example.walkhomesafe.viewmodel.AuthViewModel

/**
 * Authentication screen with login and registration forms.
 * Supports email/password login, registration with username, and password reset dialog.
 *
 * @param authViewModel ViewModel for authentication logic
 */
@Composable
fun AuthScreen(
    authViewModel: AuthViewModel = viewModel(),
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var isRegister by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    var feedback by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "WalkHomeSafe",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = if (isRegister) "Neues Konto erstellen" else "Anmelden",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(Modifier.height(32.dp))

        if (isRegister) {
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Benutzername") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))
        }

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("E-Mail") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Passwort") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))

        if (!isRegister) {
            TextButton(
                onClick = { showResetDialog = true },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Passwort vergessen?")
            }
        }

        Spacer(Modifier.height(8.dp))

        feedback?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(8.dp))
        }

        Button(
            onClick = {
                if (email.isBlank() || password.isBlank()) {
                    feedback = "Bitte E-Mail und Passwort eingeben"
                    return@Button
                }
                if (isRegister && username.isBlank()) {
                    feedback = "Bitte einen Benutzernamen eingeben"
                    return@Button
                }
                loading = true
                feedback = null
                if (isRegister) {
                    authViewModel.register(email, password, username) { success, error ->
                        loading = false
                        if (!success) {
                            feedback = error ?: "Ein Fehler ist aufgetreten"
                        }
                    }
                } else {
                    authViewModel.login(email, password) { success, error ->
                        loading = false
                        if (!success) {
                            feedback = error ?: "Ein Fehler ist aufgetreten"
                        }
                    }
                }
            },
            enabled = !loading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (loading) "Bitte warten..." else if (isRegister) "Registrieren" else "Einloggen")
        }

        Spacer(Modifier.height(12.dp))

        TextButton(onClick = {
            isRegister = !isRegister
            username = ""
            feedback = null
        }) {
            Text(
                if (isRegister) "Bereits ein Konto? Jetzt anmelden"
                else "Noch kein Konto? Jetzt registrieren"
            )
        }
    }

    if (showResetDialog) {
        PasswordResetDialog(
            initialEmail = email,
            onDismiss = { showResetDialog = false },
            onResetPassword = { resetEmail, callback ->
                authViewModel.resetPassword(resetEmail, callback)
            },
            onFeedback = { feedback = it }
        )
    }
}
