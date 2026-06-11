package org.hogel.yobidashi

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                MainScreen(
                    onStart = { startListener() },
                    onStop = { stopService(Intent(this, ListenerService::class.java)) },
                )
            }
        }
    }

    private fun startListener() {
        startForegroundService(Intent(this, ListenerService::class.java))
    }
}

@Composable
private fun MainScreen(onStart: () -> Unit, onStop: () -> Unit) {
    val running by ListenerService.running.collectAsStateWithLifecycle()
    val events by EventLog.events.collectAsStateWithLifecycle()
    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { onStart() }

    Scaffold { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (running) "Listening" else "Stopped",
                    style = MaterialTheme.typography.headlineSmall,
                )
                val context = LocalContext.current
                Button(onClick = {
                    if (running) {
                        onStop()
                    } else if (
                        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                        == PackageManager.PERMISSION_GRANTED
                    ) {
                        onStart()
                    } else {
                        notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }) {
                    Text(if (running) "Stop" else "Start")
                }
            }
            SettingsSection()
            HorizontalDivider()
            EventList(events)
        }
    }
}

@Composable
private fun SettingsSection() {
    val context = LocalContext.current
    val saved = remember { Settings.load(context) }
    var serverUrl by remember { mutableStateOf(saved.serverUrl) }
    var accessToken by remember { mutableStateOf(saved.accessToken) }

    OutlinedTextField(
        value = serverUrl,
        onValueChange = { serverUrl = it },
        label = { Text("ntfy server URL (for protected attachments)") },
        placeholder = { Text("https://ntfy.example.com") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = accessToken,
        onValueChange = { accessToken = it },
        label = { Text("Access token") },
        placeholder = { Text("tk_...") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedButton(onClick = {
        Settings.save(context, Settings(serverUrl, accessToken))
        EventLog.add("settings saved")
    }) {
        Text("Save")
    }
}

@Composable
private fun EventList(events: List<EventLog.Event>) {
    val timeFormat = SimpleDateFormat("MM-dd HH:mm:ss", Locale.US)
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(events) { event ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = timeFormat.format(Date(event.time)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                )
                Text(text = event.text, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
