package com.example.walkhomesafe.presentation.screens

import android.content.Context
import android.location.LocationManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.walkhomesafe.presentation.components.EmergencyActionButton
import com.example.walkhomesafe.presentation.widget.WidgetSosAction
import com.example.walkhomesafe.presentation.widget.WidgetTrigger
import com.example.walkhomesafe.ui.theme.FeedbackBanner
import com.example.walkhomesafe.viewmodel.ContactsViewModel
import com.example.walkhomesafe.viewmodel.HomeViewModel
import com.example.walkhomesafe.viewmodel.MapViewModel
import com.example.walkhomesafe.viewmodel.MessageViewModel
import com.example.walkhomesafe.viewmodel.PermissionsViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val FEEDBACK_TEMPLATE = "SMS an %d Kontakt(e) gesendet"

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

    fun triggerSmsAction() {
        permissionsViewModel.requestSendSms { sendSmsWithLocation() }
    }

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
        WidgetTrigger.actions.collect { action ->
            when (action) {
                WidgetSosAction.SMS -> triggerSmsAction()
                WidgetSosAction.ALARM -> triggerAlarmAction()
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
                onShortPress = { triggerSmsAction() },
                onLongPressRelease = { triggerAlarmAction() },
                onCancel = homeViewModel::stopAlarmService
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
