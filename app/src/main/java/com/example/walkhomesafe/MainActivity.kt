package com.example.walkhomesafe

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.walkhomesafe.presentation.AppRoot
import com.example.walkhomesafe.ui.theme.WalkHomeSafeTheme
import com.google.android.libraries.places.api.Places
import com.example.walkhomesafe.presentation.widget.ACTION_ALARM
import com.example.walkhomesafe.presentation.widget.ACTION_SMS
import com.example.walkhomesafe.presentation.widget.EXTRA_SOS_ACTION
import com.example.walkhomesafe.presentation.widget.WidgetSosAction
import com.example.walkhomesafe.presentation.widget.WidgetTrigger

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Places.initializeWithNewPlacesApiEnabled(applicationContext, getString(R.string.google_maps_key))
        enableEdgeToEdge()

        handleWidgetIntent(intent)

        setContent {
            WalkHomeSafeTheme {
                AppRoot()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleWidgetIntent(intent)
    }

    private fun handleWidgetIntent(intent: Intent?) {
        when (intent?.getStringExtra(EXTRA_SOS_ACTION)) {
            ACTION_SMS -> WidgetTrigger.trigger(WidgetSosAction.SMS)
            ACTION_ALARM -> WidgetTrigger.trigger(WidgetSosAction.ALARM)
        }
    }
}
