package com.example.walkhomesafe.presentation.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.walkhomesafe.MainActivity
import com.example.walkhomesafe.R
import com.example.walkhomesafe.presentation.widget.ACTION_ALARM
import com.example.walkhomesafe.presentation.widget.ACTION_SMS
import com.example.walkhomesafe.presentation.widget.EXTRA_SOS_ACTION

/**
 * Home screen widget provider for emergency SOS actions.
 * Provides two buttons: emergency SMS and alarm trigger.
 */
class EmergencyActionWidgetProvider : AppWidgetProvider() {

    /**
     * Called when the widget is updated.
     *
     * @param context The application context
     * @param appWidgetManager The widget manager
     * @param appWidgetIds Array of widget IDs to update
     */
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        /**
         * Updates a single widget instance with SOS button intents.
         *
         * @param context The application context
         * @param appWidgetManager The widget manager
         * @param appWidgetId The ID of the widget to update
         */
        internal fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val smsIntent = Intent(context, MainActivity::class.java).apply {
                putExtra(EXTRA_SOS_ACTION, ACTION_SMS)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            val smsPendingIntent = PendingIntent.getActivity(
                context,
                appWidgetId * 10 + 1,
                smsIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val alarmIntent = Intent(context, MainActivity::class.java).apply {
                putExtra(EXTRA_SOS_ACTION, ACTION_ALARM)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            val alarmPendingIntent = PendingIntent.getActivity(
                context,
                appWidgetId * 10 + 2,
                alarmIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val views = RemoteViews(context.packageName, R.layout.emergency_action_widget)
            views.setOnClickPendingIntent(R.id.widget_sms, smsPendingIntent)
            views.setOnClickPendingIntent(R.id.widget_alarm, alarmPendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
