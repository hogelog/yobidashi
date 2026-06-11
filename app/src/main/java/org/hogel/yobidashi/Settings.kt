package org.hogel.yobidashi

import android.content.Context

data class Settings(val serverUrl: String, val accessToken: String) {
    // Only attach the token to attachment URLs on the configured server, so it
    // never leaks to other servers the ntfy app is subscribed to.
    fun authHeaders(url: String): Map<String, String> =
        if (serverUrl.isNotEmpty() && accessToken.isNotEmpty() && url.startsWith(serverUrl)) {
            mapOf("Authorization" to "Bearer $accessToken")
        } else {
            emptyMap()
        }

    companion object {
        private const val PREFS = "settings"

        fun load(context: Context): Settings {
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            return Settings(
                serverUrl = prefs.getString("server_url", "") ?: "",
                accessToken = prefs.getString("access_token", "") ?: "",
            )
        }

        fun save(context: Context, settings: Settings) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString("server_url", settings.serverUrl.trim().trimEnd('/'))
                .putString("access_token", settings.accessToken.trim())
                .apply()
        }
    }
}
