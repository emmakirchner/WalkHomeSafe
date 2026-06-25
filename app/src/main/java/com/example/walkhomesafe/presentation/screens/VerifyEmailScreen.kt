package com.example.walkhomesafe.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.walkhomesafe.viewmodel.AuthViewModel

/**
 * Screen shown after registration prompting the user to verify their email address.
 * Provides a button to check verification status and an option to resend the verification email.
 *
 * @param authViewModel ViewModel for authentication logic
 */
@Composable
fun VerifyEmailScreen(
    authViewModel: AuthViewModel = viewModel(),
) {
    val authState by authViewModel.authState.collectAsState()
    val email = authState.firebaseUser?.email ?: ""
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
            text = "E-Mail bestätigen",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(Modifier.height(24.dp))

        Text(
            text = "Wir haben eine Bestätigungs-E-Mail an $email gesendet. Bitte prüfe dein Postfach und klicke auf den Bestätigungslink.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(8.dp))

        feedback?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(8.dp))
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = {
                loading = true
                feedback = null
                authViewModel.checkEmailVerified { verified ->
                    loading = false
                    if (verified) {
                        feedback = null
                    } else {
                        feedback = "E-Mail noch nicht bestätigt. Bitte klicke auf den Link in der E-Mail."
                    }
                }
            },
            enabled = !loading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (loading) "Wird überprüft..." else "Ich habe bestätigt - Weiter")
        }

        Spacer(Modifier.height(12.dp))

        TextButton(
            onClick = {
                loading = true
                feedback = null
                authViewModel.resendVerificationEmail { success, error ->
                    loading = false
                    feedback = if (success) "Bestätigungs-E-Mail erneut gesendet" else (error ?: "Fehler")
                }
            },
            enabled = !loading,
        ) {
            Text("Bestätigungs-E-Mail erneut senden")
        }

        Spacer(Modifier.height(12.dp))

        TextButton(onClick = { authViewModel.logout() }) {
            Text("Anderes Konto verwenden")
        }
    }
}
