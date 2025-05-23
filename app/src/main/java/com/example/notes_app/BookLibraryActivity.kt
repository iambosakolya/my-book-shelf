package com.example.notes_app

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import android.widget.AdapterView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.*
import com.bumptech.glide.Glide

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
        val btnMyReviews = findViewById<Button>(R.id.btnMyReviews)
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

        btnMyReviews.setOnClickListener {
            val intent = Intent(this, MyReviewsActivity::class.java)
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
        val btnHistorySpace = bookView.findViewById<View>(R.id.btnHistorySpace)
        val imageViewBookCover = bookView.findViewById<ImageView>(R.id.imageViewBookCover)

        // Parse book data
        val parsedData = parseBookData(bookData)
        
        // Display basic book info
        textViewTitle.text = parsedData["title"] ?: "Untitled"
        textViewAuthor.text = "By: ${parsedData["author"] ?: "Unknown"}"
        
        // Check if book has cover image URL and set if available
        val coverImageUrl = parsedData["coverImageUrl"]
        if (!coverImageUrl.isNullOrEmpty()) {
            imageViewBookCover.visibility = View.VISIBLE
            Glide.with(this)
                .load(coverImageUrl)
                .placeholder(R.drawable.ic_calendar)
                .into(imageViewBookCover)
        } else {
            imageViewBookCover.visibility = View.GONE
        }
        
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
        if (hasMultipleEntries) {
            btnViewHistory.visibility = View.VISIBLE
            btnHistorySpace.visibility = View.GONE
        } else {
            btnViewHistory.visibility = View.GONE
            btnHistorySpace.visibility = View.VISIBLE
        }
        
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
        val ratingBar = dialogView.findViewById<RatingBar>(R.id.ratingBar)
        val editTextReview = dialogView.findViewById<EditText>(R.id.editTextReview)
        val ratingReviewContainer = dialogView.findViewById<LinearLayout>(R.id.ratingReviewContainer)
        val btnSave = dialogView.findViewById<Button>(R.id.btnSave)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)
        
        // Set RatingBar color programmatically
        setRatingBarColor(ratingBar)
        
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
        val oldStatus = bookData["status"] ?: ""
        bookData["status"]?.let { status ->
            val position = readingStatuses.indexOf(status)
            if (position >= 0) {
                spinnerReadingStatus.setSelection(position)
            }
        }
        
        // Set rating if available
        bookData["rating"]?.toFloatOrNull()?.let { rating ->
            ratingBar.rating = rating
        }
        
        // Set review if available
        bookData["review"]?.let { review ->
            editTextReview.setText(review)
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
        
        // Add status change listener to prompt for rating/review when completing a book
        spinnerReadingStatus.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedStatus = readingStatuses[position]
                
                // Show rating/review section when marking as completed or when there's already a review
                ratingReviewContainer.visibility = if (selectedStatus == "Completed" || 
                                                     bookData["rating"]?.isNotEmpty() == true || 
                                                     bookData["review"]?.isNotEmpty() == true) 
                                                     View.VISIBLE else View.GONE
                
                // If user changes status to Completed, prompt to add a final review
                if (selectedStatus == "Completed" && oldStatus != "Completed") {
                    showRatingReviewPrompt(ratingBar, editTextReview)
                }
            }
            
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        
        // Initially hide or show rating/review section based on status
        val initialStatus = spinnerReadingStatus.selectedItem.toString()
        ratingReviewContainer.visibility = if (initialStatus == "Completed" || 
                                             bookData["rating"]?.isNotEmpty() == true || 
                                             bookData["review"]?.isNotEmpty() == true) 
                                             View.VISIBLE else View.GONE
        
        // Create dialog with custom view and no buttons (we'll use our own)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()
        
        // Set up the custom button click listeners
        btnSave.setOnClickListener {
            // Update book progress
            val newCurrentPage = editTextCurrentPage.text.toString()
            val newStatus = spinnerReadingStatus.selectedItem.toString()
            val newRating = ratingBar.rating.toString()
            val newReview = editTextReview.text.toString()
            
            // Create updated book data
            val updatedBookData = createUpdatedBookData(
                originalBookData, 
                newCurrentPage, 
                newStatus,
                newRating,
                newReview
            )
            
            // Save to the selected date
            notesManager.addNote(selectedDate, updatedBookData)
            
            // Mark the day as read
            readingTracker.markDayAsRead(selectedDate)
            
            // Refresh the display
            displayAllBooks()
            
            Toast.makeText(this, "Reading progress updated", Toast.LENGTH_SHORT).show()
            
            // Dismiss the dialog
            dialog.dismiss()
        }
        
        btnCancel.setOnClickListener {
            // Just dismiss the dialog
            dialog.dismiss()
        }
        
        // Show the dialog
        dialog.show()
    }
    
    private fun showRatingReviewPrompt(ratingBar: RatingBar, editTextReview: EditText) {
        AlertDialog.Builder(this)
            .setTitle("Book Completed!")
            .setMessage("Would you like to add a final rating and review for this book? This will be saved as your final thoughts about the book.")
            .setPositiveButton("Yes") { _, _ ->
                // Focus on the rating/review fields, they're already visible
                ratingBar.requestFocus()
            }
            .setNegativeButton("Not now") { _, _ -> }
            .setCancelable(false) // User must make a choice
            .show()
    }
    
    private fun createUpdatedBookData(
        originalData: String, 
        newCurrentPage: String, 
        newStatus: String,
        newRating: String,
        newReview: String
    ): String {
        val data = parseBookData(originalData).toMutableMap()
        
        // Update the fields that changed
        data["currentPage"] = newCurrentPage
        data["status"] = newStatus
        data["rating"] = newRating
        data["review"] = newReview
        
        // If status changed to Completed, check if pages match total
        if (newStatus == "Completed") {
            val totalPages = data["pages"]
            if (!totalPages.isNullOrEmpty() && totalPages != "0") {
                // Set current page to total pages if completed
                data["currentPage"] = totalPages
            }
        }
        
        // Format the updated book data
        return formatBookData(
            data["title"] ?: "",
            data["author"] ?: "",
            data["pages"] ?: "",
            data["currentPage"] ?: "",
            newStatus,
            newRating,
            newReview,
            data["description"] ?: "",
            data["coverImageUrl"] ?: ""
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
        description: String,
        coverImageUrl: String = ""
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
            CoverImageUrl: $coverImageUrl
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

    // Helper method to set rating bar color
    private fun setRatingBarColor(ratingBar: RatingBar) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ratingBar.progressTintList = ContextCompat.getColorStateList(this, R.color.streak)
        }
    }
} 