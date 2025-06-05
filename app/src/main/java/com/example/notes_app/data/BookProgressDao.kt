package com.example.notes_app.data

import androidx.lifecycle.LiveData
import androidx.room.*
import java.util.Date

@Dao
interface BookProgressDao {
    @Query("SELECT * FROM book_progress WHERE bookId = :bookId ORDER BY date DESC")
    fun getProgressForBook(bookId: Long): LiveData<List<BookProgress>>
    
    @Query("SELECT * FROM book_progress ORDER BY date DESC")
    fun getAllProgress(): LiveData<List<BookProgress>>
    
    @Query("SELECT * FROM book_progress WHERE date BETWEEN :startDate AND :endDate")
    fun getProgressBetweenDates(startDate: Date, endDate: Date): LiveData<List<BookProgress>>
    
    @Insert
    suspend fun insertProgress(progress: BookProgress): Long
    
    @Update
    suspend fun updateProgress(progress: BookProgress)
    
    @Delete
    suspend fun deleteProgress(progress: BookProgress)
    
    @Query("SELECT * FROM book_progress WHERE bookId = :bookId ORDER BY date DESC LIMIT 1")
    suspend fun getLatestProgressForBook(bookId: Long): BookProgress?
    
    @Query("SELECT DISTINCT date FROM book_progress ORDER BY date DESC")
    fun getAllReadingDates(): LiveData<List<Date>>
    
    @Query("SELECT * FROM book_progress WHERE date = :date ORDER BY bookId")
    fun getProgressForDate(date: Date): LiveData<List<BookProgress>>
    
    @Query("SELECT COUNT(*) FROM book_progress WHERE bookId = :bookId")
    suspend fun getProgressCountForBook(bookId: Long): Int
    
    @Query("SELECT SUM(currentPage - COALESCE((SELECT MAX(p2.currentPage) FROM book_progress p2 WHERE p2.bookId = book_progress.bookId AND p2.date < book_progress.date), 0)) FROM book_progress WHERE bookId = :bookId")
    suspend fun getTotalPagesReadForBook(bookId: Long): Int
    
    @Query("SELECT COUNT(DISTINCT date) FROM book_progress WHERE bookId = :bookId")
    suspend fun getDaysReadForBook(bookId: Long): Int
    
    @Query("SELECT COUNT(DISTINCT date) FROM book_progress WHERE bookId = :bookId AND date BETWEEN :startDate AND :endDate")
    suspend fun getDaysReadForBookInPeriod(bookId: Long, startDate: Date, endDate: Date): Int
    
    @Query("SELECT COUNT(DISTINCT bookId) FROM book_progress WHERE date = :date")
    suspend fun getBooksReadOnDate(date: Date): Int
    
    @Query("DELETE FROM book_progress")
    suspend fun deleteAllProgress()
} 