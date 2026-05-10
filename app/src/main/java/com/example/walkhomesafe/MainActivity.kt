package com.example.walkhomesafe

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.walkhomesafe.data.*
import com.example.walkhomesafe.helper.ContactHelper
import com.example.walkhomesafe.model.EmergencyContact
import com.example.walkhomesafe.navigation.*
import com.example.walkhomesafe.screens.*
import com.example.walkhomesafe.services.EmergencyAlarmService
import com.example.walkhomesafe.ui.theme.WalkHomeSafeTheme
import kotlinx.coroutines.launch


class MainActivity : ComponentActivity() {
    private var contacts by mutableStateOf<List<EmergencyContact>>(emptyList())
    private var emergencyMessage by mutableStateOf("")

    private lateinit var contactHelper: ContactHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        requestContactsPermission()
        requestSmsPermission()
        requestNotificationPermission()

        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    emergencyContactsFlow(this@MainActivity).collect { storedContacts ->
                        contacts = storedContacts
                    }
                }

                launch {
                    emergencyMessageFlow(this@MainActivity).collect { storedMessage ->
                        emergencyMessage = storedMessage
                    }
                }
            }
        }

        contactHelper = ContactHelper(this,this) { name, number ->
            val newContact = EmergencyContact(
                id = System.currentTimeMillis(),
                name = name,
                phone = number
            )
            contacts = contacts + newContact

            lifecycleScope.launch {
                saveEmergencyContacts(
                    context = this@MainActivity,
                    contacts = contacts
                )
            }
        }

        setContent {
            WalkHomeSafeTheme {
                MainScreen(
                    emergencyMessage = emergencyMessage,
                    contacts = contacts,
                    onAddContact = {
                        contactHelper.launch()
                    },
                    onDeleteContact = { contact ->
                        contacts = contacts.filterNot { it.id == contact.id }

                        lifecycleScope.launch {
                            saveEmergencyContacts(
                                context = this@MainActivity,
                                contacts = contacts
                            )
                        }
                    },
                    onEmergencyMessageChange = { newMessage ->
                        emergencyMessage = newMessage

                        lifecycleScope.launch {
                            saveEmergencyMessage(
                                context = this@MainActivity,
                                message = newMessage
                            )
                        }
                   },
                    onSendMessage = {
                        onSendMessageClicked()
                    },
                    onSendMessageAndAlarm = {
                        onSendMessageClicked()
                        startEmergencyAlarm()
                    },
                    onCancelAlarm = {
                        stopEmergencyAlarm()
                    }
                )
            }
        }
    }

    private fun requestContactsPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_CONTACTS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_CONTACTS),
                100
            )
        }
    }

    private fun requestSmsPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.SEND_SMS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.SEND_SMS),
                200
            )
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    300
                )
            }
        }
    }

    private fun onSendMessageClicked() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.SEND_SMS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            contactHelper.sendEmergencyMessage(
                contacts = contacts,
                message = emergencyMessage
            )
        } else {
            requestSmsPermission()
        }
    }

    private fun startEmergencyAlarm() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            startForegroundService(Intent(this, EmergencyAlarmService::class.java))
        } else {
            requestNotificationPermission()
        }
    }

    private fun stopEmergencyAlarm() {
        stopService(Intent(this, EmergencyAlarmService::class.java))
    }

}

@Composable
fun MainScreen(
    emergencyMessage: String,
    contacts: List<EmergencyContact>,
    onAddContact: () -> Unit,
    onDeleteContact: (EmergencyContact) -> Unit,
    onEmergencyMessageChange: (String) -> Unit,
    onSendMessage: () -> Unit,
    onSendMessageAndAlarm: () -> Unit,
    onCancelAlarm: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(BottomTab.HOME) }

    Scaffold(
        topBar = {
            TopBar()
        },
        bottomBar = {
            NavigationBar(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (selectedTab) {
                BottomTab.HOME -> HomeScreen(
                    onSendMessage = onSendMessage,
                    onSendMessageAndAlarm = onSendMessageAndAlarm,
                    onCancelAlarm = onCancelAlarm
                )
                BottomTab.CONTACTS -> ContactsScreen(
                    emergencyMessage = emergencyMessage,
                    contacts = contacts,
                    onDeleteContact = onDeleteContact,
                    onAddContact = onAddContact,
                    onEmergencyMessageChange = onEmergencyMessageChange
                )
                BottomTab.MAP -> MapScreen()
                BottomTab.SETTINGS -> SettingsScreen()
            }
        }
    }
}
