package com.example.megaoreiiiek

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.megaoreiiiek.api.EventInstanceModel
import com.example.megaoreiiiek.api.ExtendedEventInstance
import com.example.megaoreiiiek.decorator.EventDecorator
import com.firebase.ui.auth.AuthUI
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.CalendarMode
import com.prolificinteractive.materialcalendarview.MaterialCalendarView
import com.prolificinteractive.materialcalendarview.OnDateSelectedListener
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalDateTime
import org.threeten.bp.ZoneOffset


class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener, OnDateSelectedListener {

    private val daysWithEvents = mutableMapOf<CalendarDay, MutableList<Long>>()
    private lateinit var eventAdapter: EventListAdapter
    private lateinit var viewModel: CalendarViewModel
    private var selectedDay: CalendarDay = CalendarDay.today()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        viewModel = ViewModelProviders.of(this).get(CalendarViewModel::class.java)

        viewModel.eventInstances.observe(this, Observer {
            daysWithEvents.clear()
            for (instanceModel in it) {
                var start = timestampToCalendarDay(instanceModel.started_at).date
                val end = timestampToCalendarDay(instanceModel.ended_at).date
                val addToList = { date: LocalDate ->
                    val calDay = CalendarDay.from(date)
                    if (daysWithEvents[calDay] === null)
                        daysWithEvents[calDay] = mutableListOf()
                    daysWithEvents[calDay]?.add(instanceModel.event_id)
                }
                if (start == end) {
                    addToList(start)
                } else while (start <= end) {
                    addToList(start)
                    start = start.plusDays(1)
                }
            }
            calendarView.removeDecorators()
            calendarView.addDecorator(EventDecorator(Color.BLACK, daysWithEvents.keys))
            updateAdapterForDate(selectedDay)
        })

        eventAdapter = EventListAdapter(this, object : OnRecyclerItemClickListener {
            override fun onItemClick(instance: EventInstanceModel) {
                val intent = Intent(this@MainActivity, EventActivity::class.java).apply {
                    action = "EDIT"
                    val id = instance.event_id
                    putExtra("event_id", id)
                    val event = viewModel.events.value?.find { it.id == id }
                    val pattern = viewModel.patterns.value?.find { it.event_id == id }
                    putExtra("pattern_id", pattern?.id)
                    putExtra("name", event?.name)
                    putExtra("details", event?.details)
                    putExtra("location", event?.location)
                    putExtra("status", event?.status)
                    putExtra("started_at", pattern?.started_at)
                    putExtra("ended_at", (pattern?.started_at ?: 0) + (pattern?.duration ?: 0))
                    putExtra("rrule", pattern?.rrule)

                    putExtra("instance_started_at", instance.started_at)
                    putExtra("instance_ended_at", instance.ended_at)
                }
                startActivityForResult(intent, 2)
            }
        })
        eventRecyclerView.adapter = eventAdapter
        eventRecyclerView.layoutManager = LinearLayoutManager(this)

        val fab: FloatingActionButton = findViewById(R.id.fab)
        fab.setOnClickListener {
            val selectedDayNow = dateTimeToTimestamp(selectedDay.date.atStartOfDay())
            val intent = Intent(this@MainActivity, EventActivity::class.java).apply {
                action = "CREATE"
                putExtra("selectedDayNow", selectedDayNow)
            }
            startActivityForResult(intent, 1)
        }
        val drawerLayout: DrawerLayout = findViewById(R.id.drawerLayout)
        val navView: NavigationView = findViewById(R.id.navView)
        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        navView.setNavigationItemSelectedListener(this)

        calendarView.setOnMonthChangedListener { _, date ->
            sync(date.date.minusDays(41).atStartOfDay(), date.date.plusDays(72).atStartOfDay())
        }
        calendarView.setOnDateChangedListener(this)
        calendarView.selectedDate = selectedDay

        if (FirebaseAuth.getInstance().currentUser === null) {
            isSingIn(false)
            signIn()
        } else {
            isSingIn(true)
            sync(LocalDateTime.now().minusDays(41), LocalDateTime.now().plusDays(72))
        }
    }

    private fun sync(from: LocalDateTime, to: LocalDateTime) {
        viewModel.syncEvents(
            dateTimeToTimestamp(from),
            dateTimeToTimestamp(to)
        )
    }

    override fun onDateSelected(widget: MaterialCalendarView, date: CalendarDay, selected: Boolean) {
        selectedDay = date
        updateAdapterForDate(date)
    }

    private fun updateAdapterForDate(date: CalendarDay) {
        val events = viewModel.events.value?.associateBy { it.id } ?: emptyMap()
        val eventsForToday = if (daysWithEvents.keys.contains(date)) {
            viewModel.eventInstances.value?.filter {
                val start = timestampToDateTime(it.started_at).toLocalDate()
                val end = timestampToDateTime(it.ended_at).toLocalDate()
                date.date in start..end
            }?.map { ExtendedEventInstance(events[it.event_id]?.name ?: "", it) }?.sortedBy { it.instance.started_at }
        } else emptyList()
        if (eventsForToday !== null) eventAdapter.setEvents(eventsForToday, selectedDay.date)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK)
            sync(calendarView.currentDate.date.minusDays(41).atStartOfDay(), calendarView.currentDate.date.plusDays(72).atStartOfDay())
    }

    private fun signIn() {
        // Choose authentication providers
        val providers = arrayListOf(
            AuthUI.IdpConfig.EmailBuilder().build(),
            AuthUI.IdpConfig.PhoneBuilder().build()
        )

        // Create and launch sign-in intent
        startActivityForResult(
            AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAvailableProviders(providers)
                .build(),
            RC_SIGN_IN
        )
    }

    override fun onBackPressed() {
        val drawerLayout: DrawerLayout = findViewById(R.id.drawerLayout)
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.
        when (item.itemId) {
            R.id.week_view -> calendarView.state().edit().setCalendarDisplayMode(CalendarMode.WEEKS).commit()
            R.id.month_view -> calendarView.state().edit().setCalendarDisplayMode(CalendarMode.MONTHS).commit()
            R.id.sing_out -> {
                FirebaseAuth.getInstance().signOut()
                isSingIn(false)
                signIn()
            }
            R.id.sing_in -> {
                signIn()
                isSingIn(true)
            }
        }

        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun isSingIn(flag: Boolean){
        navView.menu.apply {
            findItem(R.id.sing_in).isVisible = !flag
            findItem(R.id.sing_out).isVisible = flag
        }
    }

    private fun timestampToCalendarDay(t: Long): CalendarDay {
        return CalendarDay.from(LocalDateTime.ofEpochSecond(t / 1000, 0, ZoneOffset.UTC).toLocalDate())
    }

    private fun dateTimeToTimestamp(c: LocalDateTime): Long {
        return c.toEpochSecond(ZoneOffset.UTC) * 1000
    }

    companion object {
        private const val RC_SIGN_IN = 123
    }
}
