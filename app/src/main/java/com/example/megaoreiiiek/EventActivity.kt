package com.example.megaoreiiiek

import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProviders
import com.example.megaoreiiiek.api.EventFullModel
import com.example.megaoreiiiek.api.EventPostModel
import com.example.megaoreiiiek.api.PatternPostModel
import kotlinx.android.synthetic.main.activity_event.*
import kotlinx.android.synthetic.main.content_event.*
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalDateTime
import org.threeten.bp.LocalTime
import org.threeten.bp.format.DateTimeFormatter
import java.util.*


class EventActivity : AppCompatActivity() {

    private var patternId: Long? = null
    private var eventId: Long? = null
    private lateinit var viewModel: CalendarViewModel
    var rule: String? = null
    val dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH)
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_event)

        setSupportActionBar(toolbar)

        val selectedDayNow = timestampToDateTime(intent.getLongExtra("selectedDayNow", -1))

        var startedAt = selectedDayNow.plusHours(10)
        var endedAt = selectedDayNow.plusHours(12)


//        var startedAt = LocalDateTime.now().plusHours(10)
//        var endedAt = LocalDateTime.now().plusHours(12)

        viewModel = ViewModelProviders.of(this).get(CalendarViewModel::class.java)

        if (intent.action == "EDIT") {
            eventId = intent.getLongExtra("event_id", -1)
            patternId = intent.getLongExtra("pattern_id", -1)
            val name = intent.getStringExtra("name")
            val details = intent.getStringExtra("details")
            val location = intent.getStringExtra("location")
            val status = intent.getStringExtra("status")
            rule = intent.getStringExtra("rrule")
            val started_at = intent.getLongExtra("started_at", -1)
            val ended_at = intent.getLongExtra("ended_at", -1)

            val instance_started_at = intent.getLongExtra("instance_started_at", 0)
            val instance_ended_at = intent.getLongExtra("instance_ended_at", 0)

            editTextName.setText(name)
            editTextDescription.setText(details)
            editTextAdress.setText(location)
            editTextStatus.setText(status)

            textViewInstanceStartDate.visibility = View.VISIBLE
            textViewInstanceEndDate.visibility = View.VISIBLE
            textViewStartDate3.visibility = View.VISIBLE
            textViewStartDate4.visibility = View.VISIBLE
            textViewInstanceStartDate.text = dateFormatter.format(timestampToDateTime(instance_started_at))
            textViewInstanceEndDate.text = dateFormatter.format(timestampToDateTime(instance_ended_at))

            startedAt = timestampToDateTime(started_at)
            endedAt = timestampToDateTime(ended_at)
        }
        if (!rule.isNullOrBlank() && rule != "FREQ=DAILY;INTERVAL=1;COUNT=1") {
            switchRepeatEvent.isChecked = true
            textViewChangeRepeatRule.isVisible = true
        } else textViewChangeRepeatRule.isVisible = false

        editTextStartDate.text = dateFormatter.format(startedAt)
        editTextEndDate.setText(dateFormatter.format(endedAt))
        editTextStartTime.setText(timeFormatter.format(startedAt))
        editTextEndTime.setText(timeFormatter.format(endedAt))

        switchRepeatEvent.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                val intent = Intent(this@EventActivity, RepeatActivity::class.java).apply {
                    putExtra("rule", rule)
                    putExtra("day", startedAt.dayOfMonth)
                    putExtra("month", startedAt.monthValue)
                }
                startActivityForResult(intent, 1)
            } else {
                rule = null
                textViewChangeRepeatRule.isVisible = false
            }
        }

        editTextStartTime.setOnClickListener {
            val now = LocalDateTime.now()
            TimePickerDialog(this, TimePickerDialog.OnTimeSetListener { _, hourOfDay, minute ->
                startedAt = startedAt.with(LocalTime.of(hourOfDay, minute))
                if (startedAt > endedAt) {
                    endedAt = startedAt
                    editTextEndTime.setText(timeFormatter.format(startedAt))
                }
                editTextStartTime.setText(timeFormatter.format(startedAt))
            }, now.hour, now.minute, true).show()
        }

        editTextEndTime.setOnClickListener {
            val now = LocalDateTime.now()
            TimePickerDialog(this, TimePickerDialog.OnTimeSetListener { _, hourOfDay, minute ->
                endedAt = endedAt.with(LocalTime.of(hourOfDay, minute))
                if (startedAt > endedAt) {
                    endedAt = startedAt
                    editTextStartTime.setText(timeFormatter.format(endedAt))
                }
                editTextEndTime.setText(timeFormatter.format(endedAt))
            }, now.hour, now.minute, true).show()
        }

        editTextStartDate.setOnClickListener {
            val now = LocalDateTime.now()
            DatePickerDialog(this, DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
                val oldDay = startedAt.dayOfMonth
                val oldMonth = startedAt.monthValue
                startedAt = startedAt.with(LocalDate.of(year, month + 1, dayOfMonth))
                if (startedAt > endedAt) {
                    endedAt = startedAt
                    editTextEndDate.setText(dateFormatter.format(startedAt))
                }
                editTextStartDate.setText(dateFormatter.format(startedAt))

                rule = rule?.replace("BYMONTHDAY=$oldDay", "BYMONTHDAY=${startedAt.dayOfMonth}")
                rule = rule?.replace("BYMONTH=$oldMonth", "BYMONTH=${startedAt.monthValue}")

            }, now.year, now.monthValue - 1, now.dayOfMonth).show()
        }



        editTextEndDate.setOnClickListener {
            val now = LocalDateTime.now()
            DatePickerDialog(this, DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
                endedAt = endedAt.with(LocalDate.of(year, month + 1, dayOfMonth))
                if (startedAt > endedAt) {
                    startedAt = endedAt
                    editTextStartDate.setText(dateFormatter.format(endedAt))
                }
                editTextEndDate.setText(dateFormatter.format(endedAt))
            }, now.year, now.monthValue - 1, now.dayOfMonth).show()
        }


        textViewChangeRepeatRule.setOnClickListener {
                val intent = Intent(this@EventActivity, RepeatActivity::class.java).apply {
                    putExtra("rule", rule)
                    putExtra("day", startedAt.dayOfMonth)
                    putExtra("month", startedAt.monthValue)
                }
                startActivityForResult(intent, 1)
        }


        buttonAdd.setOnClickListener {
            val replyIntent = Intent()
            val name = editTextName.text.toString()
            val details = editTextDescription.text.toString()
            val location = editTextAdress.text.toString()
            val status = editTextStatus.text.toString()

            val started_at = dateTimeToTimestamp(startedAt)
            val ended_at = dateTimeToTimestamp(endedAt)


            if (name.isBlank()) {
                displayAlert("Fill event name, please")
                return@setOnClickListener
            }

            if (intent.action == "CREATE") {

                viewModel.createEvent(
                    EventPostModel(details, location, name, status),
                    PatternPostModel(
                        ended_at - started_at,
                        dateTimeToTimestamp(LocalDateTime.now().plusYears(10)),
                        started_at,
                        rule ?: "FREQ=DAILY;INTERVAL=1;COUNT=1",
                        "UTC"
                    )
                )
            } else if (intent.action == "EDIT" && eventId !== null && patternId !== null) {
                val pId = patternId
                val eId = eventId
                if (eId !== null && pId !== null) {
                    viewModel.updateEvent(
                        EventFullModel(details, eId, location, name, "", status),
                        PatternPostModel(
                            ended_at - started_at,
                            dateTimeToTimestamp(LocalDateTime.now().plusYears(10)),
                            started_at,
                            rule ?: "FREQ=DAILY;INTERVAL=1;COUNT=1",
                            "UTC"
                        ),
                        pId
                    )
                }
            }

            setResult(Activity.RESULT_OK, replyIntent)
            finish()
        }

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onBackPressed() {
        AlertDialog.Builder(this@EventActivity)
            .setTitle("Exite without saving?")
            .setPositiveButton("Yes") { _, _ ->
                super.onBackPressed()
            }
            .setNegativeButton("No") { _, _ -> }
            .create()
            .show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        if (intent.action === "EDIT") menuInflater.inflate(R.menu.event, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_delete -> {
                AlertDialog.Builder(this@EventActivity)
                    .setTitle("Are you sure?")
                    .setPositiveButton("Yes") { _, _ ->
                        eventId?.let { viewModel.deleteEvent(it) }
                        val replyIntent = Intent()
                        setResult(Activity.RESULT_OK, replyIntent)
                        finish()
                    }
                    .setNegativeButton("No") { _, _ -> }
                    .create()
                    .show()

                true
            }
            android.R.id.home -> {
                AlertDialog.Builder(this@EventActivity)
                    .setTitle("Exit without saving?")
                    .setPositiveButton("Yes") { _, _ ->
                        super.onBackPressed()
                    }
                    .setNegativeButton("No") { _, _ -> }
                    .create()
                    .show()

                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun displayAlert(msg: String) {
        AlertDialog.Builder(this@EventActivity)
            .setTitle(msg)
            .setPositiveButton("OK") { _, _ -> }
            .create()
            .show()
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        val newRule = data?.getStringExtra("rule")

        switchRepeatEvent.isChecked = (resultCode != Activity.RESULT_CANCELED ) || (rule != null && rule != "FREQ=DAILY;INTERVAL=1;COUNT=1")
        textViewChangeRepeatRule.isVisible = (resultCode != Activity.RESULT_CANCELED) || (rule != null && rule != "FREQ=DAILY;INTERVAL=1;COUNT=1")

        if (resultCode == Activity.RESULT_OK) {
            rule = newRule
        }
    }

}
