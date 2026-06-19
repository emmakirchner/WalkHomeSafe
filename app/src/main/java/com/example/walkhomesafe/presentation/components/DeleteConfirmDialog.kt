package com.example.walkhomesafe.presentation.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.style.TextAlign

@Composable
fun DeleteConfirmDialog(
    onDismiss: () -> Unit,
    onDelete: ((Boolean, String?) -> Unit) -> Unit,
    onReauthNeeded: () -> Unit,
) {
    var isDeleting by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Account l\u00f6schen") },
        text = {
            Text(
                text = buildString {
                    append("M\u00f6chtest du deinen Account wirklich l\u00f6schen? Diese Aktion kann nicht r\u00fcckg\u00e4ngig gemacht werden.\n\n")
                    append("Eintr\u00e4ge, die du in der Community verfasst hast, bleiben weiterhin sichtbar.")
                    if (errorText != null) {
                        append("\n\n")
                        append(errorText)
                    }
                },
                textAlign = TextAlign.Start
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    isDeleting = true
                    errorText = null
                    onDelete { success, error ->
                        isDeleting = false
                        if (success) {
                            onDismiss()
                        } else if (error == "RECENT_LOGIN_REQUIRED") {
                            onReauthNeeded()
                            onDismiss()
                        } else {
                            errorText = error ?: "Fehler beim L\u00f6schen"
                        }
                    }
                },
                enabled = !isDeleting,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(if (isDeleting) "Wird gel\u00f6scht..." else "L\u00f6schen")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}
