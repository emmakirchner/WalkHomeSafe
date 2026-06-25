package com.example.walkhomesafe.presentation.components.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

/**
 * Top app bar displaying the app name "WalkHomeSafe" and subtitle "Dein Sicherheitsbegleiter".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar() {
    Column {
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = "WalkHomeSafe",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "Dein Sicherheitsbegleiter",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )

        HorizontalDivider(
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        )
    }
}
