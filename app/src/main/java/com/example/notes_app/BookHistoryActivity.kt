package com.example.notes_app

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.*

class BookHistoryActivity : AppCompatActivity() {

    private lateinit var historyContainer: LinearLayout
    private lateinit var tvBookTitle: TextView
    private val notesManager = NotesManager.getInstance()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val displayDateFormat = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_book_history)

        // Initialize UI Components
        historyContainer = findViewById(R.id.historyContainer)
        tvBookTitle = findViewById(R.id.tvBookTitle)
        val btnBack = findViewById<Button>(R.id.btnBack)

        // Get book title from intent
        val bookTitle = intent.getStringExtra("BOOK_TITLE") ?: "Unknown Book"
        tvBookTitle.text = "History: $bookTitle"

        // Display book history
        displayBookHistory(bookTitle)

        // Set click listener for back button
        btnBack.setOnClickListener {
            finish() // Return to previous activity
        }
    }

    private fun displayBookHistory(bookTitle: String) {
        historyContainer.removeAllViews()

        // Get all dates with notes
        val datesWithNotes = notesManager.getDatesWithNotes()
        
        // Create a list to store all entries for this book
        val bookEntries = mutableListOf<Pair<String, String>>() // Pair<Date, BookData>
        
        // Collect all entries for this book
        for (date in datesWithNotes) {
            val notes = notesManager.getNotesForDate(date)
            for (note in notes) {
                val data = parseBookData(note)
                val title = data["title"] ?: ""
                
                if (title.equals(bookTitle, ignoreCase = true)) {
                    bookEntries.add(Pair(date, note))
                }
            }
        }
        
        // Sort by date (newest first)
        bookEntries.sortByDescending { it.first }
        
        if (bookEntries.isEmpty()) {
            val emptyView = TextView(this)
            emptyView.text = "No history available for this book"
            emptyView.textSize = 16f
            emptyView.setPadding(16, 16, 16, 16)
            historyContainer.addView(emptyView)
        } else {
            // Display each entry
            for (entry in bookEntries) {
                addHistoryEntryView(entry.first, entry.second)
            }
        }
    }

    private fun addHistoryEntryView(date: String, bookData: String) {
        val entryView = LayoutInflater.from(this).inflate(R.layout.book_history_item, historyContainer, false)

        val textViewDate = entryView.findViewById<TextView>(R.id.textViewHistoryDate)
        val textViewProgress = entryView.findViewById<TextView>(R.id.textViewHistoryProgress)
        val textViewStatus = entryView.findViewById<TextView>(R.id.textViewHistoryStatus)

        // Parse book data
        val parsedData = parseBookData(bookData)
        
        // Format date
        try {
            val parsedDate = dateFormat.parse(date)
            textViewDate.text = displayDateFormat.format(parsedDate!!)
        } catch (e: Exception) {
            textViewDate.text = date
        }
        
        // Set progress
        val currentPage = parsedData["currentPage"] ?: "0"
        val totalPages = parsedData["pages"] ?: "0"
        
        if (totalPages != "0") {
            try {
                val current = currentPage.toInt()
                val total = totalPages.toInt()
                val percentage = if (total > 0) (current.toFloat() / total * 100).toInt() else 0
                textViewProgress.text = "$currentPage/$totalPages pages ($percentage%)"
            } catch (e: Exception) {
                textViewProgress.text = "$currentPage/$totalPages pages"
            }
        } else {
            textViewProgress.text = "Current page: $currentPage"
        }
        
        // Set status
        val status = parsedData["status"] ?: "Unknown"
        textViewStatus.text = "Status: $status"

        historyContainer.addView(entryView)
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