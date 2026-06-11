package org.hogel.yobidashi

import android.content.Context

data class Settings(val serverUrl: String, val authorization: String) {
    // Only attach the Authorization header to attachment URLs on the configured
    // server, so credentials never leak to other hosts.
    fun authHeaders(url: String): Map<String, String> =
        if (serverUrl.isNotEmpty() && authorization.isNotEmpty() && url.startsWith(serverUrl)) {
            mapOf("Authorization" to authorization)
        } else {
            emptyMap()
        }

    companion object {
        private const val PREFS = "settings"

        fun load(context: Context): Settings {
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            return Settings(
                serverUrl = prefs.getString("server_url", "") ?: "",
                authorization = prefs.getString("authorization", "") ?: "",
            )
        }

        fun save(context: Context, settings: Settings) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString("server_url", settings.serverUrl.trim().trimEnd('/'))
                .putString("authorization", settings.authorization.trim())
                .apply()
        }
    }
}
