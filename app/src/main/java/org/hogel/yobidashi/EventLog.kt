package org.hogel.yobidashi

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object EventLog {
    private const val MAX_EVENTS = 100

    data class Event(val time: Long, val text: String)

    private val mutableEvents = MutableStateFlow<List<Event>>(emptyList())
    val events: StateFlow<List<Event>> = mutableEvents

    fun add(text: String) {
        mutableEvents.value =
            (listOf(Event(System.currentTimeMillis(), text)) + mutableEvents.value).take(MAX_EVENTS)
    }
}
