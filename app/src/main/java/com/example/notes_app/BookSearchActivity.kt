package com.example.notes_app

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.notes_app.api.BookAdapter
import com.example.notes_app.api.BookItem
import com.example.notes_app.api.GoogleBooksViewModel
import com.example.notes_app.ui.BookViewModel
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class BookSearchActivity : AppCompatActivity() {
    private lateinit var viewModel: GoogleBooksViewModel
    private lateinit var bookViewModel: BookViewModel
    private lateinit var editTextSearch: EditText
    private lateinit var btnSearch: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var textViewError: TextView
    private lateinit var recyclerViewBooks: RecyclerView
    private lateinit var btnAddManually: Button
    private lateinit var btnBack: Button
    
    private lateinit var selectedDate: String
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    
    private val bookAdapter by lazy {
        BookAdapter(
            onBookSelected = { book ->
                addBookToLibrary(book)
            },
            onBookClicked = { book ->
                showBookDetailsDialog(book)
            }
        )
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_book_search)
        
        // Get selected date from intent
        selectedDate = intent.getStringExtra("SELECTED_DATE") ?: SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        
        // Initialize ViewModels
        viewModel = ViewModelProvider(this)[GoogleBooksViewModel::class.java]
        bookViewModel = ViewModelProvider(this)[BookViewModel::class.java]
        
        // Initialize UI components
        editTextSearch = findViewById(R.id.editTextSearch)
        btnSearch = findViewById(R.id.btnSearch)
        progressBar = findViewById(R.id.progressBar)
        textViewError = findViewById(R.id.textViewError)
        recyclerViewBooks = findViewById(R.id.recyclerViewBooks)
        btnAddManually = findViewById(R.id.btnAddManually)
        btnBack = findViewById(R.id.btnBack)
        
        // Set up RecyclerView
        recyclerViewBooks.adapter = bookAdapter
        
        // Set up search functionality
        btnSearch.setOnClickListener {
            performSearch()
        }
        
        editTextSearch.setOnEditorActionListener { _, actionId, keyEvent ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH || 
                (keyEvent != null && keyEvent.keyCode == KeyEvent.KEYCODE_ENTER && keyEvent.action == KeyEvent.ACTION_DOWN)) {
                performSearch()
                return@setOnEditorActionListener true
            }
            return@setOnEditorActionListener false
        }
        
        // Set up button listeners
        btnAddManually.setOnClickListener {
            // Navigate to CreateNoteActivity to add a book manually
            val intent = Intent(this, CreateNoteActivity::class.java)
            intent.putExtra("SELECTED_DATE", selectedDate)
            startActivity(intent)
            finish()
        }
        
        btnBack.setOnClickListener {
            finish() // Return to previous activity
        }
        
        // Observe LiveData
        viewModel.searchResults.observe(this) { books ->
            bookAdapter.submitList(books)
            if (books.isEmpty() && editTextSearch.text.isNotBlank() && !viewModel.isLoading.value!!) {
                Toast.makeText(this, "No books found", Toast.LENGTH_SHORT).show()
            }
        }
        
        viewModel.isLoading.observe(this) { isLoading ->
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
        
        viewModel.errorMessage.observe(this) { errorMessage ->
            if (errorMessage != null) {
                textViewError.text = errorMessage
                textViewError.visibility = View.VISIBLE
            } else {
                textViewError.visibility = View.GONE
            }
        }
    }
    
    private fun performSearch() {
        val query = editTextSearch.text.toString().trim()
        if (query.isNotEmpty()) {
            viewModel.searchBooks(query)
            hideKeyboard()
        } else {
            Toast.makeText(this, "Please enter a search term", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun hideKeyboard() {
        val imm = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        val view = currentFocus ?: View(this)
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }
    
    private fun addBookToLibrary(book: BookItem) {
        val volumeInfo = book.volumeInfo ?: return
        
        try {
            // Get the book cover image URL (use https instead of http)
            val coverImageUrl = volumeInfo.imageLinks?.thumbnail?.replace("http:", "https:") ?: ""
            
            // Get page count as Int - handle properly based on the type
            val pageCount = volumeInfo.pageCount ?: 0
            
            // Add the book to Room database
            val bookId = bookViewModel.addBook(
                title = volumeInfo.title ?: "Unknown Title",
                author = volumeInfo.authors?.joinToString(", ") ?: "Unknown Author",
                totalPages = pageCount,
                currentPage = 0,
                status = "Not Started",
                rating = 0f,
                review = "",
                description = volumeInfo.description?.take(300) ?: "No description available",
                coverImageUrl = coverImageUrl
            )
            
            Log.d("BookSearchActivity", "Added book with ID: $bookId")
            
            // Now record that this book was read on the selected date
            // Parse the selected date
            val parsedDate = dateFormat.parse(selectedDate)
            
            if (parsedDate != null) {
                val calendar = Calendar.getInstance()
                calendar.time = parsedDate
                
                // The bookId returned by addBook might not be used correctly,
                // so we'll query the database for the book we just added
                bookViewModel.searchBooks(volumeInfo.title ?: "").observe(this) { books ->
                    if (books.isNotEmpty()) {
                        val addedBook = books.firstOrNull { it.title == volumeInfo.title }
                        if (addedBook != null) {
                            // Add a reading entry for this book on the selected date
                            bookViewModel.updateBookProgress(
                                bookId = addedBook.id,
                                currentPage = 0,
                                status = "Not Started",
                                date = calendar.time
                            )
                        }
                    }
                }
            }
            
            Toast.makeText(this, "${volumeInfo.title} added to your library", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e("BookSearchActivity", "Error adding book: ${e.message}")
            Toast.makeText(this, "Error adding book: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun showBookDetailsDialog(book: BookItem) {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_book_details)
        
        val volumeInfo = book.volumeInfo ?: return
        
        // Set book details in the dialog
        val titleTextView = dialog.findViewById<TextView>(R.id.textViewDetailTitle)
        val authorsTextView = dialog.findViewById<TextView>(R.id.textViewDetailAuthors)
        val pagesTextView = dialog.findViewById<TextView>(R.id.textViewDetailPages)
        val publisherTextView = dialog.findViewById<TextView>(R.id.textViewDetailPublisher)
        val publishDateTextView = dialog.findViewById<TextView>(R.id.textViewDetailPublishDate)
        val descriptionTextView = dialog.findViewById<TextView>(R.id.textViewDetailDescription)
        val coverImageView = dialog.findViewById<ImageView>(R.id.imageViewDetailCover)
        val closeButton = dialog.findViewById<MaterialButton>(R.id.btnClose)
        val addToLibraryButton = dialog.findViewById<MaterialButton>(R.id.btnAddToLibrary)
        
        // Set dialog title
        dialog.findViewById<TextView>(R.id.textViewDialogTitle).text = "Book Details"
        
        // Set basic book information
        titleTextView.text = volumeInfo.title
        authorsTextView.text = "By: ${volumeInfo.authors?.joinToString(", ") ?: "Unknown Author"}"
        pagesTextView.text = "Pages: ${volumeInfo.pageCount ?: "Unknown"}"
        
        // Get language name based on language code
        val languageCode = volumeInfo.language ?: "unknown"
        val languageName = when (languageCode) {
            "en" -> "English"
            "uk" -> "Ukrainian"
            "de" -> "German"
            "fr" -> "French"
            "es" -> "Spanish"
            "it" -> "Italian"
            "pl" -> "Polish"
            "cs" -> "Czech"
            else -> languageCode
        }
        
        // Set additional details
        publisherTextView.text = "Publisher: ${volumeInfo.publisher ?: "Unknown"}"
        publishDateTextView.text = "Published: ${volumeInfo.publishedDate ?: "Unknown"} | Language: $languageName"
        descriptionTextView.text = volumeInfo.description ?: "No description available"
        
        // Load book cover image
        val imageUrl = volumeInfo.imageLinks?.thumbnail?.replace("http:", "https:")
        Glide.with(this)
            .load(imageUrl)
            .placeholder(R.drawable.ic_calendar)
            .into(coverImageView)
            
        // Set button click listeners
        closeButton.setOnClickListener {
            dialog.dismiss()
        }
        
        addToLibraryButton.setOnClickListener {
            addBookToLibrary(book)
            dialog.dismiss()
        }
        
        dialog.show()
    }
} 