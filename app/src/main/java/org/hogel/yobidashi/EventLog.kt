package org.hogel.yobidashi

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object EventLog {
    private const val MAX_EVENTS = 100
    private const val FILE_NAME = "event_log.json"

    data class Event(val time: Long, val text: String, val url: String? = null, val message: String? = null)

    private val mutableEvents = MutableStateFlow<List<Event>>(emptyList())
    val events: StateFlow<List<Event>> = mutableEvents

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private val writer = CoroutineScope(Dispatchers.IO.limitedParallelism(1))
    private lateinit var file: File

    fun init(context: Context) {
        file = File(context.filesDir, FILE_NAME)
        mutableEvents.value = load()
    }

    fun add(text: String, url: String? = null, message: String? = null) {
        mutableEvents.value =
            (listOf(Event(System.currentTimeMillis(), text, url, message)) + mutableEvents.value).take(MAX_EVENTS)
        persist(mutableEvents.value)
    }

    // A half-written file from a killed process must not brick the app, so a
    // broken log just starts over empty.
    private fun load(): List<Event> = try {
        val array = JSONArray(file.readText())
        (0 until array.length()).map { i ->
            val obj = array.getJSONObject(i)
            Event(
                time = obj.getLong("time"),
                text = obj.getString("text"),
                url = if (obj.has("url")) obj.getString("url") else null,
                message = if (obj.has("message")) obj.getString("message") else null,
            )
        }
    } catch (e: Exception) {
        emptyList()
    }

    private fun persist(events: List<Event>) {
        writer.launch {
            val array = JSONArray()
            events.forEach { event ->
                array.put(
                    JSONObject()
                        .put("time", event.time)
                        .put("text", event.text)
                        .putOpt("url", event.url)
                        .putOpt("message", event.message)
                )
            }
            file.writeText(array.toString())
        }
    }
}
