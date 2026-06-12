package org.hogel.yobidashi

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

// Restarts the listener after reboot or app update, but only if the user
// left it running (Stop persists across restarts).
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (Settings.listenerEnabled(context)) {
            context.startForegroundService(Intent(context, ListenerService::class.java))
        }
    }
}
