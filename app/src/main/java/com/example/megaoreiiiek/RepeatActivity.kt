package com.example.megaoreiiiek

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Switch
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_repeat.*
import kotlinx.android.synthetic.main.content_repeat.*
import org.dmfs.rfc5545.recur.RecurrenceRule

class RepeatActivity : AppCompatActivity(), AdapterView.OnItemSelectedListener {

    private lateinit var switchWeekDaysList: List<Switch>
    private val days = listOf("MO", "TU", "WE", "TH", "FR", "SA", "SU")
    private val selectedDays = mutableSetOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_repeat)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        val rule = intent.getStringExtra("rule")

        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter.createFromResource(
            this,
            R.array.repeat_mode_array,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            // Specify the layout to use when the list of choices appears
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            // Apply the adapter to the spinner
            spinnerRepeatMode.adapter = adapter
        }
        spinnerRepeatMode.onItemSelectedListener = this

        switchWeekDaysList = listOf(switchMon, switchTue, switchWen, switchThu, switchFri, switchSat, switchSun)
        for ((index, item) in switchWeekDaysList.withIndex()) {
            item.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    selectedDays.add(days[index])
                } else {
                    selectedDays.remove(days[index])
                }

                if (selectedDays.isEmpty()) {
                    selectedDays.add(days[index])
                    item.isChecked = true
                }
            }
        }

        switchSun.isChecked = true
        radioGroup.check(R.id.radioBtnNever)



        if (rule != "FREQ=DAILY;INTERVAL=1;COUNT=1" && rule != null) {
            val rrule = RecurrenceRule(rule)

            val ruleCount = rrule.count
            val ruleFreq = rrule.freq
            val ruleInterval = rrule.interval
            val isInfinite = rrule.isInfinite

            when (ruleFreq) {
                RecurrenceRule.Freq.DAILY -> spinnerRepeatMode.setSelection(0)
                RecurrenceRule.Freq.WEEKLY -> spinnerRepeatMode.setSelection(1)
                RecurrenceRule.Freq.MONTHLY -> spinnerRepeatMode.setSelection(2)
                RecurrenceRule.Freq.YEARLY -> spinnerRepeatMode.setSelection(3)
            }

            if (ruleFreq.name == "WEEKLY") {
                val weekDays =
                    rule.split(";").find { it.startsWith("BYDAY=") }?.replace("BYDAY=", "")?.split(",") ?: emptyList()

                for ((i, switch) in switchWeekDaysList.withIndex()) {
                    switch.isChecked = weekDays.contains(days[i])
                }
            }

            if (isInfinite) {
                isAlwaysRepeats(true, 1)
            } else {
                isAlwaysRepeats(false, ruleCount)
            }

            editTextRepeatEvery.setText("$ruleInterval")
        }

        radioBtnNever.setOnClickListener {
            isAlwaysRepeats(true, 1)
        }

        radioBtnAfter.setOnClickListener {
            isAlwaysRepeats(false, 0)
        }


        val day = intent.getIntExtra("day", 1)
        val month = intent.getIntExtra("month", 1)

        buttonSaveRepeat.setOnClickListener {

            val interval = editTextRepeatEvery.text.toString()
            if (interval.isBlank() || interval == "0") {
                displayAlert("Fill interval, please")
                return@setOnClickListener
            }

            val count = editTextRepeatEndAfter.text.toString()
            if (count.isEmpty()) {
                displayAlert("Fill count occurrence, please")
                return@setOnClickListener
            }
            var rule = "FREQ"
            when (spinnerRepeatMode.selectedItemPosition) {
                0 -> {
                    rule = "$rule=DAILY;INTERVAL=$interval"
                }
                1 -> {
                    rule = "$rule=WEEKLY;BYDAY=${selectedDays.joinToString(",")};INTERVAL=$interval"
                }
                2 -> {
                    rule = "$rule=MONTHLY;BYMONTHDAY=$day;INTERVAL=$interval"
                }
                3 -> {
                    rule = "$rule=YEARLY;BYMONTH=$month;BYMONTHDAY=$day"
                }
            }

            when (radioGroup.checkedRadioButtonId) {
                R.id.radioBtnAfter -> {
                    rule = "$rule;COUNT=$count"
                }
            }

            val replyIntent = Intent()
            replyIntent.putExtra("rule", rule)
            setResult(Activity.RESULT_OK, replyIntent)
            finish()
        }
    }

    private fun displayAlert(msg: String) {
        AlertDialog.Builder(this@RepeatActivity)
            .setTitle(msg)
            .setPositiveButton("OK") { _, _ -> }
            .create()
            .show()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                AlertDialog.Builder(this@RepeatActivity)
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

//    private fun ruleParser(rule: String):Map {
//        val regex
//
//        var ruleList = rule.split(";")
//    }

    override fun onItemSelected(parent: AdapterView<*>, view: View, pos: Int, id: Long) {
        when (pos) {
            1 -> {
                for (item in switchWeekDaysList) item.visibility = View.VISIBLE
                textRepeatsOn.visibility = View.VISIBLE
            }
            else -> {
                for (item in switchWeekDaysList) item.visibility = View.INVISIBLE
                textRepeatsOn.visibility = View.INVISIBLE
            }
        }
        when (pos) {
            3 -> {
                radioGroup.visibility = View.INVISIBLE
                editTextRepeatEndAfter.visibility = View.INVISIBLE
                textEnd2.visibility = View.INVISIBLE
                textEnd.visibility = View.INVISIBLE
            }
            else -> {
                radioGroup.visibility = View.VISIBLE
                editTextRepeatEndAfter.visibility = View.VISIBLE
                textEnd2.visibility = View.VISIBLE
                textEnd.visibility = View.VISIBLE
            }
        }
    }


    private fun isAlwaysRepeats(isAlwaysRepeat: Boolean, ruleCount: Int) {
        if (isAlwaysRepeat) {
            radioGroup.check(R.id.radioBtnNever)
            editTextRepeatEndAfter.isEnabled = false
            editTextRepeatEndAfter.setTextIsSelectable(false)
            textEnd2.setTextColor(Color.rgb(139, 139, 139))
            radioBtnAfter.setTextColor(Color.rgb(139, 139, 139))
            radioBtnNever.setTextColor(Color.rgb(0, 0, 0))

        } else {
            if (ruleCount != 0) editTextRepeatEndAfter.setText("$ruleCount")
            radioGroup.check(R.id.radioBtnAfter)
            editTextRepeatEndAfter.isEnabled = true
            editTextRepeatEndAfter.setTextIsSelectable(true)
            textEnd2.setTextColor(Color.rgb(0, 0, 0))
            radioBtnAfter.setTextColor(Color.rgb(0, 0, 0))
            radioBtnNever.setTextColor(Color.rgb(139, 139, 139))
        }
    }


    override fun onBackPressed() {
        AlertDialog.Builder(this@RepeatActivity)
            .setTitle("Exit without saving?")
            .setPositiveButton("Yes") { _, _ ->
                super.onBackPressed()
            }
            .setNegativeButton("No") { _, _ -> }
            .create()
            .show()
    }

    override fun onNothingSelected(parent: AdapterView<*>) {
        // Another interface callback
    }

}
