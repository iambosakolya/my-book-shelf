package com.example.notes_app

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.notes_app.data.Book
import com.example.notes_app.ui.BookViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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
    private lateinit var bookViewModel: BookViewModel
    
    private var isEditMode = false
    private var bookId: Long = -1L
    private var noteIndex = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_note)

        // Initialize ViewModel
        bookViewModel = ViewModelProvider(this)[BookViewModel::class.java]
        Log.d("CreateNoteActivity", "ViewModel initialized")

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

        // Set RatingBar color programmatically
        setRatingBarColor(ratingBar)

        // Setup reading status spinner
        val readingStatuses = arrayOf("Not Started", "In Progress", "Completed")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, readingStatuses)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerReadingStatus.adapter = adapter

        // Get selected date from intent or use today's date
        selectedDate = intent.getStringExtra("SELECTED_DATE") ?:
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        
        // Check if we're in edit mode
        bookId = intent.getLongExtra("BOOK_ID", -1L)
        val editNote = intent.getStringExtra("EDIT_NOTE")
        
        if (bookId != -1L) {
            // We have a valid book ID to edit
            isEditMode = true
            
            // Get book by ID - we'll use the repository directly to avoid LiveData issues
            lifecycleScope.launch(Dispatchers.Main) {
                try {
                    val app = application as NotesApplication
                    val book = app.database.bookDao().getBookByIdDirect(bookId)
                    
                    if (book != null) {
                        Log.d("CreateNoteActivity", "Editing book: ${book.title}, ID: ${book.id}")
                        editTextBookTitle.setText(book.title)
                        editTextBookAuthor.setText(book.author)
                        editTextBookPages.setText(book.totalPages.toString())
                        editTextCurrentPage.setText(book.currentPage.toString())
                        editTextReview.setText(book.review)
                        editTextNote.setText(book.description)
                        ratingBar.rating = book.rating
                        
                        // Set reading status if available
                        val position = readingStatuses.indexOf(book.status)
                        if (position >= 0) {
                            spinnerReadingStatus.setSelection(position)
                        }
                    } else {
                        Log.e("CreateNoteActivity", "Book not found with ID: $bookId")
                        Toast.makeText(this@CreateNoteActivity, "Error: Book not found", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                } catch (e: Exception) {
                    Log.e("CreateNoteActivity", "Error loading book: ${e.message}")
                    Toast.makeText(this@CreateNoteActivity, "Error loading book: ${e.message}", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        } else if (editNote != null) {
            // Legacy edit mode with string data
            isEditMode = true
            noteIndex = intent.getIntExtra("NOTE_INDEX", -1)
            
            // Parse book data from the note format
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
                Log.d("BookUpdate", "Validation failed: $currentPageNum > $totalPages")
                Toast.makeText(this, "Current page ($currentPageNum) cannot exceed total pages ($totalPages)", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Double-check date validity before saving
            if (!isDateValid(selectedDate)) {
                Toast.makeText(this, "Cannot add books for future dates", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            // Save using only Room database via ViewModel
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    // Check if we're in edit mode with a valid book ID
                    if (isEditMode && bookId != -1L) {
                        // Update existing book
                        Log.d("CreateNoteActivity", "Updating existing book with ID: $bookId")
                        bookViewModel.updateBook(
                            bookId = bookId,
                            title = bookTitle,
                            author = bookAuthor,
                            totalPages = totalPages,
                            currentPage = currentPageNum,
                            status = readingStatus,
                            rating = rating,
                            review = review,
                            description = bookDescription
                        )
                    } else {
                        // Add new book
                        Log.d("CreateNoteActivity", "Adding new book: $bookTitle")
                        bookViewModel.addBook(
                            title = bookTitle,
                            author = bookAuthor,
                            totalPages = totalPages,
                            currentPage = currentPageNum,
                            status = readingStatus,
                            rating = rating,
                            review = review,
                            description = bookDescription,
                            coverImageUrl = ""
                        )
                        Log.d("CreateNoteActivity", "New book added")
                    }
                    
                    // Update reading streak for the selected date
                    bookViewModel.updateReadingStreak(selectedDate)
                    
                    // Debug: Check the database status
                    try {
                        val dbDao = (application as NotesApplication).database.bookDao()
                        val count = dbDao.getBooksCount()
                        Log.d("CreateNoteActivity", "Database now contains $count books")
                    } catch (e: Exception) {
                        Log.e("CreateNoteActivity", "Error getting book count: ${e.message}")
                    }
                    
                    // Return to the main thread to show success message
                    runOnUiThread {
                        Toast.makeText(this@CreateNoteActivity, 
                            if (isEditMode) "Book updated" else "Book saved", 
                            Toast.LENGTH_SHORT).show()
                            
                        // Navigate back
                        val intent = Intent(this@CreateNoteActivity, NotesActivity::class.java)
                        intent.putExtra("SELECTED_DATE", selectedDate)
                        startActivity(intent)
                        finish()
                    }
                } catch (e: Exception) {
                    Log.e("CreateNoteActivity", "Error saving book: ${e.message}")
                    e.printStackTrace()
                    
                    // Show error on main thread
                    runOnUiThread {
                        Toast.makeText(this@CreateNoteActivity, 
                            "Error saving book: ${e.message}", 
                            Toast.LENGTH_LONG).show()
                    }
                }
            }
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
        data["coverImageUrl"] = ""
        
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
                    key.equals("CoverImageUrl", ignoreCase = true) -> data["coverImageUrl"] = value
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

    // Helper method to set rating bar color
    private fun setRatingBarColor(ratingBar: RatingBar) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ratingBar.progressTintList = ContextCompat.getColorStateList(this, R.color.streak)
        }
    }
}