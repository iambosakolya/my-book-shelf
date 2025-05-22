package com.example.notes_app

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class NotesActivity : AppCompatActivity() {

    private lateinit var notesContainer: LinearLayout
    private lateinit var selectedDate: String
    private lateinit var dateDisplayTextView: TextView
    private val notesManager = NotesManager.getInstance()
    private val readingTracker = ReadingTrackerManager.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notes)

        // Initialize UI Components
        notesContainer = findViewById(R.id.notesContainer)
        dateDisplayTextView = findViewById(R.id.dateDisplayTextView)
        val btnCreateNote = findViewById<Button>(R.id.btnCreateNote)
        val btnBack = findViewById<Button>(R.id.btnBack)
        val btnHome = findViewById<Button>(R.id.btnHome)

        // Get selected date from intent or use today's date
        selectedDate = intent.getStringExtra("SELECTED_DATE") ?:
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        // Format the date for display
        try {
            val parsedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(selectedDate)
            val displayFormat = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
            dateDisplayTextView.text = "Books for ${displayFormat.format(parsedDate!!)}"
        } catch (e: Exception) {
            dateDisplayTextView.text = "Books for $selectedDate"
        }

        // Check if selected date is valid (not in the future)
        if (!isDateValid(selectedDate)) {
            btnCreateNote.isEnabled = false
            Toast.makeText(this, "Cannot add new books for future dates", Toast.LENGTH_SHORT).show()
        } else {
            btnCreateNote.isEnabled = true
            
            // Check if this is a "mark as read" operation from CalendarActivity
            if (intent.getBooleanExtra("MARK_AS_READ", false)) {
                // If there are no books for this date, prompt to add one
                if (notesManager.getNotesForDate(selectedDate).isEmpty()) {
                    Toast.makeText(this, "Add a book to mark this day as read", Toast.LENGTH_LONG).show()
                }
            }
        }

        // Display notes for selected date
        displayNotes()

        // Set click listeners for buttons
        btnCreateNote.setOnClickListener {
            // Double-check before proceeding
            if (!isDateValid(selectedDate)) {
                Toast.makeText(this, "Cannot create notes for future dates", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val intent = Intent(this, CreateNoteActivity::class.java)
            intent.putExtra("SELECTED_DATE", selectedDate)
            startActivity(intent)
        }

        btnBack.setOnClickListener {
            finish() // Return to previous activity
        }

        btnHome.setOnClickListener {
            // Create a new intent to MainActivity and clear the stack
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh notes when returning to this activity
        displayNotes()
    }

    private fun displayNotes() {
        notesContainer.removeAllViews()

        val notes = notesManager.getNotesForDate(selectedDate)

        if (notes.isEmpty()) {
            val emptyView = TextView(this)
            emptyView.text = "No books for this date"
            emptyView.textSize = 16f
            emptyView.setPadding(16, 16, 16, 16)
            notesContainer.addView(emptyView)
        } else {
            for (note in notes) {
                val noteView = LayoutInflater.from(this).inflate(R.layout.note_item, notesContainer, false)

                val textView = noteView.findViewById<TextView>(R.id.textViewNote)
                val btnDelete = noteView.findViewById<Button>(R.id.btnDelete)
                val btnEdit = noteView.findViewById<Button>(R.id.btnEdit)

                // Parse and format the book data for display
                val bookData = parseBookData(note)
                val displayText = buildString {
                    // Use the normalized keys where possible for consistency
                    val title = bookData["title"] ?: ""
                    val author = bookData["author"] ?: ""
                    val pages = bookData["pages"] ?: ""
                    val currentPage = bookData["currentPage"] ?: ""
                    val status = bookData["status"] ?: ""
                    val rating = bookData["rating"] ?: ""
                    val review = bookData["review"] ?: ""
                    val description = bookData["description"] ?: ""
                    
                    append("Title: $title\n")
                    append("Author: $author\n")
                    
                    if (pages.isNotEmpty()) {
                        append("Pages: $pages\n")
                    }
                    
                    if (currentPage.isNotEmpty()) {
                        append("Current Page: $currentPage\n")
                    }
                    
                    if (status.isNotEmpty()) {
                        append("Status: $status\n")
                    }
                    
                    if (rating.isNotEmpty()) {
                        append("Rating: $rating stars\n")
                    }
                    
                    if (review.isNotEmpty()) {
                        append("Review: $review\n")
                    }
                    
                    if (description.isNotEmpty()) {
                        append("Description: $description")
                    }
                }
                textView.text = displayText

                btnDelete.setOnClickListener {
                    notesManager.deleteNote(selectedDate, note)
                    notesContainer.removeView(noteView)
                    Toast.makeText(this, "Book deleted", Toast.LENGTH_SHORT).show()

                    // Refresh the view if all notes are deleted
                    if (notesManager.getNotesForDate(selectedDate).isEmpty()) {
                        displayNotes()
                    }
                }

                btnEdit.setOnClickListener {
                    // Check if the date is valid before allowing edits
                    if (!isDateValid(selectedDate)) {
                        Toast.makeText(this, "Cannot edit books for future dates", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }

                    val intent = Intent(this, CreateNoteActivity::class.java)
                    intent.putExtra("SELECTED_DATE", selectedDate)
                    intent.putExtra("EDIT_NOTE", note)
                    intent.putExtra("NOTE_INDEX", notesManager.getNotesForDate(selectedDate).indexOf(note))
                    startActivity(intent)
                }

                // Only disable edit button for future dates
                btnEdit.isEnabled = isDateValid(selectedDate)

                notesContainer.addView(noteView)
            }
        }
    }

    private fun parseBookData(note: String): Map<String, String> {
        val data = mutableMapOf<String, String>()
        
        // Handle old format keys (TITLE, AUTHOR, etc.) and new format (Title, Author, etc.)
        note.split("\n").forEach { line ->
            try {
                val parts = line.split(":", limit = 2)
                if (parts.size == 2) {
                    val key = parts[0].trim()
                    val value = parts[1].trim()
                    
                    // Store both original key and normalized key to help with transition
                    data[key] = value
                    
                    // Also normalize keys for easier access
                    when {
                        key.equals("Title", ignoreCase = true) || key.equals("TITLE", ignoreCase = true) -> 
                            data["title"] = value
                        key.equals("Author", ignoreCase = true) || key.equals("AUTHOR", ignoreCase = true) -> 
                            data["author"] = value
                        key.equals("Total Pages", ignoreCase = true) || key.equals("Pages", ignoreCase = true) || key.equals("PAGES", ignoreCase = true) -> 
                            data["pages"] = value
                        key.equals("Current Page", ignoreCase = true) -> 
                            data["currentPage"] = value
                        key.equals("Status", ignoreCase = true) -> 
                            data["status"] = value
                        key.equals("Rating", ignoreCase = true) -> 
                            data["rating"] = value
                        key.equals("Review", ignoreCase = true) -> 
                            data["review"] = value
                        key.equals("Description", ignoreCase = true) || key.equals("DESCRIPTION", ignoreCase = true) -> 
                            data["description"] = value
                    }
                }
            } catch (e: Exception) {
                // Skip invalid lines
            }
        }
        return data
    }

    private fun isDateValid(dateString: String): Boolean {
        try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val selectedDate = dateFormat.parse(dateString)

            // Create calendar for today with time set to beginning of day
            val today = Calendar.getInstance()
            today.set(Calendar.HOUR_OF_DAY, 0)
            today.set(Calendar.MINUTE, 0)
            today.set(Calendar.SECOND, 0)
            today.set(Calendar.MILLISECOND, 0)

            // Check if selected date is not in the future
            return !selectedDate.after(today.time)
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }
}