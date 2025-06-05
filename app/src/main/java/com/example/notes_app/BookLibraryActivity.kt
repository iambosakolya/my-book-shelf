package com.example.notes_app

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import android.widget.AdapterView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.notes_app.data.Book
import com.example.notes_app.ui.BookViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class BookLibraryActivity : AppCompatActivity() {

    private lateinit var booksContainer: LinearLayout
    private lateinit var bookViewModel: BookViewModel
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val displayDateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_book_library)

        // Initialize ViewModel
        bookViewModel = ViewModelProvider(this)[BookViewModel::class.java]

        // Initialize UI Components
        booksContainer = findViewById(R.id.booksContainer)
        val btnAddNewBook = findViewById<Button>(R.id.btnAddNewBook)
        val btnMyReviews = findViewById<Button>(R.id.btnMyReviews)
        val btnBack = findViewById<Button>(R.id.btnBack)

        // Observe books
        bookViewModel.allBooks.observe(this) { books ->
            displayBooks(books)
        }

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
        Log.d("BookLibraryActivity", "onResume called")
        
        // Test database access in onResume
        val application = application as NotesApplication
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val count = application.database.bookDao().getBooksCount()
                Log.d("BookLibraryActivity", "Database contains $count books")
                
                // If no books yet, add a test book
                if (count == 0) {
                    Log.d("BookLibraryActivity", "Adding a test book")
                    bookViewModel.addBook(
                        title = "Test Book",
                        author = "Test Author",
                        totalPages = 100
                    )
                }
            } catch (e: Exception) {
                Log.e("BookLibraryActivity", "Error accessing database: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    private fun displayBooks(books: List<Book>) {
        booksContainer.removeAllViews()
        
        if (books.isEmpty()) {
            val emptyView = TextView(this)
            emptyView.text = "No books in your library yet"
            emptyView.textSize = 16f
            emptyView.setPadding(16, 16, 16, 16)
            booksContainer.addView(emptyView)
        } else {
            // Display each book
            for (book in books) {
                addBookView(book)
            }
        }
    }

    private fun addBookView(book: Book) {
        val bookView = LayoutInflater.from(this).inflate(R.layout.book_library_item, booksContainer, false)

        val textViewTitle = bookView.findViewById<TextView>(R.id.textViewBookTitle)
        val textViewAuthor = bookView.findViewById<TextView>(R.id.textViewBookAuthor)
        val textViewDetails = bookView.findViewById<TextView>(R.id.textViewBookDetails)
        val textViewDate = bookView.findViewById<TextView>(R.id.textViewLastRead)
        val btnUpdateProgress = bookView.findViewById<Button>(R.id.btnUpdateProgress)
        val btnViewHistory = bookView.findViewById<Button>(R.id.btnViewHistory)
        val btnDeleteBook = bookView.findViewById<Button>(R.id.btnDeleteBookItem)
        val btnHistorySpace = bookView.findViewById<View>(R.id.btnHistorySpace)
        val imageViewBookCover = bookView.findViewById<ImageView>(R.id.imageViewBookCover)
        
        // Display basic book info
        textViewTitle.text = book.title
        textViewAuthor.text = "By: ${book.author}"
        
        // Check if book has cover image URL and set if available
        if (book.coverImageUrl.isNotEmpty()) {
            imageViewBookCover.visibility = View.VISIBLE
            Glide.with(this)
                .load(book.coverImageUrl)
                .placeholder(R.drawable.ic_calendar)
                .into(imageViewBookCover)
        } else {
            imageViewBookCover.visibility = View.GONE
        }
        
        // Format details
        val pagesInfo = buildString {
            append("Progress: ${book.currentPage}/${book.totalPages} pages")
                
                // Calculate percentage if possible
            if (book.totalPages > 0) {
                val percentage = (book.currentPage.toFloat() / book.totalPages * 100).toInt()
                        append(" ($percentage%)")
                    }
            
            append("\nStatus: ${book.status}")
        }
        textViewDetails.text = pagesInfo
        
        // Format date using the new formatter from Converters
        textViewDate.text = "Last read: ${com.example.notes_app.data.Converters.formatDateForDisplay(book.lastReadDate)}"
        
        // Check if book has reading history entries
        bookViewModel.getBookHistory(book.id).observe(this) { progressEntries ->
            val hasHistory = progressEntries.size > 1
            if (hasHistory) {
                btnViewHistory.visibility = View.VISIBLE
                btnHistorySpace.visibility = View.GONE
            } else {
                btnViewHistory.visibility = View.GONE
                btnHistorySpace.visibility = View.VISIBLE
            }
        }
        
        // Set click listeners
        btnUpdateProgress.setOnClickListener {
            showUpdateProgressDialog(book)
        }
        
        btnViewHistory.setOnClickListener {
            // Show book reading history
            val intent = Intent(this, BookHistoryActivity::class.java)
            intent.putExtra("BOOK_ID", book.id)
            intent.putExtra("BOOK_TITLE", book.title)
            startActivity(intent)
        }
        
        // Set delete button click listener
        btnDeleteBook.setOnClickListener {
            showDeleteConfirmationDialog(book)
        }

        booksContainer.addView(bookView)
    }

    private fun showUpdateProgressDialog(book: Book) {
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
        
        // Set current values
        editTextCurrentPage.setText(book.currentPage.toString())
        
        // Set status if available
        val oldStatus = book.status
        val position = readingStatuses.indexOf(book.status)
            if (position >= 0) {
                spinnerReadingStatus.setSelection(position)
        }
        
        // Set rating and review
        ratingBar.rating = book.rating
        editTextReview.setText(book.review)
        
        // Set default date to today
        var selectedDate = Date()
        textViewDate.text = "Date: ${displayDateFormat.format(selectedDate)}"
        
        // Date selection
        btnSelectDate.setOnClickListener {
            showDatePicker { newDate ->
                selectedDate = newDate
                textViewDate.text = "Date: ${displayDateFormat.format(selectedDate)}"
            }
        }
        
        // Add status change listener to prompt for rating/review when completing a book
        spinnerReadingStatus.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedStatus = readingStatuses[position]
                
                // Show rating/review section when marking as completed or when there's already a review
                ratingReviewContainer.visibility = if (selectedStatus == "Completed" || 
                                                     book.rating > 0 || 
                                                     book.review.isNotEmpty()) 
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
                                             book.rating > 0 || 
                                             book.review.isNotEmpty()) 
                                             View.VISIBLE else View.GONE
        
        // Create dialog with custom view and no buttons (we'll use our own)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()
        
        // Set up the custom button click listeners
        btnSave.setOnClickListener {
            val newCurrentPage = editTextCurrentPage.text.toString().toIntOrNull() ?: 0
            // Make sure book.totalPages is also an Int and not 0
            val newStatus = spinnerReadingStatus.selectedItem.toString()
            val newRating = ratingBar.rating
            val newReview = editTextReview.text.toString()
        
            // Validate input
            if (book.totalPages > 0 && newCurrentPage > book.totalPages) {
                Toast.makeText(this, "Current page cannot exceed total pages", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            // Use ViewModel to update the book
            bookViewModel.updateBookProgress(
                bookId = book.id,
                currentPage = newCurrentPage,
                status = newStatus,
                rating = newRating,
                review = newReview
            )
            
            // Update reading streak
            val dateStr = dateFormat.format(selectedDate)
            bookViewModel.updateReadingStreak(dateStr)
            
            Log.d("BookLibraryActivity", "Current page: $newCurrentPage, Total pages: ${book.totalPages}")
            
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
    
    private fun showDatePicker(onDateSelected: (Date) -> Unit) {
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
                    onDateSelected(selectedCalendar.time)
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

    // Helper method to set rating bar color
    private fun setRatingBarColor(ratingBar: RatingBar) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ratingBar.progressTintList = ContextCompat.getColorStateList(this, R.color.streak)
        }
    }

    private fun showDeleteConfirmationDialog(book: Book) {
        AlertDialog.Builder(this)
            .setTitle("Delete Book")
            .setMessage("Are you sure you want to delete \"${book.title}\"? This will also delete all reading progress history for this book.")
            .setPositiveButton("Delete") { _, _ ->
                // Delete the book using ViewModel
                bookViewModel.deleteBook(book)
                Toast.makeText(this, "\"${book.title}\" has been deleted", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .setCancelable(true)
            .show()
    }
} 