package com.example.walkhomesafe.presentation.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.walkhomesafe.components.EmergencyActionButton
import com.example.walkhomesafe.viewmodel.ContactsViewModel
import com.example.walkhomesafe.viewmodel.HomeViewModel
import com.example.walkhomesafe.viewmodel.MessageViewModel
import com.example.walkhomesafe.viewmodel.PermissionsViewModel

@Composable
fun HomeScreen(
    homeViewModel: HomeViewModel = viewModel(),
    contactsViewModel: ContactsViewModel = viewModel(),
    messageViewModel: MessageViewModel = viewModel(),
    permissionsViewModel: PermissionsViewModel = viewModel()
) {
    val contacts by contactsViewModel.contacts.collectAsState()
    val message by messageViewModel.message.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
    ) {
        EmergencyActionButton(
            onShortPress = {
                permissionsViewModel.requestSendSms {
                    homeViewModel.onSendMessage(
                        contacts = contacts,
                        message = message
                    )
                }
            },
            onLongPressRelease = {
                permissionsViewModel.requestSendSmsAndNotifications {
                    homeViewModel.onSendMessageAndAlarm(
                        contacts = contacts,
                        message = message
                    )
                }
            },
            onCancel = homeViewModel::onCancelAlarm
        )
    }
}