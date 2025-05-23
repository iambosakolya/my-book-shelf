package com.example.notes_app

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.*

class MyReviewsActivity : AppCompatActivity() {

    private lateinit var reviewsContainer: LinearLayout
    private val notesManager = NotesManager.getInstance()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val displayDateFormat = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_reviews)

        // Initialize UI Components
        reviewsContainer = findViewById(R.id.reviewsContainer)
        val btnBack = findViewById<Button>(R.id.btnBack)

        // Display all reviews
        displayAllReviews()

        // Set click listener for back button
        btnBack.setOnClickListener {
            finish() // Return to previous activity
        }
    }

    private fun displayAllReviews() {
        reviewsContainer.removeAllViews()

        // Get all dates with notes
        val datesWithNotes = notesManager.getDatesWithNotes()
        
        // Create a list to store all books with reviews
        val completedBooksWithReviews = mutableListOf<Triple<String, String, String>>() // Triple<Date, BookData, Title>
        
        // Collect all completed books with ratings or reviews
        for (date in datesWithNotes) {
            val notes = notesManager.getNotesForDate(date)
            for (note in notes) {
                val data = parseBookData(note)
                val title = data["title"] ?: ""
                val status = data["status"] ?: ""
                val rating = data["rating"] ?: ""
                val review = data["review"] ?: ""
                
                // Only include completed books with ratings or reviews
                if (status == "Completed" && (rating.isNotEmpty() || review.isNotEmpty())) {
                    completedBooksWithReviews.add(Triple(date, note, title))
                }
            }
        }
        
        // Group by book title to get only the latest review for each book
        val latestReviewsByBook = mutableMapOf<String, Triple<String, String, String>>()
        
        for (bookEntry in completedBooksWithReviews) {
            val title = bookEntry.third
            
            if (!latestReviewsByBook.containsKey(title) || 
                bookEntry.first > latestReviewsByBook[title]!!.first) {
                latestReviewsByBook[title] = bookEntry
            }
        }
        
        if (latestReviewsByBook.isEmpty()) {
            val emptyView = TextView(this)
            emptyView.text = "You haven't reviewed any completed books yet"
            emptyView.textSize = 16f
            emptyView.setPadding(16, 16, 16, 16)
            reviewsContainer.addView(emptyView)
        } else {
            // Sort by date (newest first)
            val sortedReviews = latestReviewsByBook.values.sortedByDescending { it.first }
            
            // Display each review
            for (review in sortedReviews) {
                addReviewView(review.first, review.second)
            }
        }
    }

    private fun addReviewView(date: String, bookData: String) {
        val reviewView = LayoutInflater.from(this).inflate(R.layout.book_review_item, reviewsContainer, false)

        val textViewBookTitle = reviewView.findViewById<TextView>(R.id.textViewBookTitle)
        val textViewBookAuthor = reviewView.findViewById<TextView>(R.id.textViewBookAuthor)
        val textViewRating = reviewView.findViewById<TextView>(R.id.textViewRating)
        val textViewCompletionDate = reviewView.findViewById<TextView>(R.id.textViewCompletionDate)
        val textViewReview = reviewView.findViewById<TextView>(R.id.textViewReview)

        // Parse book data
        val parsedData = parseBookData(bookData)
        
        // Set book info
        textViewBookTitle.text = parsedData["title"] ?: "Untitled"
        textViewBookAuthor.text = "By: ${parsedData["author"] ?: "Unknown"}"
        
        // Set rating
        val rating = parsedData["rating"] ?: ""
        textViewRating.text = if (rating.isNotEmpty()) {
            "$rating stars"
        } else {
            "Not rated"
        }
        
        // Set completion date
        try {
            val parsedDate = dateFormat.parse(date)
            textViewCompletionDate.text = "Completed: ${displayDateFormat.format(parsedDate!!)}"
        } catch (e: Exception) {
            textViewCompletionDate.text = "Completed: $date"
        }
        
        // Set review
        val review = parsedData["review"] ?: ""
        textViewReview.text = if (review.isNotEmpty()) {
            review
        } else {
            "No written review"
        }

        reviewsContainer.addView(reviewView)
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