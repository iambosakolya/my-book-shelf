package com.example.notes_app

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize UI Components
        val btnOpenCalendar = findViewById<Button>(R.id.btnOpenCalendar)
        val btnMyLibrary = findViewById<Button>(R.id.btnMyLibrary)
        val btnCreateNote = findViewById<Button>(R.id.btnCreateNote)
        val currentDateView = findViewById<TextView>(R.id.currentDateView)

        // Set current date
        val dateFormat = SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault())
        currentDateView.text = dateFormat.format(Date())

        // Set click listeners for buttons
        btnOpenCalendar.setOnClickListener {
            val intent = Intent(this, CalendarActivity::class.java)
            startActivity(intent)
        }

        btnMyLibrary.setOnClickListener {
            val intent = Intent(this, BookLibraryActivity::class.java)
            startActivity(intent)
        }

        btnCreateNote.setOnClickListener {
            // Always create note for today's date from main screen
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val intent = Intent(this, CreateNoteActivity::class.java)
            intent.putExtra("SELECTED_DATE", today)
            startActivity(intent)
        }
    }
}
