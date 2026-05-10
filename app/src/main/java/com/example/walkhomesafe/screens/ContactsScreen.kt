package com.example.walkhomesafe.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.walkhomesafe.components.EmergencyContactList
import com.example.walkhomesafe.components.EmergencyMessageTextField
import com.example.walkhomesafe.model.*


@Composable
fun ContactsScreen(
    emergencyMessage: String,
    contacts: List<EmergencyContact>,
    onDeleteContact: (EmergencyContact) -> Unit,
    onAddContact: () -> Unit,
    onEmergencyMessageChange: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        EmergencyContactList(contacts, onDeleteContact, onAddContact)

        EmergencyMessageTextField(emergencyMessage, onEmergencyMessageChange)
    }
}



