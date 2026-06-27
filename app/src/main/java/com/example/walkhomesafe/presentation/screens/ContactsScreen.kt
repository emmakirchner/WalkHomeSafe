package com.example.walkhomesafe.presentation.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.walkhomesafe.services.ContactHelper
import com.example.walkhomesafe.presentation.components.EmergencyContactList
import com.example.walkhomesafe.presentation.components.EmergencyMessageTextField
import com.example.walkhomesafe.viewmodel.ContactsViewModel
import com.example.walkhomesafe.viewmodel.MessageViewModel
import com.example.walkhomesafe.viewmodel.PermissionsViewModel

/**
 * Contacts screen displaying the emergency contact list and the
 * emergency message text editor. Handles contact picker integration.
 *
 * @param contactsViewModel ViewModel for managing emergency contacts
 * @param messageViewModel ViewModel for the emergency message text
 * @param permissionsViewModel ViewModel for runtime permissions
 */
@Composable
fun ContactsScreen(
    contactsViewModel: ContactsViewModel = viewModel(),
    messageViewModel: MessageViewModel = viewModel(),
    permissionsViewModel: PermissionsViewModel = viewModel()
) {
    val contacts by contactsViewModel.contacts.collectAsState()
    val emergencyMessage by messageViewModel.message.collectAsState()
    val context = LocalContext.current
    val contactHelper = remember { ContactHelper(context) }

    val contactPickerLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.PickContact()
        ) { uri ->
            uri?.let {
                contactHelper.readContact(it) { name, number ->
                    contactsViewModel.addContact(name, number)
                }
            }
        }

    LaunchedEffect(Unit) {
        contactsViewModel.launchContactPicker.collect {
            contactPickerLauncher.launch(null)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        EmergencyContactList(
            contacts = contacts,
            onDeleteContact = contactsViewModel::deleteContact,
            onAddContact = {
                permissionsViewModel.requestReadContacts {
                    contactsViewModel.onReadContactsGranted()
                }
            }
        )

        EmergencyMessageTextField(
            value = emergencyMessage,
            onValueChange = messageViewModel::updateMessage
        )
    }
}


