package com.example.smartmusicplayer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

class BatteryReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        if (intent.action == Intent.ACTION_BATTERY_LOW) {

            Toast.makeText(
                context,
                "Battery low — music stopped",
                Toast.LENGTH_LONG
            ).show()

            val stopIntent = Intent(context, MusicService::class.java)
            stopIntent.action = "STOP"
            context.startService(stopIntent)
        }
    }
}