package com.example.walkhomesafe.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

enum class BottomTab {
    HOME,
    CONTACTS,
    MAP,
    SETTINGS
}

@Composable
fun NavigationBar(
    selectedTab: BottomTab,
    onTabSelected: (BottomTab) -> Unit) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {

        NavigationBarItem(
            selected = selectedTab == BottomTab.HOME,
            onClick = { onTabSelected(BottomTab.HOME) },
            icon = {
                Icon(
                    imageVector = Icons.Filled.Home,
                    contentDescription = "Home"
                )
            },
            label = { Text("Home") }
        )

        NavigationBarItem(
            selected = selectedTab == BottomTab.CONTACTS,
            onClick = { onTabSelected(BottomTab.CONTACTS) },
            icon = {
                Icon(
                    imageVector = Icons.Filled.Contacts,
                    contentDescription = "Kontakte"
                )
            },
            label = { Text("Kontakte") }
        )

        NavigationBarItem(
            selected = selectedTab == BottomTab.MAP,
            onClick = { onTabSelected(BottomTab.MAP) },
            icon = {
                Icon(
                    imageVector = Icons.Filled.Map,
                    contentDescription = "Karte"
                )
            },
            label = { Text("Karte") }
        )

        NavigationBarItem(
            selected = selectedTab == BottomTab.SETTINGS,
            onClick = { onTabSelected(BottomTab.SETTINGS) },
            icon = {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = "Einstellungen"
                )
            },
            label = { Text("Einstellungen") }
        )
    }
}
