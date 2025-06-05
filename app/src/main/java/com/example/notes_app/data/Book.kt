package com.example.notes_app.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "books")
data class Book(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val title: String,
    val author: String,
    val totalPages: Int,
    val currentPage: Int,
    val status: String, // "Not Started", "In Progress", "Completed"
    val rating: Float,
    val review: String,
    val description: String,
    val coverImageUrl: String,
    val pdfUrl: String,
    
    val dateAdded: Date,
    val lastReadDate: Date
) 