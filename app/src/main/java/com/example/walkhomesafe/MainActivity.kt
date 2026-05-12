package com.example.walkhomesafe

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.walkhomesafe.presentation.AppRoot
import com.example.walkhomesafe.ui.theme.WalkHomeSafeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            WalkHomeSafeTheme {
                AppRoot()
            }
        }
    }
}