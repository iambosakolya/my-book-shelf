package com.example.notes_app

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RatingBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
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
                addHistoryItemView(entry)
            }
        }
    }

    private fun addHistoryItemView(entry: Pair<String, String>) {
        val historyItemView = LayoutInflater.from(this).inflate(R.layout.book_history_item, historyContainer, false)

        val dateTextView = historyItemView.findViewById<TextView>(R.id.textViewHistoryDate)
        val detailsTextView = historyItemView.findViewById<TextView>(R.id.textViewHistoryDetails)
        val reviewContainer = historyItemView.findViewById<LinearLayout>(R.id.reviewContainer)
        val ratingBar = historyItemView.findViewById<RatingBar>(R.id.ratingBar)
        val reviewTextView = historyItemView.findViewById<TextView>(R.id.textViewReview)
        val imageViewBookCover = historyItemView.findViewById<ImageView>(R.id.imageViewBookCover)

        // Set RatingBar tint programmatically
        setRatingBarColor(ratingBar)
        
        // Format the date
        val displayDate = formatDisplayDate(entry.first)
        dateTextView.text = displayDate
        
        // Parse the book data
        val bookData = parseBookData(entry.second)
        
        // Display cover image if available
        val coverImageUrl = bookData["coverImageUrl"]
        if (!coverImageUrl.isNullOrEmpty()) {
            imageViewBookCover.visibility = View.VISIBLE
            Glide.with(this)
                .load(coverImageUrl)
                .placeholder(R.drawable.ic_calendar)
                .into(imageViewBookCover)
        } else {
            imageViewBookCover.visibility = View.GONE
        }
        
        // Format the details text
        val details = buildString {
            val currentPage = bookData["currentPage"]
            val totalPages = bookData["pages"]
            val status = bookData["status"]
        
            if (!currentPage.isNullOrEmpty() && !totalPages.isNullOrEmpty()) {
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
            
            if (!status.isNullOrEmpty()) {
                append("Status: $status")
            }
        }
        detailsTextView.text = details
        
        // Show review if available
        val rating = bookData["rating"]?.toFloatOrNull() ?: 0f
        val review = bookData["review"] ?: ""
        
        if (rating > 0 || review.isNotEmpty()) {
            reviewContainer.visibility = View.VISIBLE
            ratingBar.rating = rating
            reviewTextView.text = review.ifEmpty { "No review provided" }
        } else {
            reviewContainer.visibility = View.GONE
        }

        historyContainer.addView(historyItemView)
    }

    private fun formatDisplayDate(date: String): String {
        try {
            val parsedDate = dateFormat.parse(date)
            return com.example.notes_app.data.Converters.formatDateForDisplay(parsedDate)
        } catch (e: Exception) {
            return date
        }
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
                        key.equals("Cover Image URL", ignoreCase = true) -> 
                            data["coverImageUrl"] = value
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