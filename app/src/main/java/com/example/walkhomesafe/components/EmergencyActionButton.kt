package com.example.walkhomesafe.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ReportGmailerrorred
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.Brush

enum class ButtonState {
    Idle,
    Pressed,
}

@Composable
fun EmergencyActionButton(
    modifier: Modifier = Modifier,
    onShortPress: () -> Unit,
    onLongPressRelease: () -> Unit,
    onCancel: () -> Unit
) {
    var state by remember { mutableStateOf(ButtonState.Idle) }
    var cancelActive by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(260.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFB00010),
                        Color(0xFFE30613),
                        Color(0xFFB00010)
                    )
                )
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onShortPress() },
                    onLongPress = { state = ButtonState.Pressed },
                    onPress = {
                        val released = tryAwaitRelease()
                        if (state == ButtonState.Pressed && released) {
                            if (cancelActive) onCancel()
                            else onLongPressRelease()
                        }
                        state = ButtonState.Idle
                        cancelActive = false
                    }
                )
            }
            .padding(24.dp)
    ) {

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            Icon(
                imageVector = Icons.Filled.ReportGmailerrorred,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(48.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "SOS",
                color = Color.White,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.ExtraBold
            )

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Tippen = Notfall‑SMS senden",
                color = Color.White,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.VolumeUp,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text =
                        if (state == ButtonState.Pressed)
                            "Loslassen = Alarm starten"
                        else
                            "Halten = Lauter Alarm",
                    color = Color.White.copy(alpha = 0.9f),
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (state == ButtonState.Pressed) {
            CancelZone(
                modifier = Modifier.align(Alignment.TopEnd),
                onHover = { cancelActive = it }
            )
        }
    }
}

@Composable
fun CancelZone(
    modifier: Modifier,
    onHover: (Boolean) -> Unit
) {
    Box(
        modifier = modifier
            .padding(12.dp)
            .size(48.dp)
            .background(Color.DarkGray, CircleShape)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        onHover(true)
                        tryAwaitRelease()
                        onHover(false)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text("✕", color = Color.White)
    }
}
