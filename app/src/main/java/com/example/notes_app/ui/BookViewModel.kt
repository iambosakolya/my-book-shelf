package com.example.notes_app.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.*
import com.example.notes_app.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class BookViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository: BookRepository
    private val preferencesManager = PreferencesManager.getInstance(application)
    
    val allBooks: LiveData<List<Book>>
    val inProgressBooks: LiveData<List<Book>>
    val completedBooks: LiveData<List<Book>>
    val notStartedBooks: LiveData<List<Book>>
    
    private val _userPreferences = MutableLiveData<UserPreferences>()
    val userPreferences: LiveData<UserPreferences> = _userPreferences
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    
    init {
        val database = AppDatabase.getDatabase(application)
        repository = BookRepository(database.bookDao(), database.bookProgressDao())
        
        allBooks = repository.allBooks
        inProgressBooks = repository.getBooksByStatus("In Progress")
        completedBooks = repository.getBooksByStatus("Completed")
        notStartedBooks = repository.getBooksByStatus("Not Started")
        
        // Load user preferences
        _userPreferences.value = preferencesManager.getUserPreferences()
    }
    
    // Book operations
    fun addBook(
        title: String,
        author: String,
        totalPages: Int,
        currentPage: Int = 0,
        status: String = "Not Started",
        rating: Float = 0f,
        review: String = "",
        description: String = "",
        coverImageUrl: String = "",
        pdfUrl: String = ""
    ): Long {
        val now = Date()
        val book = Book(
            title = title,
            author = author,
            totalPages = totalPages,
            currentPage = currentPage,
            status = status,
            rating = rating,
            review = review,
            description = description,
            coverImageUrl = coverImageUrl,
            pdfUrl = pdfUrl,
            dateAdded = now,
            lastReadDate = now
        )
        
        var bookId: Long = 0
        
        viewModelScope.launch(Dispatchers.IO) {
            bookId = repository.insertBook(book)
            
            // Create initial progress entry
            val progress = BookProgress(
                bookId = bookId,
                currentPage = currentPage,
                status = status,
                date = now
            )
            repository.insertProgress(progress)
            
            // Update reading streak in preferences
            val dateStr = dateFormat.format(now)
            updateReadingStreak(dateStr)
        }
        
        return bookId
    }
    
    fun updateBookProgress(bookId: Long, currentPage: Int, status: String, rating: Float = 0f, review: String = "") {
        Log.d("BookViewModel", "Updating book progress for ID: $bookId, page: $currentPage, status: $status")
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Get the book directly from the database
                val database = AppDatabase.getDatabase(getApplication())
                val book = database.bookDao().getBookByIdDirect(bookId)
                
                if (book != null) {
                    Log.d("BookViewModel", "Found book: ${book.title}, current page: ${book.currentPage}")
                    
                    // Create updated book with new progress
                    val now = Date()
                    val updatedBook = book.copy(
                        currentPage = currentPage,
                        status = status,
                        lastReadDate = now,
                        rating = if (rating > 0) rating else book.rating,
                        review = if (review.isNotEmpty()) review else book.review
                    )
                    
                    // Update the book
                    database.bookDao().updateBook(updatedBook)
                    
                    // Create progress entry
                    val progress = BookProgress(
                        bookId = bookId,
                        currentPage = currentPage,
                        status = status,
                        date = now
                    )
                    
                    // Insert progress
                    database.bookProgressDao().insertProgress(progress)
                    
                    // Update reading streak in preferences
                    val dateStr = dateFormat.format(now)
                    updateReadingStreak(dateStr)
                    
                    Log.d("BookViewModel", "Book progress updated successfully")
                } else {
                    Log.e("BookViewModel", "Failed to find book with ID: $bookId")
                }
            } catch (e: Exception) {
                Log.e("BookViewModel", "Error updating book progress: ${e.message}")
                e.printStackTrace()
            }
        }
    }
    
    // New method to update book progress with a specific date
    fun updateBookProgress(bookId: Long, currentPage: Int, status: String, date: Date, rating: Float = 0f, review: String = "") {
        Log.d("BookViewModel", "Updating book progress for ID: $bookId on specific date: ${dateFormat.format(date)}")
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Get the book directly from the database
                val database = AppDatabase.getDatabase(getApplication())
                val book = database.bookDao().getBookByIdDirect(bookId)
                
                if (book != null) {
                    Log.d("BookViewModel", "Found book: ${book.title}, current page: ${book.currentPage}")
                    
                    // Create updated book with new progress and specific date
                    val updatedBook = book.copy(
                        currentPage = currentPage,
                        status = status,
                        lastReadDate = date,
                        rating = if (rating > 0) rating else book.rating,
                        review = if (review.isNotEmpty()) review else book.review
                    )
                    
                    // Update the book
                    database.bookDao().updateBook(updatedBook)
                    
                    // Create progress entry with specified date
                    val progress = BookProgress(
                        bookId = bookId,
                        currentPage = currentPage,
                        status = status,
                        date = date
                    )
                    
                    // Insert progress
                    database.bookProgressDao().insertProgress(progress)
                    
                    // Update reading streak in preferences
                    val dateStr = dateFormat.format(date)
                    updateReadingStreak(dateStr)
                    
                    Log.d("BookViewModel", "Book progress updated successfully for date: ${dateFormat.format(date)}")
                } else {
                    Log.e("BookViewModel", "Failed to find book with ID: $bookId")
                }
            } catch (e: Exception) {
                Log.e("BookViewModel", "Error updating book progress: ${e.message}")
                e.printStackTrace()
            }
        }
    }
    
    fun deleteBook(book: Book) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteBook(book)
        }
    }
    
    fun searchBooks(query: String): LiveData<List<Book>> {
        return repository.searchBooks(query)
    }
    
    // Reading history
    fun getBookHistory(bookId: Long): LiveData<List<BookProgress>> {
        return repository.getProgressForBook(bookId)
    }
    
    fun getRecentlyReadBooks(): LiveData<List<Book>> {
        // Get books read in the last 30 days
        val calendar = Calendar.getInstance()
        val endDate = calendar.time
        
        calendar.add(Calendar.DAY_OF_YEAR, -30)
        val startDate = calendar.time
        
        return repository.getBooksReadBetweenDates(startDate, endDate)
    }
    
    // Get books read between specific dates
    fun getBooksReadBetweenDates(startDate: Date, endDate: Date): LiveData<List<Book>> {
        return repository.getBooksReadBetweenDates(startDate, endDate)
    }
    
    // New methods for comprehensive book progress tracking
    
    fun getProgressForDate(date: Date): LiveData<List<BookProgress>> {
        return repository.getProgressForDate(date)
    }
    
    fun getAllProgress(): LiveData<List<BookProgress>> {
        return repository.getAllProgress()
    }
    
    suspend fun getProgressCountForBook(bookId: Long): Int {
        return repository.getProgressCountForBook(bookId)
    }
    
    suspend fun getTotalPagesReadForBook(bookId: Long): Int {
        return repository.getTotalPagesReadForBook(bookId)
    }
    
    suspend fun getDaysReadForBook(bookId: Long): Int {
        return repository.getDaysReadForBook(bookId)
    }
    
    suspend fun getDaysReadForBookInPeriod(bookId: Long, startDate: Date, endDate: Date): Int {
        return repository.getDaysReadForBookInPeriod(bookId, startDate, endDate)
    }
    
    suspend fun getBooksReadOnDate(date: Date): Int {
        return repository.getBooksReadOnDate(date)
    }
    
    // User preferences and streak
    fun updateReadingStreak(dateStr: String) {
        preferencesManager.updateReadingStreak(dateStr)
        _userPreferences.postValue(preferencesManager.getUserPreferences())
    }
    
    fun saveUserPreferences(preferences: UserPreferences) {
        preferencesManager.saveUserPreferences(preferences)
        _userPreferences.value = preferences
    }
    
    fun updateBook(
        bookId: Long,
        title: String,
        author: String,
        totalPages: Int,
        currentPage: Int,
        status: String,
        rating: Float,
        review: String,
        description: String,
        coverImageUrl: String = ""
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Get the current book
                val book = repository.getBookById(bookId).value
                if (book != null) {
                    // Create updated book with all new fields
                    val now = Date()
                    val updatedBook = book.copy(
                        title = title,
                        author = author,
                        totalPages = totalPages,
                        currentPage = currentPage,
                        status = status,
                        rating = rating,
                        review = review,
                        description = description,
                        coverImageUrl = coverImageUrl,
                        lastReadDate = now
                    )
                    
                    // Update the book
                    repository.updateBook(updatedBook)
                    
                    // Create progress entry
                    val progress = BookProgress(
                        bookId = bookId,
                        currentPage = currentPage,
                        status = status,
                        date = now,
                        note = ""
                    )
                    
                    // Insert progress
                    repository.insertProgress(progress)
                    
                    Log.d("BookViewModel", "Book updated: $title (ID: $bookId)")
                } else {
                    Log.e("BookViewModel", "Book not found with ID: $bookId")
                }
            } catch (e: Exception) {
                Log.e("BookViewModel", "Error updating book: ${e.message}")
                e.printStackTrace()
            }
        }
    }
    
    fun getBookCount(): Int {
        var count = 0
        viewModelScope.launch(Dispatchers.IO) {
            count = repository.getBooksCount()
        }
        return count
    }
} 