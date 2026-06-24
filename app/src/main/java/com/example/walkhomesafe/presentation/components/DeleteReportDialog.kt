package com.example.walkhomesafe.presentation.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

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
