package com.example.walkhomesafe.presentation.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.walkhomesafe.presentation.components.EmergencyActionButton
import com.example.walkhomesafe.ui.theme.FeedbackBanner
import com.example.walkhomesafe.viewmodel.ContactsViewModel
import com.example.walkhomesafe.viewmodel.HomeViewModel
import com.example.walkhomesafe.viewmodel.MessageViewModel
import com.example.walkhomesafe.viewmodel.PermissionsViewModel
import kotlinx.coroutines.delay

private const val FEEDBACK_TEMPLATE = "SMS an %d Kontakt(e) gesendet"

@Suppress("DefaultLocale")
@Composable
fun HomeScreen(
    homeViewModel: HomeViewModel = viewModel(),
    contactsViewModel: ContactsViewModel = viewModel(),
    messageViewModel: MessageViewModel = viewModel(),
    permissionsViewModel: PermissionsViewModel = viewModel()
) {
    val contacts by contactsViewModel.contacts.collectAsState()
    val message by messageViewModel.message.collectAsState()
    var feedbackMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(feedbackMessage) {
        if (feedbackMessage != null) {
            delay(2500)
            feedbackMessage = null
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            EmergencyActionButton(
                onShortPress = {
                    permissionsViewModel.requestSendSms {
                        homeViewModel.onSendMessage(contacts, message)
                        feedbackMessage = String.format(FEEDBACK_TEMPLATE, contacts.size)
                    }
                },
                onLongPressRelease = {
                    permissionsViewModel.requestSendSmsAndNotifications {
                        homeViewModel.onSendMessageAndAlarm(contacts, message)
                        feedbackMessage = String.format(FEEDBACK_TEMPLATE, contacts.size)
                    }
                },
                onCancel = homeViewModel::onCancelAlarm
            )
        }

        // Feedback banner shown after sending SMS
        AnimatedVisibility(
            visible = feedbackMessage != null,
            enter = slideInVertically { -it },
            exit = slideOutVertically { -it },
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Surface(
                Modifier.fillMaxWidth(),
                color = FeedbackBanner,
                shadowElevation = 8.dp
            ) {
                Text(
                    text = feedbackMessage ?: "",
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
