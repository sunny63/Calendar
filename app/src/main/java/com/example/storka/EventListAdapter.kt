package com.example.storka

import android.widget.AdapterView
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.storka.api.EventFullModel
import kotlinx.android.synthetic.main.event_recycler_view_item.view.*
import org.threeten.bp.LocalDate

class EventListAdapter internal constructor(
    context: Context, private val listener: OnRecyclerItemClickListener
) : RecyclerView.Adapter<EventListAdapter.EventViewHolder>() {

    private val inflater: LayoutInflater = LayoutInflater.from(context)
    private var events = emptyList<EventFullModel>() // Cached copy of words

    inner class EventViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val view = itemView
        val eventItemView: TextView = itemView.textEventName
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val itemView = inflater.inflate(R.layout.event_recycler_view_item, parent, false)
        return EventViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        val current = events[position]

        holder.view.setOnClickListener {
            listener.onItemClick(current.id)
        }

        holder.eventItemView.text = current.name

//        var eventInformation = "Name: "
//        val eventName = current.name
//        val eventLocation = current.location
//        if (!eventLocation.isNullOrBlank()) {
//            eventInformation = "$eventInformation$eventName,   Location: $eventLocation"
//        } else eventInformation = "$eventInformation$eventName"
//
//        holder.eventItemView.text = eventInformation


    }

    internal fun setEvents(events: List<EventFullModel>) {
        this.events = events
        notifyDataSetChanged()
    }

    override fun getItemCount() = events.size
}