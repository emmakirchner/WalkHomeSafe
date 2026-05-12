package com.example.walkhomesafe.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ReportGmailerrorred
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.walkhomesafe.ui.theme.GrayLight
import com.example.walkhomesafe.ui.theme.GrayMedium
import com.example.walkhomesafe.ui.theme.GrayWhite
import com.example.walkhomesafe.ui.theme.RedBright
import com.example.walkhomesafe.ui.theme.RedDark

private const val CROSS = "\u2715"

@Composable
fun EmergencyActionButton(
    modifier: Modifier = Modifier,
    onShortPress: () -> Unit,
    onLongPressRelease: () -> Unit,
    onCancel: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    var cancelHovered by remember { mutableStateOf(false) }
    var isAlarmActive by remember { mutableStateOf(false) }
    val isWhite = isPressed || isAlarmActive

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(260.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(brush = backgroundBrush(isWhite))
            .pointerInput(isAlarmActive) {
                val cancelPx = 72.dp.toPx()
                detectTapGestures(
                    onPress = {
                        if (!isAlarmActive) {
                            isPressed = true
                            tryAwaitRelease()
                            isPressed = false
                        }
                    },
                    onTap = { offset ->
                        if (isAlarmActive) {
                            if (isInCancelZone(offset.x, offset.y, size.width.toFloat(), cancelPx)) {
                                isPressed = false
                                isAlarmActive = false
                                onCancel()
                            }
                        } else {
                            onShortPress()
                        }
                    }
                )
            }
            .pointerInput(isAlarmActive) {
                if (isAlarmActive) return@pointerInput
                val cancelPx = 72.dp.toPx()
                detectDragGesturesAfterLongPress(
                    onDragStart = { isPressed = true },
                    onDrag = { change, _ ->
                        val inZone = isInCancelZone(change.position.x, change.position.y, size.width.toFloat(), cancelPx)
                        if (inZone != cancelHovered) cancelHovered = inZone
                    },
                    onDragEnd = {
                        if (cancelHovered) {
                            onCancel()
                        } else {
                            onLongPressRelease()
                            isAlarmActive = true
                        }
                        isPressed = false
                        cancelHovered = false
                    },
                    onDragCancel = {
                        isPressed = false
                        cancelHovered = false
                    }
                )
            }
            .padding(24.dp)
    ) {
        SosContent(isWhite, isAlarmActive)
        if (isPressed || isAlarmActive) {
            CancelZone(Modifier.align(Alignment.TopEnd), cancelHovered)
        }
    }
}

private fun backgroundBrush(isWhite: Boolean) =
    if (isWhite) {
        Brush.verticalGradient(listOf(GrayLight, GrayWhite, GrayLight))
    } else {
        Brush.verticalGradient(listOf(RedDark, RedBright, RedDark))
    }

private fun isInCancelZone(x: Float, y: Float, width: Float, cancelPx: Float): Boolean {
    return x >= width - cancelPx && y <= cancelPx
}

@Composable
private fun SosContent(isWhite: Boolean, isAlarmActive: Boolean) {
    val textColor = if (isWhite) RedDark else Color.White
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)
    ) {
        Icon(
            imageVector = Icons.Filled.ReportGmailerrorred,
            contentDescription = null,
            tint = textColor,
            modifier = Modifier.size(48.dp)
        )
        Text(
            text = "SOS",
            color = textColor,
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            text = when {
                isAlarmActive ->  "Auf $CROSS tippen = Alarm stoppen"
                else -> "Tippen = Notfall-SMS senden"
            },
            color = textColor.copy(alpha = 0.8f),
            fontWeight = FontWeight.Medium
        )
        SosHintRow(isWhite, isAlarmActive)
    }
}

@Composable
private fun SosHintRow(isWhite: Boolean, isAlarmActive: Boolean) {
    val textColor = if (isWhite) RedDark else Color.White
    val label = when {
        isAlarmActive -> ""
        isWhite -> "Loslassen = Alarm starten"
        else -> "Halten = Lauter Alarm"
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Filled.VolumeUp,
            contentDescription = null,
            tint = textColor,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            color = textColor.copy(alpha = if (textColor == RedDark) 0.8f else 0.9f),
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun CancelZone(modifier: Modifier, isHovered: Boolean) {
    Box(
        modifier = modifier
            .padding(12.dp)
            .size(48.dp)
            .background(
                color = if (isHovered) RedBright else GrayMedium,
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = CROSS,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp
        )
    }
}
