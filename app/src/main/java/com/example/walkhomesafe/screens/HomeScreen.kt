package com.example.walkhomesafe.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.walkhomesafe.components.EmergencyActionButton

@Composable
fun HomeScreen(
    onSendMessage: () -> Unit,
    onSendMessageAndAlarm: () -> Unit,
    onCancelAlarm: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
    ) {

        EmergencyActionButton(
            onShortPress = onSendMessage,
            onLongPressRelease = onSendMessageAndAlarm,
            onCancel = onCancelAlarm
        )
    }
}