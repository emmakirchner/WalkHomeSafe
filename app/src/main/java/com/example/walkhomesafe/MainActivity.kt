package com.example.walkhomesafe

import android.Manifest
import android.content.pm.PackageManager
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
import com.example.walkhomesafe.helper.ContactPickerHelper
import com.example.walkhomesafe.model.EmergencyContact
import com.example.walkhomesafe.navigation.*
import com.example.walkhomesafe.screens.*
import com.example.walkhomesafe.ui.theme.WalkHomeSafeTheme


class MainActivity : ComponentActivity() {
    private var contacts by mutableStateOf<List<EmergencyContact>>(emptyList())

    private lateinit var contactPicker: ContactPickerHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        requestContactsPermission()

        contactPicker = ContactPickerHelper(this) { name, number ->
            contacts = contacts + EmergencyContact(
                id = System.currentTimeMillis(),
                name = name,
                phone = number
            )
        }

        setContent {
            WalkHomeSafeTheme {
                MainScreen(
                    contacts = contacts,
                    onAddContact = {
                        contactPicker.launch()
                    },
                    onDeleteContact = { contact ->
                        contacts = contacts.filterNot { it.id == contact.id }
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
}

@Composable
fun MainScreen(
    contacts: List<EmergencyContact>,
    onAddContact: () -> Unit,
    onDeleteContact: (EmergencyContact) -> Unit
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
                BottomTab.HOME -> HomeScreen()
                BottomTab.CONTACTS -> ContactsScreen(
                    contacts = contacts,
                    onDeleteContact = onDeleteContact,
                    onAddContact = onAddContact
                )
                BottomTab.MAP -> MapScreen()
                BottomTab.SETTINGS -> SettingsScreen()
            }
        }
    }
}
