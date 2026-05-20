package com.example.walkhomesafe.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.example.walkhomesafe.viewmodel.LOCATION_PLACEHOLDER
import com.example.walkhomesafe.viewmodel.MAX_MESSAGE_LENGTH

@Composable
fun EmergencyMessageTextField(
    value: String,
    onValueChange: (String) -> Unit
) {
    var localMessage by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue(value)) }
    val focusManager = LocalFocusManager.current
    var hasFocus by remember { mutableStateOf(false) }
    val hasChanges = localMessage.text != value
    LaunchedEffect(value) { localMessage = TextFieldValue(value) }

    val placeholderCount = localMessage.text.split(LOCATION_PLACEHOLDER).size - 1
    val effectiveLength = localMessage.text.length - placeholderCount * LOCATION_PLACEHOLDER.length

    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        colors = CardDefaults.outlinedCardColors(
            containerColor = Color.Transparent
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            Text(
                text = "Notfall‑Nachricht",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = "$effectiveLength / $MAX_MESSAGE_LENGTH",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (effectiveLength >= MAX_MESSAGE_LENGTH)
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            TextField(
                value = localMessage,
                onValueChange = {
                    val count = it.text.split(LOCATION_PLACEHOLDER).size - 1
                    val effective = it.text.length - count * LOCATION_PLACEHOLDER.length
                    if (effective <= MAX_MESSAGE_LENGTH) {
                        localMessage = it
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { hasFocus = it.isFocused },
                maxLines = 4,
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (hasFocus) {
                    TextButton(onClick = {
                        val cursorPos = localMessage.selection.start
                        val text = localMessage.text
                        val newText = text.substring(0, cursorPos) +
                            LOCATION_PLACEHOLDER + text.substring(cursorPos)
                        localMessage = localMessage.copy(
                            text = newText,
                            selection = TextRange(cursorPos + LOCATION_PLACEHOLDER.length)
                        )
                    }) {
                        Text("Standort-Link")
                    }
                }

                if (hasFocus && hasChanges) {
                    TextButton(
                        onClick = {
                            onValueChange(localMessage.text)
                            focusManager.clearFocus()
                        }
                    ) {
                        Text("Speichern")
                    }
                }
            }

            Text(
                text = "Diese Nachricht wird an alle Notfall‑Kontakte gesendet.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}