package com.example.notes_app

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.notes_app.data.Book
import com.example.notes_app.ui.BookViewModel
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
    private lateinit var bookViewModel: BookViewModel
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notes)

        // Initialize ViewModel
        bookViewModel = ViewModelProvider(this)[BookViewModel::class.java]
        Log.d("NotesActivity", "ViewModel initialized")

        // Initialize UI Components
        notesContainer = findViewById(R.id.notesContainer)
        dateDisplayTextView = findViewById(R.id.dateDisplayTextView)
        val btnCreateNote = findViewById<Button>(R.id.btnCreateNote)
        val btnSearchBooks = findViewById<Button>(R.id.btnSearchBooks)
        val btnSelectFromLibrary = findViewById<Button>(R.id.btnSelectFromLibrary)
        val btnBack = findViewById<Button>(R.id.btnBack)
        val btnHome = findViewById<Button>(R.id.btnHome)

        // Get selected date from intent or use today's date
        selectedDate = intent.getStringExtra("SELECTED_DATE") ?:
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        Log.d("NotesActivity", "Selected date: $selectedDate")

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
            btnSearchBooks.isEnabled = false
            btnSelectFromLibrary.isEnabled = false
            Toast.makeText(this, "Cannot add new books for future dates", Toast.LENGTH_SHORT).show()
        } else {
            btnCreateNote.isEnabled = true
            btnSearchBooks.isEnabled = true
            btnSelectFromLibrary.isEnabled = true
            
            // Check if this is a "mark as read" operation from CalendarActivity
            if (intent.getBooleanExtra("MARK_AS_READ", false)) {
                // If there are no books for this date, prompt to add one
                if (notesManager.getNotesForDate(selectedDate).isEmpty()) {
                    Toast.makeText(this, "Add a book to mark this day as read", Toast.LENGTH_LONG).show()
                }
            }
        }

        // Observe books from Room database
        // Convert selected date to start and end of day
        try {
            val parsedDate = dateFormat.parse(selectedDate)
            if (parsedDate != null) {
                // Create calendar for start of day
                val startCalendar = Calendar.getInstance()
                startCalendar.time = parsedDate
                startCalendar.set(Calendar.HOUR_OF_DAY, 0)
                startCalendar.set(Calendar.MINUTE, 0)
                startCalendar.set(Calendar.SECOND, 0)
                
                // Create calendar for end of day
                val endCalendar = Calendar.getInstance()
                endCalendar.time = parsedDate
                endCalendar.set(Calendar.HOUR_OF_DAY, 23)
                endCalendar.set(Calendar.MINUTE, 59)
                endCalendar.set(Calendar.SECOND, 59)
                
                // Get books between these dates
                val startDate = startCalendar.time
                val endDate = endCalendar.time
                
                Log.d("NotesActivity", "Looking for books between $startDate and $endDate")
                
                bookViewModel.getBooksReadBetweenDates(startDate, endDate).observe(this) { books ->
                    Log.d("NotesActivity", "Found ${books.size} books for date $selectedDate")
                    displayBooks(books)
                }
            } else {
                Log.e("NotesActivity", "Error parsing date: $selectedDate")
                displayNotes() // Fallback to legacy system
            }
        } catch (e: Exception) {
            Log.e("NotesActivity", "Error setting up date observation: ${e.message}")
            e.printStackTrace()
            displayNotes() // Fallback to legacy system
        }

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
        
        btnSelectFromLibrary.setOnClickListener {
            // Double-check before proceeding
            if (!isDateValid(selectedDate)) {
                Toast.makeText(this, "Cannot add books for future dates", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            // Show dialog to select from library
            showSelectFromLibraryDialog()
        }
        
        btnSearchBooks.setOnClickListener {
            // Double-check before proceeding
            if (!isDateValid(selectedDate)) {
                Toast.makeText(this, "Cannot add books for future dates", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val intent = Intent(this, BookSearchActivity::class.java)
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
        // No need to refresh - the LiveData observer will handle it
        Log.d("NotesActivity", "onResume called")
    }

    private fun displayBooks(books: List<Book>) {
        notesContainer.removeAllViews()
        
        if (books.isEmpty()) {
            val emptyView = TextView(this)
            emptyView.text = "No books for this date"
            emptyView.textSize = 16f
            emptyView.setPadding(16, 16, 16, 16)
            notesContainer.addView(emptyView)
            
            // Also check legacy storage as fallback
            displayNotes()
        } else {
            for (book in books) {
                val noteView = LayoutInflater.from(this).inflate(R.layout.note_item, notesContainer, false)

                val textView = noteView.findViewById<TextView>(R.id.textViewNote)
                val btnDelete = noteView.findViewById<Button>(R.id.btnDelete)
                val btnEdit = noteView.findViewById<Button>(R.id.btnEdit)

                // Format book data for display
                val displayText = buildString {
                    append("Title: ${book.title}\n")
                    append("Author: ${book.author}\n")
                    
                    if (book.totalPages > 0) {
                        append("Pages: ${book.totalPages}\n")
                    }
                    
                    if (book.currentPage > 0) {
                        append("Current Page: ${book.currentPage}\n")
                    }
                    
                    if (book.status.isNotEmpty()) {
                        append("Status: ${book.status}\n")
                    }
                    
                    if (book.rating > 0) {
                        append("Rating: ${book.rating} stars\n")
                    }
                    
                    if (book.review.isNotEmpty()) {
                        append("Review: ${book.review}\n")
                    }
                    
                    if (book.description.isNotEmpty()) {
                        append("Description: ${book.description}")
                    }
                    
                    // Display last read date using formatter
                    append("\n\nLast read: ${com.example.notes_app.data.Converters.formatDateForDisplay(book.lastReadDate)}")
                }
                textView.text = displayText

                btnDelete.setOnClickListener {
                    // Show confirmation dialog before deleting
                    AlertDialog.Builder(this)
                        .setTitle("Delete Book")
                        .setMessage("Are you sure you want to delete \"${book.title}\"? This will also delete all reading progress history for this book.")
                        .setPositiveButton("Delete") { _, _ ->
                            // Delete from Room database
                            bookViewModel.deleteBook(book)
                            notesContainer.removeView(noteView)
                            Toast.makeText(this, "\"${book.title}\" has been deleted", Toast.LENGTH_SHORT).show()
                            
                            // Check if all books have been removed
                            if (notesContainer.childCount == 0) {
                                val emptyView = TextView(this)
                                emptyView.text = "No books for this date"
                                emptyView.textSize = 16f
                                emptyView.setPadding(16, 16, 16, 16)
                                notesContainer.addView(emptyView)
                            }
                        }
                        .setNegativeButton("Cancel", null)
                        .setCancelable(true)
                        .show()
                }

                btnEdit.setOnClickListener {
                    // Check if the date is valid before allowing edits
                    if (!isDateValid(selectedDate)) {
                        Toast.makeText(this, "Cannot edit books for future dates", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }

                    // Open edit screen with Room book ID
                    val intent = Intent(this, CreateNoteActivity::class.java)
                    intent.putExtra("SELECTED_DATE", selectedDate)
                    intent.putExtra("BOOK_ID", book.id)
                    startActivity(intent)
                }

                // Only disable edit button for future dates
                btnEdit.isEnabled = isDateValid(selectedDate)

                notesContainer.addView(noteView)
            }
        }
    }

    // Legacy method - keeps backward compatibility
    private fun displayNotes() {
        // If container already has views, don't override with legacy data
        if (notesContainer.childCount > 0) {
            return
        }
        
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

    // Add method to show the select from library dialog
    private fun showSelectFromLibraryDialog() {
        // Create a dialog to display the list of books
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_select_book, null)
        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.recyclerViewBooks)
        val progressBar = dialogView.findViewById<ProgressBar>(R.id.progressBar)
        val noItemsText = dialogView.findViewById<TextView>(R.id.textViewNoItems)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)
        
        // Create dialog
        val dialog = AlertDialog.Builder(this)
            .setTitle("Select Book from Library")
            .setView(dialogView)
            .setCancelable(true)
            .create()
        
        // Set up RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)
        
        // Get all books from the library
        bookViewModel.allBooks.observe(this) { books ->
            progressBar.visibility = View.GONE
            
            if (books.isEmpty()) {
                noItemsText.visibility = View.VISIBLE
                recyclerView.visibility = View.GONE
            } else {
                noItemsText.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE
                
                // Create adapter with books
                val adapter = object : RecyclerView.Adapter<BookViewHolder>() {
                    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookViewHolder {
                        val view = LayoutInflater.from(parent.context)
                            .inflate(R.layout.item_select_book, parent, false)
                        return BookViewHolder(view)
                    }
                    
                    override fun getItemCount(): Int = books.size
                    
                    override fun onBindViewHolder(holder: BookViewHolder, position: Int) {
                        val book = books[position]
                        holder.bind(book)
                        
                        // Set click listener for the book item
                        holder.itemView.setOnClickListener {
                            // Add this book to the reading history for this date
                            recordBookReadingForDate(book)
                            dialog.dismiss()
                        }
                    }
                }
                
                recyclerView.adapter = adapter
            }
        }
        
        // Set cancel button listener
        btnCancel.setOnClickListener {
            dialog.dismiss()
        }
        
        // Show dialog
        dialog.show()
    }
    
    // ViewHolder for book items in the selection dialog
    private class BookViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleTextView = itemView.findViewById<TextView>(R.id.textViewBookTitle)
        private val authorTextView = itemView.findViewById<TextView>(R.id.textViewBookAuthor)
        private val detailsTextView = itemView.findViewById<TextView>(R.id.textViewBookDetails)
        
        fun bind(book: Book) {
            titleTextView.text = book.title
            authorTextView.text = "By: ${book.author}"
            
            // Format details
            val details = buildString {
                append("Progress: ${book.currentPage}/${book.totalPages} pages")
                
                // Calculate percentage if possible
                if (book.totalPages > 0) {
                    val percentage = (book.currentPage.toFloat() / book.totalPages * 100).toInt()
                    append(" ($percentage%)")
                }
                
                append("\nStatus: ${book.status}")
            }
            
            detailsTextView.text = details
        }
    }
    
    // Add a book to the reading history for the selected date
    private fun recordBookReadingForDate(book: Book) {
        try {
            // Parse the selected date
            val parsedDate = dateFormat.parse(selectedDate)
            
            if (parsedDate != null) {
                // Create a calendar for the selected date
                val calendar = Calendar.getInstance()
                calendar.time = parsedDate
                
                // Add a reading entry for this book on this date
                bookViewModel.updateBookProgress(
                    bookId = book.id,
                    currentPage = book.currentPage, // Keep the current progress
                    status = book.status,           // Keep the current status
                    date = calendar.time            // Use the selected date
                )
                
                Toast.makeText(this, "Added \"${book.title}\" to your reading for ${selectedDate}", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Error adding book: Invalid date", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("NotesActivity", "Error recording book reading: ${e.message}")
            Toast.makeText(this, "Error adding book: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}