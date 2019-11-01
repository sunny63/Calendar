package com.example.megaoreiiiek

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.megaoreiiiek.api.ExtendedEventInstance
import kotlinx.android.synthetic.main.event_recycler_view_item.view.*
import org.threeten.bp.LocalDate

class EventListAdapter internal constructor(
    context: Context, private val listener: OnRecyclerItemClickListener
) : RecyclerView.Adapter<EventListAdapter.EventViewHolder>() {

    private var selectedDate: LocalDate = LocalDate.now()
    private val inflater: LayoutInflater = LayoutInflater.from(context)
    private var events = emptyList<ExtendedEventInstance>() // Cached copy of words

    inner class EventViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val view = itemView
        val eventItemView: TextView = itemView.textEventName
        val eventTimeView = itemView.textViewEventTime
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val itemView = inflater.inflate(R.layout.event_recycler_view_item, parent, false)
        return EventViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        val current = events[position]

        holder.view.setOnClickListener {
            listener.onItemClick(current.instance)
        }

        holder.eventItemView.text = current.name

        val start = timestampToDateTime(current.instance.started_at)
        val end = timestampToDateTime(current.instance.ended_at)



        val startDate = start.toLocalDate()
        val endDate = end.toLocalDate()
        if (startDate < selectedDate && endDate > selectedDate) {
            holder.eventTimeView.text = "All day"
        } else if (startDate == selectedDate && endDate == selectedDate && start.hour == 0 && start.minute == 0 &&
            end.hour == 23 && end.minute == 59) {
            holder.eventTimeView.text = "00:00 - 23:59"
        } else if (startDate < selectedDate && endDate == selectedDate) {
            holder.eventTimeView.text = "until ${formatTime(end)}"
        } else if (startDate == selectedDate && endDate > selectedDate) {
            holder.eventTimeView.text = "after ${formatTime(start)}"
        } else {
            holder.eventTimeView.text = "${formatTime(start)} - ${formatTime(end)}"
        }
    }

    internal fun setEvents(events: List<ExtendedEventInstance>, selectedDate: LocalDate) {
        this.events = events
        this.selectedDate = selectedDate
        notifyDataSetChanged()
    }

    override fun getItemCount() = events.size
}