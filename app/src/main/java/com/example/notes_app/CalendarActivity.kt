package com.example.notes_app

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.CalendarView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class CalendarActivity : AppCompatActivity() {

    private lateinit var calendarView: CalendarView
    private lateinit var selectedDate: String
    private lateinit var tvStreak: TextView
    private lateinit var tvTotalReadingDays: TextView
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val readingTracker = ReadingTrackerManager.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calendar)

        // Initialize UI Components
        calendarView = findViewById(R.id.calendarView)
        tvStreak = findViewById(R.id.tvCalendarStreak)
        tvTotalReadingDays = findViewById(R.id.tvCalendarTotalDays)
        val btnViewBooks = findViewById<Button>(R.id.btnViewNotes)
        val btnMarkAsRead = findViewById<Button>(R.id.btnCreateNote)
//        val btnReadToday = findViewById<Button>(R.id.btnReadToday)
//        val btnBack = findViewById<Button>(R.id.btnBack)

        // Update streak and stats
        updateReadingStats()

        // Set default selected date to today
        selectedDate = dateFormat.format(Date())

        // Set calendar date change listener
        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val calendar = Calendar.getInstance()
            calendar.set(year, month, dayOfMonth)

            // Get current date without time
            val today = Calendar.getInstance()
            today.set(Calendar.HOUR_OF_DAY, 0)
            today.set(Calendar.MINUTE, 0)
            today.set(Calendar.SECOND, 0)
            today.set(Calendar.MILLISECOND, 0)

            if (calendar.after(today)) {
                Toast.makeText(this, "Cannot select future dates", Toast.LENGTH_SHORT).show()
                // Reset calendar to today
                calendarView.date = today.timeInMillis
                selectedDate = dateFormat.format(today.time)
            } else {
                selectedDate = dateFormat.format(calendar.time)

                // Check if this day was marked as read
                val isReadDay = readingTracker.isDayRead(selectedDate)
                if (isReadDay) {
                    Toast.makeText(this, "You've read on this day!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Selected date: $selectedDate", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Set click listeners for buttons
        btnViewBooks.setOnClickListener {
            val intent = Intent(this, NotesActivity::class.java)
            intent.putExtra("SELECTED_DATE", selectedDate)
            startActivity(intent)
        }

        btnMarkAsRead.setOnClickListener {
            // Double-check that we're not marking a future date
            if (!isDateValid(selectedDate)) {
                Toast.makeText(this, "Cannot mark future dates as read", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Navigate to book selection with the intent to mark this day as read
            val intent = Intent(this, NotesActivity::class.java)
            intent.putExtra("SELECTED_DATE", selectedDate)
            intent.putExtra("MARK_AS_READ", true)
            startActivity(intent)
        }

        // Added Read Today button functionality (moved from MainActivity)
//        btnReadToday.setOnClickListener {
//            // Get today's date
//            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
//
//            // Navigate to book selection screen
//            val intent = Intent(this, NotesActivity::class.java)
//            intent.putExtra("SELECTED_DATE", today)
//            startActivity(intent)
//        }

//        btnBack.setOnClickListener {
//            finish() // Return to previous activity
//        }
    }

    override fun onResume() {
        super.onResume()
        // Update stats when returning to this activity
        updateReadingStats()
    }

    private fun updateReadingStats() {
        val currentStreak = readingTracker.getCurrentStreak()
        val totalDays = readingTracker.getTotalReadingDays()

        tvStreak.text = "Streak : $currentStreak day(s)"
        tvTotalReadingDays.text = "Total reading days: $totalDays"
    }

    private fun isDateValid(dateString: String): Boolean {
        try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val selectedDate = dateFormat.parse(dateString)

            val today = Calendar.getInstance()
            today.set(Calendar.HOUR_OF_DAY, 0)
            today.set(Calendar.MINUTE, 0)
            today.set(Calendar.SECOND, 0)
            today.set(Calendar.MILLISECOND, 0)

            return !selectedDate.after(today.time)
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }
}