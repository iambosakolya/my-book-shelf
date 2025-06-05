package com.example.notes_app.data

import androidx.lifecycle.LiveData
import androidx.room.*
import java.util.Date
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Dao
interface BookDao {
    @Query("SELECT * FROM books ORDER BY lastReadDate DESC")
    fun getAllBooks(): LiveData<List<Book>>
    
    @Query("SELECT * FROM books WHERE id = :bookId")
    fun getBookById(bookId: Long): LiveData<Book>
    
    @Query("SELECT * FROM books WHERE id = :bookId")
    suspend fun getBookByIdDirect(bookId: Long): Book?
    
    @Query("SELECT * FROM books WHERE title LIKE :searchQuery OR author LIKE :searchQuery")
    fun searchBooks(searchQuery: String): LiveData<List<Book>>
    
    @Query("SELECT * FROM books WHERE status = :status")
    fun getBooksByStatus(status: String): LiveData<List<Book>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: Book): Long
    
    @Update
    suspend fun updateBook(book: Book)
    
    @Delete
    suspend fun deleteBook(book: Book)
    
    @Query("SELECT * FROM books WHERE lastReadDate BETWEEN :startDate AND :endDate")
    fun getBooksReadBetweenDates(startDate: Date, endDate: Date): LiveData<List<Book>>
    
    @Query("SELECT * FROM books WHERE rating >= :minRating")
    fun getBooksWithMinRating(minRating: Float): LiveData<List<Book>>
    
    @Query("SELECT COUNT(*) FROM books")
    suspend fun getBooksCount(): Int
    
    @Query("SELECT * FROM books")
    suspend fun getAllBooksDirect(): List<Book>
} 