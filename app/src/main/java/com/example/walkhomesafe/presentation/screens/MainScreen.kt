package com.example.walkhomesafe.presentation.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.walkhomesafe.model.*
import com.example.walkhomesafe.presentation.components.navigation.BottomTab
import com.example.walkhomesafe.presentation.components.navigation.NavigationBar
import com.example.walkhomesafe.presentation.components.navigation.TopBar

@Composable
fun MainScreen(
    onLogout: () -> Unit = {}
)
{
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
                BottomTab.CONTACTS -> ContactsScreen()
                BottomTab.MAP -> MapScreen()
                BottomTab.SETTINGS -> SettingsScreen(onLogout = onLogout)
            }
        }
    }
}



