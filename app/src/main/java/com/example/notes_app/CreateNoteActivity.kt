package com.example.notes_app

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.*

class CreateNoteActivity : AppCompatActivity() {

    private lateinit var editTextNote: EditText
    private lateinit var editTextBookTitle: EditText
    private lateinit var editTextBookAuthor: EditText
    private lateinit var editTextBookPages: EditText
    private lateinit var editTextCurrentPage: EditText
    private lateinit var editTextReview: EditText
    private lateinit var spinnerReadingStatus: Spinner
    private lateinit var ratingBar: RatingBar
    private lateinit var selectedDate: String
    private lateinit var dateDisplayTextView: TextView
    private var isEditMode = false
    private var noteIndex = -1
    private val notesManager = NotesManager.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_note)

        // Initialize UI Components
        editTextNote = findViewById(R.id.editTextNote)
        editTextBookTitle = findViewById(R.id.editTextBookTitle)
        editTextBookAuthor = findViewById(R.id.editTextBookAuthor)
        editTextBookPages = findViewById(R.id.editTextBookPages)
        editTextCurrentPage = findViewById(R.id.editTextCurrentPage)
        editTextReview = findViewById(R.id.editTextReview)
        spinnerReadingStatus = findViewById(R.id.spinnerReadingStatus)
        ratingBar = findViewById(R.id.ratingBar)
        dateDisplayTextView = findViewById(R.id.dateDisplayTextView)
        val btnSave = findViewById<Button>(R.id.btnSave)
        val btnClear = findViewById<Button>(R.id.btnClear)
        val btnBack = findViewById<Button>(R.id.btnBack)
        val btnHome = findViewById<Button>(R.id.btnHome)

        // Setup reading status spinner
        val readingStatuses = arrayOf("Not Started", "In Progress", "Completed")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, readingStatuses)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerReadingStatus.adapter = adapter

        // Get selected date from intent or use today's date
        selectedDate = intent.getStringExtra("SELECTED_DATE") ?:
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        // Validate the date is not in the future
        if (!isDateValid(selectedDate)) {
            Toast.makeText(this, "Cannot add books for future dates", Toast.LENGTH_LONG).show()
            // Return to main activity
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
            finish()
            return
        }

        // Check if we're in edit mode
        val editNote = intent.getStringExtra("EDIT_NOTE")
        if (editNote != null) {
            isEditMode = true
            noteIndex = intent.getIntExtra("NOTE_INDEX", -1)

            // Parse book data from the note format if in edit mode
            try {
                val bookData = parseBookData(editNote)
                editTextBookTitle.setText(bookData["title"])
                editTextBookAuthor.setText(bookData["author"])
                editTextBookPages.setText(bookData["pages"])
                editTextCurrentPage.setText(bookData["currentPage"])
                editTextReview.setText(bookData["review"])
                editTextNote.setText(bookData["description"])
                
                // Set rating if available
                bookData["rating"]?.toFloatOrNull()?.let { rating ->
                    ratingBar.rating = rating
                }
                
                // Set reading status if available
                bookData["status"]?.let { status ->
                    val position = readingStatuses.indexOf(status)
                    if (position >= 0) {
                        spinnerReadingStatus.setSelection(position)
                    }
                }
            } catch (e: Exception) {
                // If parsing fails, just put everything in the description field
                editTextNote.setText(editNote)
            }
        }

        // Format the date for display
        try {
            val parsedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(selectedDate)
            val displayFormat = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
            dateDisplayTextView.text = if (isEditMode) {
                "Edit Book - ${displayFormat.format(parsedDate!!)}"
            } else {
                "New Book - ${displayFormat.format(parsedDate!!)}"
            }
        } catch (e: Exception) {
            dateDisplayTextView.text = if (isEditMode) "Edit Book - $selectedDate" else "New Book - $selectedDate"
        }

        // Set click listeners for buttons
        btnSave.setOnClickListener {
            val bookTitle = editTextBookTitle.text.toString().trim()
            val bookAuthor = editTextBookAuthor.text.toString().trim()
            val bookPages = editTextBookPages.text.toString().trim()
            val currentPage = editTextCurrentPage.text.toString().trim()
            val bookDescription = editTextNote.text.toString().trim()
            val review = editTextReview.text.toString().trim()
            val rating = ratingBar.rating
            val readingStatus = spinnerReadingStatus.selectedItem.toString()

            if (bookTitle.isEmpty()) {
                Toast.makeText(this, "Please enter a book title", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Validate current page is not greater than total pages
            val totalPages = bookPages.toIntOrNull() ?: 0
            val currentPageNum = currentPage.toIntOrNull() ?: 0
            if (totalPages > 0 && currentPageNum > totalPages) {
                Toast.makeText(this, "Current page cannot be greater than total pages", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Double-check date validity before saving
            if (!isDateValid(selectedDate)) {
                Toast.makeText(this, "Cannot add books for future dates", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            // Format book data into a structured string
            val bookData = formatBookData(
                bookTitle,
                bookAuthor,
                bookPages,
                currentPage,
                readingStatus,
                rating.toString(),
                review,
                bookDescription
            )

            // Save the book data
            if (isEditMode && noteIndex >= 0) {
                notesManager.updateNote(selectedDate, noteIndex, bookData)
                Toast.makeText(this, "Book updated", Toast.LENGTH_SHORT).show()
            } else {
                notesManager.addNote(selectedDate, bookData)
                
                // Also mark this day as a reading day for streak tracking
                ReadingTrackerManager.getInstance().markDayAsRead(selectedDate)
                
                Toast.makeText(this, "Book saved", Toast.LENGTH_SHORT).show()
            }

            // Return to NotesActivity
            val intent = Intent(this, NotesActivity::class.java)
            intent.putExtra("SELECTED_DATE", selectedDate)
            startActivity(intent)
            finish()
        }

        btnClear.setOnClickListener {
            editTextBookTitle.text.clear()
            editTextBookAuthor.text.clear()
            editTextBookPages.text.clear()
            editTextCurrentPage.text.clear()
            editTextReview.text.clear()
            editTextNote.text.clear()
            ratingBar.rating = 0f
            spinnerReadingStatus.setSelection(0)
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

    private fun formatBookData(
        title: String,
        author: String,
        pages: String,
        currentPage: String,
        status: String,
        rating: String,
        review: String,
        description: String
    ): String {
        // Consistent, standardized format for all book data
        return """
            Title: $title
            Author: $author
            Total Pages: $pages
            Current Page: $currentPage
            Status: $status
            Rating: $rating
            Review: $review
            Description: $description
        """.trimIndent()
    }

    private fun parseBookData(note: String): Map<String, String> {
        val data = mutableMapOf<String, String>()
        
        // Initialize with empty default values
        data["title"] = ""
        data["author"] = ""
        data["pages"] = ""
        data["currentPage"] = ""
        data["status"] = ""
        data["rating"] = ""
        data["review"] = ""
        data["description"] = ""
        
        note.split("\n").forEach { line ->
            val parts = line.split(":", limit = 2)
            if (parts.size == 2) {
                val key = parts[0].trim()
                val value = parts[1].trim()
                
                // Normalize keys to our standard format
                when {
                    key.equals("Title", ignoreCase = true) -> data["title"] = value
                    key.equals("Author", ignoreCase = true) -> data["author"] = value
                    key.equals("Total Pages", ignoreCase = true) || key.equals("Pages", ignoreCase = true) -> data["pages"] = value
                    key.equals("Current Page", ignoreCase = true) -> data["currentPage"] = value
                    key.equals("Status", ignoreCase = true) -> data["status"] = value
                    key.equals("Rating", ignoreCase = true) -> data["rating"] = value
                    key.equals("Review", ignoreCase = true) -> data["review"] = value
                    key.equals("Description", ignoreCase = true) -> data["description"] = value
                }
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