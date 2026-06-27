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

/** Main entry point for the WalkHomeSafe app. Initializes Places API and handles widget intents. */
class MainActivity : ComponentActivity() {
    /**
     * Called when the activity is created. Initializes the Places API,
     * processes any incoming widget intent, and sets the composable content.
     *
     * @param savedInstanceState The saved instance state bundle, or null
     */
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

    /**
     * Called when the activity receives a new intent (e.g., from the home screen widget).
     * Forwards to handleWidgetIntent.
     *
     * @param intent The new intent containing the widget action
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleWidgetIntent(intent)
    }

    /**
     * Processes an incoming intent for SOS widget actions
     * (send SMS or trigger alarm) and forwards them to WidgetTrigger.
     *
     * @param intent The incoming intent, or null
     */
    private fun handleWidgetIntent(intent: Intent?) {
        when (intent?.getStringExtra(EXTRA_SOS_ACTION)) {
            ACTION_SMS -> WidgetTrigger.trigger(WidgetSosAction.SMS)
            ACTION_ALARM -> WidgetTrigger.trigger(WidgetSosAction.ALARM)
        }
    }
}
