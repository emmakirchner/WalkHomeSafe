package com.example.walkhomesafe.presentation.screens

import android.content.Context
import android.location.LocationManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.walkhomesafe.presentation.components.EmergencyActionButton
import com.example.walkhomesafe.presentation.widget.WidgetSosAction
import com.example.walkhomesafe.presentation.widget.WidgetTrigger
import com.example.walkhomesafe.services.WalkHomeTimerState
import com.example.walkhomesafe.services.WalkHomeTimerState.TimerPhase
import com.example.walkhomesafe.ui.theme.FeedbackBanner
import com.example.walkhomesafe.ui.theme.RedBright
import com.example.walkhomesafe.ui.theme.RedDark
import com.example.walkhomesafe.viewmodel.ContactsViewModel
import com.example.walkhomesafe.viewmodel.HomeViewModel
import com.example.walkhomesafe.viewmodel.MapViewModel
import com.example.walkhomesafe.viewmodel.MessageViewModel
import com.example.walkhomesafe.viewmodel.PermissionsViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val FEEDBACK_TEMPLATE = "SMS an %d Kontakt(e) gesendet"

/**
 * Home screen composable. Displays the emergency action (SOS) button and the
 * walk-home timer section. Monitors GPS status and handles widget-triggered actions.
 *
 * @param homeViewModel ViewModel for alarm and timer logic
 * @param contactsViewModel ViewModel for emergency contacts
 * @param messageViewModel ViewModel for the emergency message text
 * @param permissionsViewModel ViewModel for runtime permissions
 * @param mapViewModel ViewModel for location determination
 */
@Suppress("DefaultLocale")
@Composable
fun HomeScreen(
    homeViewModel: HomeViewModel = viewModel(),
    contactsViewModel: ContactsViewModel = viewModel(),
    messageViewModel: MessageViewModel = viewModel(),
    permissionsViewModel: PermissionsViewModel = viewModel(),
    mapViewModel: MapViewModel = viewModel()
) {
    val contacts by contactsViewModel.contacts.collectAsState()
    val message by messageViewModel.message.collectAsState()
    val isAlarmActive by homeViewModel.isAlarmActive.collectAsState()
    val timerState by homeViewModel.timerState.collectAsState()
    val remainingSeconds by homeViewModel.remainingSeconds.collectAsState()
    val timerEndTime by homeViewModel.timerEndTime.collectAsState()
    val timerDurationInput by homeViewModel.timerDurationInput.collectAsState()
    var feedbackMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val locationManager = remember {
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }
    var isGpsEnabled by remember {
        mutableStateOf(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))
    }
    var wasGpsOff by remember { mutableStateOf(!isGpsEnabled) }

    /**
     * Sends an emergency SMS with the current location (if GPS is enabled and permission granted).
     */
    fun sendSmsWithLocation() {
        permissionsViewModel.requestAccessFineLocation(
            onGranted = {
                scope.launch {
                    val latLng = if (isGpsEnabled) {
                        mapViewModel.requestLocationForSms()
                    } else {
                        null
                    }
                    homeViewModel.onSendMessage(contacts, message, latLng)
                    feedbackMessage = String.format(FEEDBACK_TEMPLATE, contacts.size)
                }
            },
            onDenied = {
                homeViewModel.onSendMessage(contacts, message, null)
                feedbackMessage = String.format(FEEDBACK_TEMPLATE, contacts.size)
            }
        )
    }

    /**
     * Requests SMS permission and sends the emergency SMS.
     */
    fun triggerSmsAction() {
        permissionsViewModel.requestSendSms { sendSmsWithLocation() }
    }

    /**
     * Requests SMS and notification permissions, starts the alarm service,
     * and sends the emergency SMS.
     */
    fun triggerAlarmAction() {
        permissionsViewModel.requestSendSmsAndNotifications {
            homeViewModel.startAlarmService()
            sendSmsWithLocation()
        }
    }

    LaunchedEffect(Unit) {
        if (permissionsViewModel.hasFineLocationPermission()) {
            mapViewModel.fetchLocation()
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            val current = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            isGpsEnabled = current
            if (current && wasGpsOff) {
                mapViewModel.requestLocationRefresh()
            }
            wasGpsOff = !current
            delay(3000)
        }
    }

    LaunchedEffect(feedbackMessage) {
        if (feedbackMessage != null) {
            delay(2500)
            feedbackMessage = null
        }
    }

    LaunchedEffect(Unit) {
        WidgetTrigger.action.collect { action ->
            if (action != null && WidgetTrigger.consume() != null) {
                when (action) {
                    WidgetSosAction.SMS -> triggerSmsAction()
                    WidgetSosAction.ALARM -> triggerAlarmAction()
                }
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            EmergencyActionButton(
                isAlarmActive = isAlarmActive,
                onShortPress = { triggerSmsAction() },
                onLongPressRelease = { triggerAlarmAction() },
                onCancel = homeViewModel::stopAlarmService
            )

            Spacer(Modifier.height(16.dp))

            WalkHomeTimerSection(
                timerState = timerState,
                remainingSeconds = remainingSeconds,
                timerEndTime = timerEndTime,
                timerDurationInput = timerDurationInput,
                hasContacts = contacts.isNotEmpty(),
                onSetDuration = homeViewModel::setTimerDuration,
                onStart = {
                    permissionsViewModel.requestPostNotifications(
                        onGranted = homeViewModel::startTimer
                    )
                },
                onDeactivate = homeViewModel::deactivateTimer
            )
        }

        if (!isGpsEnabled) {
            Card(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Text(
                        text = "GPS ist deaktiviert. Aktiviere GPS für eine genaue Standortbestimmung.",
                        modifier = Modifier.padding(start = 8.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
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

/**
 * Formats seconds into a mm:ss display string.
 *
 * @param seconds Total seconds to format
 * @return Formatted string in "m:ss" format
 */
private fun formatTime(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%d:%02d".format(m, s)
}

/**
 * Composable section displaying the walk-home timer UI.
 * Shows different states depending on the timer phase: idle, countdown, expired, reminder, emergency.
 *
 * @param timerState Current state of the walk-home timer
 * @param remainingSeconds Seconds remaining in the current phase
 * @param timerEndTime Timer end time in milliseconds since epoch
 * @param timerDurationInput Current duration input value in minutes
 * @param hasContacts Whether emergency contacts are configured
 * @param onSetDuration Callback to change the duration
 * @param onStart Callback to start the timer
 * @param onDeactivate Callback to deactivate the timer
 */
@Composable
private fun WalkHomeTimerSection(
    timerState: WalkHomeTimerState.TimerState,
    remainingSeconds: Int,
    timerEndTime: Long,
    timerDurationInput: Int,
    hasContacts: Boolean,
    onSetDuration: (Int) -> Unit,
    onStart: () -> Unit,
    onDeactivate: () -> Unit
) {
    var showInfoDialog by remember { mutableStateOf(false) }

    if (showInfoDialog) {
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            title = { Text("Heimweg-Timer") },
            text = {
                Text(
                    "Der Heimweg-Timer hilft dir, sicher nach Hause zu kommen.\n\n" +
                    "1. Stelle die gewünschte Zeit (in Minuten) ein\n" +
                    "2. Starte den Timer, wenn du losgehst\n" +
                    "3. Wenn der Timer abläuft, wirst du per Benachrichtigung erinnert\n" +
                    "4. Deaktiviere den Timer, wenn du angekommen bist\n\n" +
                    "Wenn du den Timer nicht deaktivierst, wird nach 2 Minuten " +
                    "automatisch eine Erinnerung gesendet. Nach weiteren 2 Minuten werden " +
                    "deine Notfallkontakte per SMS benachrichtigt."
                )
            },
            confirmButton = {
                TextButton(onClick = { showInfoDialog = false }) {
                    Text("OK")
                }
            }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.AccessTime,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Heimweg-Timer",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = { showInfoDialog = true },
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Info",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            when (timerState.phase) {
                TimerPhase.IDLE -> {
                    var editText by remember { mutableStateOf(timerDurationInput.toString()) }

                    LaunchedEffect(timerDurationInput) {
                        if (editText.toIntOrNull() != timerDurationInput) {
                            editText = timerDurationInput.toString()
                        }
                    }

                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {
                            if (timerDurationInput > 1) onSetDuration(timerDurationInput - 1)
                        }) {
                            Text("-", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        }
                        OutlinedTextField(
                            value = editText,
                            onValueChange = {
                                if (it.isEmpty() || (it.all { c -> c.isDigit() } && it.length <= 3)) {
                                    editText = it
                                    it.toIntOrNull()?.let { n -> onSetDuration(n.coerceIn(1, 999)) }
                                }
                            },
                            modifier = Modifier.width(100.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            suffix = { Text("min") },
                            textStyle = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        IconButton(onClick = {
                            onSetDuration(timerDurationInput + 1)
                        }) {
                            Text("+", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    if (!hasContacts) {
                        Text(
                            text = "Bitte hinterlege Notfallkontakte im Kontakte-Tab, um den Timer zu nutzen.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }
                    Button(
                        onClick = onStart,
                        enabled = timerDurationInput > 0 && hasContacts,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Timer starten")
                    }
                }

                TimerPhase.COUNTDOWN -> {
                    Text(
                        text = formatTime(remainingSeconds),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Timer läuft...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onDeactivate,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = RedDark
                        ),
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Timer stoppen")
                    }
                }

                TimerPhase.EXPIRED -> {
                    var emergencyCountdown by remember { mutableStateOf(0) }

                    LaunchedEffect(timerEndTime) {
                        while (true) {
                            val remaining = ((timerEndTime + 240_000L - System.currentTimeMillis()) / 1000).toInt().coerceAtLeast(0)
                            emergencyCountdown = remaining
                            if (remaining <= 0) break
                            delay(1000)
                        }
                    }

                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = RedBright,
                        modifier = Modifier.size(48.dp).align(Alignment.CenterHorizontally)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Timer abgelaufen!",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = RedDark,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                    Text(
                        text = if (emergencyCountdown > 0) "Notfall-SMS in ${emergencyCountdown}s" else "Notfall-SMS wird gesendet...",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = RedBright,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = onDeactivate,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RedBright
                        ),
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Angekommen")
                    }
                }

                TimerPhase.REMINDER -> {
                    var emergencyCountdown by remember { mutableStateOf(0) }

                    LaunchedEffect(timerEndTime) {
                        while (true) {
                            val remaining = ((timerEndTime + 240_000L - System.currentTimeMillis()) / 1000).toInt().coerceAtLeast(0)
                            emergencyCountdown = remaining
                            if (remaining <= 0) break
                            delay(1000)
                        }
                    }

                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = RedBright,
                        modifier = Modifier.size(48.dp).align(Alignment.CenterHorizontally)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Erinnerung",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = RedDark,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                    Text(
                        text = if (emergencyCountdown > 0) "Notfall-SMS in ${emergencyCountdown}s" else "Notfall-SMS wird gesendet...",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = RedBright,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = onDeactivate,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RedBright
                        ),
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Angekommen")
                    }
                }

                TimerPhase.EMERGENCY -> {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = RedDark,
                        modifier = Modifier.size(48.dp).align(Alignment.CenterHorizontally)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Notfall-SMS gesendet",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = RedDark,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                    Text(
                        text = "Deine Notfallkontakte wurden informiert.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = onDeactivate,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RedBright
                        ),
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Timer deaktivieren")
                    }
                }
            }
        }
    }
}
