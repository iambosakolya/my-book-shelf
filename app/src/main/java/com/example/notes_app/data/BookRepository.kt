package com.example.notes_app.data

import android.util.Log
import androidx.lifecycle.LiveData
import java.util.Date

class BookRepository(private val bookDao: BookDao, private val progressDao: BookProgressDao) {

    // Book operations
    val allBooks: LiveData<List<Book>> = bookDao.getAllBooks()
    
    fun getBookById(bookId: Long): LiveData<Book> {
        return bookDao.getBookById(bookId)
    }
    
    fun searchBooks(query: String): LiveData<List<Book>> {
        return bookDao.searchBooks("%$query%") // Add wildcards for SQL LIKE query
    }
    
    fun getBooksByStatus(status: String): LiveData<List<Book>> {
        return bookDao.getBooksByStatus(status)
    }
    
    suspend fun insertBook(book: Book): Long {
        return bookDao.insertBook(book)
    }
    
    suspend fun updateBook(book: Book) {
        bookDao.updateBook(book)
    }
    
    suspend fun deleteBook(book: Book) {
        bookDao.deleteBook(book)
    }
    
    // Progress operations
    fun getProgressForBook(bookId: Long): LiveData<List<BookProgress>> {
        return progressDao.getProgressForBook(bookId)
    }
    
    fun getAllReadingDates(): LiveData<List<Date>> {
        return progressDao.getAllReadingDates()
    }
    
    suspend fun insertProgress(progress: BookProgress): Long {
        return progressDao.insertProgress(progress)
    }
    
    suspend fun updateLatestBookProgress(bookId: Long, currentPage: Int, status: String, date: Date) {
        // Get the book directly instead of using LiveData
        val bookDao = (bookDao as BookDao)
        val book = bookDao.getBookByIdDirect(bookId)
        
        if (book != null) {
            // Create updated book with new progress
            val updatedBook = book.copy(
                currentPage = currentPage,
                status = status,
                lastReadDate = date
            )
            
            // Update the book
            bookDao.updateBook(updatedBook)
            
            // Create progress entry
            val progress = BookProgress(
                bookId = bookId,
                currentPage = currentPage,
                status = status,
                date = date
            )
            
            // Insert progress
            progressDao.insertProgress(progress)
        } else {
            Log.e("BookRepository", "Failed to update progress: Book with ID $bookId not found")
        }
    }
    
    // Date-based queries
    fun getBooksReadBetweenDates(startDate: Date, endDate: Date): LiveData<List<Book>> {
        return bookDao.getBooksReadBetweenDates(startDate, endDate)
    }
    
    fun getProgressBetweenDates(startDate: Date, endDate: Date): LiveData<List<BookProgress>> {
        return progressDao.getProgressBetweenDates(startDate, endDate)
    }
    
    // New methods for comprehensive book progress tracking
    
    fun getProgressForDate(date: Date): LiveData<List<BookProgress>> {
        return progressDao.getProgressForDate(date)
    }
    
    fun getAllProgress(): LiveData<List<BookProgress>> {
        return progressDao.getAllProgress()
    }
    
    suspend fun getProgressCountForBook(bookId: Long): Int {
        return progressDao.getProgressCountForBook(bookId)
    }
    
    suspend fun getTotalPagesReadForBook(bookId: Long): Int {
        return progressDao.getTotalPagesReadForBook(bookId)
    }
    
    suspend fun getDaysReadForBook(bookId: Long): Int {
        return progressDao.getDaysReadForBook(bookId)
    }
    
    suspend fun getDaysReadForBookInPeriod(bookId: Long, startDate: Date, endDate: Date): Int {
        return progressDao.getDaysReadForBookInPeriod(bookId, startDate, endDate)
    }
    
    suspend fun getBooksReadOnDate(date: Date): Int {
        return progressDao.getBooksReadOnDate(date)
    }
    
    // Utility methods for book conversion
    fun convertFromLegacyBookData(
        bookData: String,
        date: String,
        legacyDateFormat: java.text.SimpleDateFormat
    ): Book {
        val parsedData = parseBookData(bookData)
        val dateObj = try {
            legacyDateFormat.parse(date) ?: Date()
        } catch (e: Exception) {
            Date()
        }
        
        return Book(
            title = parsedData["title"] ?: "",
            author = parsedData["author"] ?: "",
            totalPages = parsedData["pages"]?.toIntOrNull() ?: 0,
            currentPage = parsedData["currentPage"]?.toIntOrNull() ?: 0,
            status = parsedData["status"] ?: "Not Started",
            rating = parsedData["rating"]?.toFloatOrNull() ?: 0f,
            review = parsedData["review"] ?: "",
            description = parsedData["description"] ?: "",
            coverImageUrl = parsedData["coverimageurl"] ?: "",
            pdfUrl = parsedData["pdfurl"] ?: "",
            dateAdded = dateObj,
            lastReadDate = dateObj
        )
    }
    
    // Helper method to parse book data from legacy string format
    private fun parseBookData(note: String): Map<String, String> {
        val data = mutableMapOf<String, String>()
        
        note.split("\n").forEach { line ->
            try {
                val parts = line.split(":", limit = 2)
                if (parts.size == 2) {
                    val key = parts[0].trim().lowercase()
                    val value = parts[1].trim()
                    data[key] = value
                }
            } catch (e: Exception) {
                // Skip invalid lines
            }
        }
        return data
    }

    suspend fun getBooksCount(): Int {
        return bookDao.getBooksCount()
    }
} 