package com.example.notes_app

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.*

class BookLibraryActivity : AppCompatActivity() {

    private lateinit var booksContainer: LinearLayout
    private val notesManager = NotesManager.getInstance()
    private val readingTracker = ReadingTrackerManager.getInstance()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val displayDateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_book_library)

        // Initialize UI Components
        booksContainer = findViewById(R.id.booksContainer)
        val btnAddNewBook = findViewById<Button>(R.id.btnAddNewBook)
        val btnBack = findViewById<Button>(R.id.btnBack)

        // Display all books
        displayAllBooks()

        // Set click listeners for buttons
        btnAddNewBook.setOnClickListener {
            // Navigate to CreateNoteActivity to add a new book for today
            val today = dateFormat.format(Date())
            val intent = Intent(this, CreateNoteActivity::class.java)
            intent.putExtra("SELECTED_DATE", today)
            startActivity(intent)
        }

        btnBack.setOnClickListener {
            finish() // Return to previous activity
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh books when returning to this activity
        displayAllBooks()
    }

    private fun displayAllBooks() {
        booksContainer.removeAllViews()

        // Get all dates with notes
        val datesWithNotes = notesManager.getDatesWithNotes()
        
        // Create a list to store all books with their dates
        val allBooks = mutableListOf<Pair<String, String>>() // Pair<Date, BookData>
        
        // Collect all books
        for (date in datesWithNotes) {
            val notes = notesManager.getNotesForDate(date)
            for (note in notes) {
                allBooks.add(Pair(date, note))
            }
        }
        
        if (allBooks.isEmpty()) {
            val emptyView = TextView(this)
            emptyView.text = "No books in your library yet"
            emptyView.textSize = 16f
            emptyView.setPadding(16, 16, 16, 16)
            booksContainer.addView(emptyView)
        } else {
            // Group books by title to avoid duplicates
            val booksByTitle = mutableMapOf<String, MutableList<Pair<String, String>>>()
            
            for (bookData in allBooks) {
                val data = parseBookData(bookData.second)
                val title = data["title"] ?: "Untitled"
                
                if (!booksByTitle.containsKey(title)) {
                    booksByTitle[title] = mutableListOf()
                }
                
                booksByTitle[title]?.add(bookData)
            }
            
            // Display each unique book
            for ((title, bookEntries) in booksByTitle) {
                // Get the most recent entry for this book
                val latestEntry = bookEntries.maxByOrNull { it.first }
                if (latestEntry != null) {
                    addBookView(latestEntry.first, latestEntry.second, bookEntries.size > 1)
                }
            }
        }
    }

    private fun addBookView(date: String, bookData: String, hasMultipleEntries: Boolean) {
        val bookView = LayoutInflater.from(this).inflate(R.layout.book_library_item, booksContainer, false)

        val textViewTitle = bookView.findViewById<TextView>(R.id.textViewBookTitle)
        val textViewAuthor = bookView.findViewById<TextView>(R.id.textViewBookAuthor)
        val textViewDetails = bookView.findViewById<TextView>(R.id.textViewBookDetails)
        val textViewDate = bookView.findViewById<TextView>(R.id.textViewLastRead)
        val btnUpdateProgress = bookView.findViewById<Button>(R.id.btnUpdateProgress)
        val btnViewHistory = bookView.findViewById<Button>(R.id.btnViewHistory)

        // Parse book data
        val parsedData = parseBookData(bookData)
        
        // Display basic book info
        textViewTitle.text = parsedData["title"] ?: "Untitled"
        textViewAuthor.text = "By: ${parsedData["author"] ?: "Unknown"}"
        
        // Format details
        val pagesInfo = buildString {
            val totalPages = parsedData["pages"] ?: ""
            val currentPage = parsedData["currentPage"] ?: ""
            val status = parsedData["status"] ?: ""
            
            if (totalPages.isNotEmpty() && currentPage.isNotEmpty()) {
                append("Progress: $currentPage/$totalPages pages")
                
                // Calculate percentage if possible
                try {
                    val current = currentPage.toInt()
                    val total = totalPages.toInt()
                    if (total > 0) {
                        val percentage = (current.toFloat() / total * 100).toInt()
                        append(" ($percentage%)")
                    }
                } catch (e: Exception) {
                    // Skip percentage if calculation fails
                }
                
                append("\n")
            }
            
            if (status.isNotEmpty()) {
                append("Status: $status")
            }
        }
        textViewDetails.text = pagesInfo
        
        // Format date
        try {
            val parsedDate = dateFormat.parse(date)
            textViewDate.text = "Last read: ${displayDateFormat.format(parsedDate!!)}"
        } catch (e: Exception) {
            textViewDate.text = "Last read: $date"
        }
        
        // Set button visibility based on multiple entries
        btnViewHistory.visibility = if (hasMultipleEntries) View.VISIBLE else View.GONE
        
        // Set click listeners
        btnUpdateProgress.setOnClickListener {
            showUpdateProgressDialog(date, bookData)
        }
        
        btnViewHistory.setOnClickListener {
            // Show book reading history
            val title = parsedData["title"] ?: "Untitled"
            val intent = Intent(this, BookHistoryActivity::class.java)
            intent.putExtra("BOOK_TITLE", title)
            startActivity(intent)
        }

        booksContainer.addView(bookView)
    }

    private fun showUpdateProgressDialog(originalDate: String, originalBookData: String) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_update_progress, null)
        val editTextCurrentPage = dialogView.findViewById<EditText>(R.id.editTextCurrentPage)
        val spinnerReadingStatus = dialogView.findViewById<Spinner>(R.id.spinnerReadingStatus)
        val textViewDate = dialogView.findViewById<TextView>(R.id.textViewSelectedDate)
        val btnSelectDate = dialogView.findViewById<Button>(R.id.btnSelectDate)
        
        // Setup spinner with reading statuses
        val readingStatuses = arrayOf("Not Started", "In Progress", "Completed")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, readingStatuses)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerReadingStatus.adapter = adapter
        
        // Parse book data
        val bookData = parseBookData(originalBookData)
        
        // Set current values
        editTextCurrentPage.setText(bookData["currentPage"])
        
        // Set status if available
        bookData["status"]?.let { status ->
            val position = readingStatuses.indexOf(status)
            if (position >= 0) {
                spinnerReadingStatus.setSelection(position)
            }
        }
        
        // Set default date to today
        var selectedDate = dateFormat.format(Date())
        updateDateDisplay(textViewDate, selectedDate)
        
        // Date selection
        btnSelectDate.setOnClickListener {
            showDatePicker { newDate ->
                selectedDate = newDate
                updateDateDisplay(textViewDate, selectedDate)
            }
        }
        
        // Create dialog
        val dialog = AlertDialog.Builder(this)
            .setTitle("Update Reading Progress")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                // Update book progress
                val newCurrentPage = editTextCurrentPage.text.toString()
                val newStatus = spinnerReadingStatus.selectedItem.toString()
                
                // Create updated book data
                val updatedBookData = createUpdatedBookData(originalBookData, newCurrentPage, newStatus)
                
                // Save to the selected date
                notesManager.addNote(selectedDate, updatedBookData)
                
                // Mark the day as read
                readingTracker.markDayAsRead(selectedDate)
                
                // Refresh the display
                displayAllBooks()
                
                Toast.makeText(this, "Reading progress updated", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .create()
        
        dialog.show()
    }
    
    private fun createUpdatedBookData(originalData: String, newCurrentPage: String, newStatus: String): String {
        val data = parseBookData(originalData).toMutableMap()
        
        // Update the fields that changed
        data["currentPage"] = newCurrentPage
        data["status"] = newStatus
        
        // Format the updated book data
        return formatBookData(
            data["title"] ?: "",
            data["author"] ?: "",
            data["pages"] ?: "",
            newCurrentPage,
            newStatus,
            data["rating"] ?: "",
            data["review"] ?: "",
            data["description"] ?: ""
        )
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
    
    private fun updateDateDisplay(textView: TextView, dateString: String) {
        try {
            val date = dateFormat.parse(dateString)
            textView.text = "Date: ${displayDateFormat.format(date!!)}"
        } catch (e: Exception) {
            textView.text = "Date: $dateString"
        }
    }
    
    private fun showDatePicker(onDateSelected: (String) -> Unit) {
        val calendar = Calendar.getInstance()
        
        // Don't allow future dates
        val today = Calendar.getInstance()
        
        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val selectedCalendar = Calendar.getInstance()
                selectedCalendar.set(Calendar.YEAR, year)
                selectedCalendar.set(Calendar.MONTH, month)
                selectedCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                
                if (selectedCalendar.after(today)) {
                    Toast.makeText(this, "Cannot select future dates", Toast.LENGTH_SHORT).show()
                } else {
                    val selectedDateStr = dateFormat.format(selectedCalendar.time)
                    onDateSelected(selectedDateStr)
                }
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        
        // Set max date to today
        datePickerDialog.datePicker.maxDate = today.timeInMillis
        
        datePickerDialog.show()
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
            try {
                val parts = line.split(":", limit = 2)
                if (parts.size == 2) {
                    val key = parts[0].trim()
                    val value = parts[1].trim()
                    
                    // Normalize keys for easier access
                    when {
                        key.equals("Title", ignoreCase = true) -> 
                            data["title"] = value
                        key.equals("Author", ignoreCase = true) -> 
                            data["author"] = value
                        key.equals("Total Pages", ignoreCase = true) || key.equals("Pages", ignoreCase = true) -> 
                            data["pages"] = value
                        key.equals("Current Page", ignoreCase = true) -> 
                            data["currentPage"] = value
                        key.equals("Status", ignoreCase = true) -> 
                            data["status"] = value
                        key.equals("Rating", ignoreCase = true) -> 
                            data["rating"] = value
                        key.equals("Review", ignoreCase = true) -> 
                            data["review"] = value
                        key.equals("Description", ignoreCase = true) -> 
                            data["description"] = value
                    }
                }
            } catch (e: Exception) {
                // Skip invalid lines
            }
        }
        return data
    }
} 