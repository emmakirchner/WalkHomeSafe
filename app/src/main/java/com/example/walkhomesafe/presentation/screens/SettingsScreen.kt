package com.example.walkhomesafe.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import com.example.walkhomesafe.viewmodel.AuthViewModel

@Composable
fun SettingsScreen(
    authViewModel: AuthViewModel = viewModel(),
) {
    val authState by authViewModel.authState.collectAsState()
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showReauth by remember { mutableStateOf(false) }
    var deleteLoading by remember { mutableStateOf(false) }
    var deleteFeedback by remember { mutableStateOf<String?>(null) }

    var reauthEmail by remember { mutableStateOf("") }
    var reauthPassword by remember { mutableStateOf("") }
    var reauthLoading by remember { mutableStateOf(false) }
    var reauthError by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = "Einstellungen",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        val username = authState.username
        if (username != null) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = username,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.size(4.dp))
                Text(
                    text = "Angemeldet",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(32.dp))
        }

        Button(
            onClick = { authViewModel.logout() },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Abmelden")
        }

        Spacer(Modifier.height(12.dp))

        OutlinedButton(
            onClick = { showDeleteConfirm = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            )
        ) {
            Text("Account l\u00f6schen")
        }

        deleteFeedback?.let {
            Spacer(Modifier.height(8.dp))
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Account l\u00f6schen") },
            text = {
                Text(
                    "M\u00f6chtest du deinen Account wirklich l\u00f6schen? Diese Aktion kann nicht r\u00fcckg\u00e4ngig gemacht werden.\n\n" +
                            "Eintr\u00e4ge, die du in der Community verfasst hast, bleiben weiterhin sichtbar."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        deleteLoading = true
                        deleteFeedback = null
                        authViewModel.deleteAccount { success, error ->
                            deleteLoading = false
                            showDeleteConfirm = false
                            if (success) {
                                // Auth-Listener leitet automatisch zum AuthScreen um
                            } else if (error == "RECENT_LOGIN_REQUIRED") {
                                showReauth = true
                            } else {
                                deleteFeedback = error ?: "Fehler beim L\u00f6schen"
                            }
                        }
                    },
                    enabled = !deleteLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(if (deleteLoading) "Wird gel\u00f6scht..." else "L\u00f6schen")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Abbrechen")
                }
            }
        )
    }

    if (showReauth) {
        AlertDialog(
            onDismissRequest = { showReauth = false },
            title = { Text("Erneut anmelden") },
            text = {
                Column {
                    Text(
                        "Aus Sicherheitsgr\u00fcnden musst du dich erneut anmelden, um deinen Account zu l\u00f6schen.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = reauthEmail,
                        onValueChange = { reauthEmail = it },
                        label = { Text("E-Mail") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = reauthPassword,
                        onValueChange = { reauthPassword = it },
                        label = { Text("Passwort") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    reauthError?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (reauthEmail.isBlank() || reauthPassword.isBlank()) {
                            reauthError = "Bitte E-Mail und Passwort eingeben"
                            return@Button
                        }
                        reauthLoading = true
                        reauthError = null
                        authViewModel.reauthenticateAndDelete(reauthEmail, reauthPassword) { success, error ->
                            reauthLoading = false
                            if (success) {
                                showReauth = false
                            } else {
                                reauthError = error ?: "Fehler beim L\u00f6schen"
                            }
                        }
                    },
                    enabled = !reauthLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(if (reauthLoading) "Wird gel\u00f6scht..." else "L\u00f6schen")
                }
            },
            dismissButton = {
                TextButton(onClick = { showReauth = false }) {
                    Text("Abbrechen")
                }
            }
        )
    }
}
