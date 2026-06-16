package com.example.walkhomesafe.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun PasswordResetDialog(
    initialEmail: String,
    onDismiss: () -> Unit,
    onResetPassword: (String, (Boolean, String?) -> Unit) -> Unit,
    onFeedback: (String) -> Unit,
) {
    var resetEmail by remember { mutableStateOf(initialEmail) }
    var resetLoading by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Passwort zur\u00fccksetzen") },
        text = {
            Column {
                Text(
                    "Gib deine E-Mail-Adresse ein. Wir senden dir einen Link zum Zur\u00fccksetzen deines Passworts.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = resetEmail,
                    onValueChange = { resetEmail = it },
                    label = { Text("E-Mail") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (resetEmail.isBlank()) return@Button
                    resetLoading = true
                    onResetPassword(resetEmail) { success, error ->
                        resetLoading = false
                        onDismiss()
                        onFeedback(if (success) "Link zum Zur\u00fccksetzen gesendet" else (error ?: "Fehler"))
                    }
                },
                enabled = !resetLoading
            ) {
                Text(if (resetLoading) "Wird gesendet..." else "Senden")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}
