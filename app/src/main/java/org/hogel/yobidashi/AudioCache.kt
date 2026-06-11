package org.hogel.yobidashi

import android.content.Context
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

object AudioCache {
    private const val MAX_FILES = 50

    fun get(context: Context, url: String): File? =
        fileFor(context, url).takeIf { it.exists() }?.also {
            it.setLastModified(System.currentTimeMillis())
        }

    // Returns the cached file, or null after logging the failure.
    fun download(context: Context, url: String, headers: Map<String, String>): File? {
        val conn = URL(url).openConnection() as HttpURLConnection
        return try {
            // A redirect here means failed auth (e.g. an access login page);
            // never cache it as audio.
            conn.instanceFollowRedirects = false
            headers.forEach { (name, value) -> conn.setRequestProperty(name, value) }
            if (conn.responseCode != HttpURLConnection.HTTP_OK) {
                EventLog.add("download failed (${conn.responseCode})")
                return null
            }
            val file = fileFor(context, url)
            val tmp = File(file.path + ".tmp")
            conn.inputStream.use { input -> tmp.outputStream().use { input.copyTo(it) } }
            tmp.renameTo(file)
            evict(context)
            file
        } catch (e: Exception) {
            EventLog.add("download failed: ${e.message}")
            null
        } finally {
            conn.disconnect()
        }
    }

    private fun fileFor(context: Context, url: String): File =
        File(dir(context), sha256(url))

    private fun dir(context: Context): File =
        File(context.cacheDir, "audio").apply { mkdirs() }

    private fun evict(context: Context) {
        dir(context).listFiles()
            ?.sortedByDescending { it.lastModified() }
            ?.drop(MAX_FILES)
            ?.forEach { it.delete() }
    }

    private fun sha256(text: String): String =
        MessageDigest.getInstance("SHA-256").digest(text.toByteArray())
            .joinToString("") { "%02x".format(it) }
}
