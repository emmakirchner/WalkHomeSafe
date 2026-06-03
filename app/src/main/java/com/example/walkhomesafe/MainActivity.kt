package com.example.walkhomesafe

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.walkhomesafe.presentation.AppRoot
import com.example.walkhomesafe.ui.theme.WalkHomeSafeTheme
import com.google.android.libraries.places.api.Places

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Places.initializeWithNewPlacesApiEnabled(applicationContext, getString(R.string.google_maps_key))
        enableEdgeToEdge()

        setContent {
            WalkHomeSafeTheme {
                AppRoot()
            }
        }
    }
}
