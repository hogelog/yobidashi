package org.hogel.yobidashi

import android.content.Context

data class Settings(
    val serverUrl: String,
    val headers: String,
    val allowedOutputs: Set<String>,
) {
    // Only attach the configured headers to attachment URLs on the configured
    // server, so credentials never leak to other hosts.
    fun headersFor(url: String): Map<String, String> =
        if (serverUrl.isNotEmpty() && url.startsWith(serverUrl)) {
            headers.lines().mapNotNull { line ->
                val name = line.substringBefore(':', "").trim()
                val value = line.substringAfter(':', "").trim()
                if (name.isEmpty() || value.isEmpty()) null else name to value
            }.toMap()
        } else {
            emptyMap()
        }

    companion object {
        private const val PREFS = "settings"

        fun load(context: Context): Settings {
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            return Settings(
                serverUrl = prefs.getString("server_url", "") ?: "",
                headers = prefs.getString("headers", "") ?: "",
                allowedOutputs = prefs.getStringSet("allowed_outputs", emptySet()) ?: emptySet(),
            )
        }

        fun save(context: Context, settings: Settings) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString("server_url", settings.serverUrl.trim().trimEnd('/'))
                .putString("headers", settings.headers.trim())
                .putStringSet("allowed_outputs", settings.allowedOutputs.toSet())
                .apply()
        }

        fun saveAllowedOutputs(context: Context, allowedOutputs: Set<String>) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putStringSet("allowed_outputs", allowedOutputs.toSet())
                .apply()
        }
    }
}
