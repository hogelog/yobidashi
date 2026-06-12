package org.hogel.yobidashi

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
                var showSettings by remember { mutableStateOf(false) }
                if (showSettings) {
                    SettingsScreen(onBack = { showSettings = false })
                } else {
                    MainScreen(
                        onStart = { startListener() },
                        onStop = { stopService(Intent(this, ListenerService::class.java)) },
                        onPlay = { url -> playUrl(url) },
                        onOpenSettings = { showSettings = true },
                    )
                }
            }
        }
    }

    private fun startListener() {
        startForegroundService(Intent(this, ListenerService::class.java))
    }

    private fun playUrl(url: String) {
        startForegroundService(
            Intent(this, ListenerService::class.java)
                .setAction(ListenerService.ACTION_PLAY)
                .putExtra(ListenerService.EXTRA_URL, url)
        )
    }
}

@Composable
private fun MainScreen(
    onStart: () -> Unit,
    onStop: () -> Unit,
    onPlay: (String) -> Unit,
    onOpenSettings: () -> Unit,
) {
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
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onOpenSettings) {
                        Text("Settings")
                    }
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
            }
            HorizontalDivider()
            EventList(events, onPlay)
        }
    }
}

@Composable
private fun SettingsScreen(onBack: () -> Unit) {
    BackHandler(onBack = onBack)
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
                    text = "Settings",
                    style = MaterialTheme.typography.headlineSmall,
                )
                TextButton(onClick = onBack) {
                    Text("Back")
                }
            }
            ServerSettingsSection()
            OutputsSection()
        }
    }
}

@Composable
private fun ServerSettingsSection() {
    val context = LocalContext.current
    val saved = remember { Settings.load(context) }
    var serverUrl by remember { mutableStateOf(saved.serverUrl) }
    var headers by remember { mutableStateOf(saved.headers) }

    OutlinedTextField(
        value = serverUrl,
        onValueChange = { serverUrl = it },
        label = { Text("Protected audio server URL") },
        placeholder = { Text("https://audio.example.com") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = headers,
        onValueChange = { headers = it },
        label = { Text("Request headers for that server") },
        placeholder = { Text("Authorization: Basic ...\nCF-Access-Client-Id: ...") },
        minLines = 2,
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedButton(onClick = {
        Settings.save(context, Settings(serverUrl, headers, Settings.load(context).allowedOutputs))
        EventLog.add("settings saved")
    }) {
        Text("Save")
    }
}

@Composable
private fun OutputsSection() {
    val context = LocalContext.current
    var allowed by remember { mutableStateOf(Settings.load(context).allowedOutputs) }
    var connected by remember { mutableStateOf(AudioOutputs.connected(context)) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Auto-play outputs (none checked = always)",
            style = MaterialTheme.typography.titleSmall,
        )
        TextButton(onClick = { connected = AudioOutputs.connected(context) }) {
            Text("Refresh")
        }
    }
    Column {
        (connected + allowed.filter { it !in connected }).forEach { key ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = key in allowed,
                    onCheckedChange = { checked ->
                        allowed = if (checked) allowed + key else allowed - key
                        Settings.saveAllowedOutputs(context, allowed)
                    },
                )
                Text(
                    text = AudioOutputs.label(key) + if (key in connected) "" else " (disconnected)",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun EventList(events: List<EventLog.Event>, onPlay: (String) -> Unit) {
    val timeFormat = SimpleDateFormat("MM-dd HH:mm:ss", Locale.US)
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(events) { event ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = if (event.url != null) {
                    Modifier.fillMaxWidth().clickable { onPlay(event.url) }
                } else {
                    Modifier.fillMaxWidth()
                },
            ) {
                Text(
                    text = timeFormat.format(Date(event.time)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                )
                Text(
                    text = (if (event.url != null) "▶ " else "") + event.text,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}
