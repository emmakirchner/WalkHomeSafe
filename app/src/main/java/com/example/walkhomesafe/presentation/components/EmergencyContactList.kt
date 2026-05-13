package com.example.walkhomesafe.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.walkhomesafe.model.EmergencyContact

@Composable
fun EmergencyContactList(
    contacts: List<EmergencyContact>,
    onDeleteContact: (EmergencyContact) -> Unit,
    onAddContact: () -> Unit
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        colors = CardDefaults.outlinedCardColors(containerColor = Color.Transparent)
    ) {

        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Notfall-Kontakte",
                style = MaterialTheme.typography.titleMedium
            )

            LazyColumn(
                modifier = Modifier.heightIn(max = 300.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                items(
                    items = contacts,
                    key = { it.id }
                ) { contact ->
                    EmergencyContactItem(
                        contact = contact,
                        onDelete = { onDeleteContact(contact) }
                    )
                }

                item {
                    OutlinedButton(
                        onClick = onAddContact,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PersonAdd,
                            contentDescription = null
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Kontakt hinzufügen")
                    }
                }
            }
        }
    }
}

