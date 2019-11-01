package com.example.megaoreiiiek

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.megaoreiiiek.api.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.threeten.bp.LocalDateTime
import java.util.*

class CalendarViewModel(application: Application) : AndroidViewModel(application) {

    val events = MutableLiveData<List<EventFullModel>>()
    val patterns = MutableLiveData<List<PatternGetModel>>()
    val eventInstances = MutableLiveData<List<EventInstanceModel>>()

    fun syncEvents(from: Long, to: Long) = viewModelScope.launch(Dispatchers.IO) {
        Api.create()?.let {
            val eventsFromWeb = it.listEvents().execute()
            val eventsBody = eventsFromWeb.body()
            if (eventsFromWeb.code() == 200 && eventsBody !== null) {
                events.postValue(eventsBody.data)
            }

            val patternResponse = it.listPatterns().execute()
            val patternBody = patternResponse.body()
            if (patternResponse.code() == 200 && patternBody !== null) {
                patterns.postValue(patternBody.data)
            }

            val instancesFromWeb = it.getEventInstances(from, to).execute()
            val instancesBody = instancesFromWeb.body()
            if (instancesFromWeb.code() == 200 && instancesBody !== null) {
                eventInstances.postValue(instancesBody.data)
            }
        }
    }

    fun createEvent(event: EventPostModel, pattern: PatternPostModel) = viewModelScope.launch(Dispatchers.IO) {
        Api.create()?.let {
            val eventsFromWeb = it.createEvent(event).execute()
            val body = eventsFromWeb.body()
            if (eventsFromWeb.code() == 200 && body !== null) {
                createPattern(body.data[0].id, pattern)
            }
        }
    }

    private fun createPattern(event_id: Long, pattern: PatternPostModel) = viewModelScope.launch(Dispatchers.IO) {
        Api.create()?.let {
            it.createPattern(event_id, pattern).execute()
        }
    }

    fun deleteEvent(event_id: Long) = viewModelScope.launch(Dispatchers.IO) {
        Api.create()?.let {
            it.deleteEvent(event_id).execute()
        }
    }

    fun updateEvent(event: EventFullModel, pattern: PatternPostModel, patternId: Long) = viewModelScope.launch(Dispatchers.IO) {
        Api.create()?.let {
            val eventsFromWeb = it.updateEvent(event.id, event).execute()
            val body = eventsFromWeb.body()
            if (eventsFromWeb.code() == 200 && body !== null) {
                updatePattern(patternId, pattern)
            }
        }
    }

    private fun updatePattern(id: Long, updates: PatternPostModel) = viewModelScope.launch(Dispatchers.IO) {
        Api.create()?.let {
            it.updatePattern(id, updates).execute()
        }
    }
}