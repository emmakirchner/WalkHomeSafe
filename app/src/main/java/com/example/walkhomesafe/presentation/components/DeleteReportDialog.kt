package com.example.walkhomesafe.presentation.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

/**
 * Confirmation dialog for deleting a report.
 * Shows the report title and provides confirm/cancel buttons.
 *
 * @param onDismiss Callback when the dialog is dismissed or cancelled
 * @param onConfirm Callback when deletion is confirmed
 * @param reportTitle Title of the report to delete (optional, for display)
 */
@Composable
fun DeleteReportDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    reportTitle: String = ""
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Report löschen") },
        text = {
            Text(
                if (reportTitle.isNotBlank()) "Möchtest du \"$reportTitle\" wirklich löschen?"
                else "Möchtest du diesen Report wirklich löschen?"
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Löschen")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}
