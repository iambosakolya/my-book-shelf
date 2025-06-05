package com.example.notes_app.data

import androidx.room.TypeConverter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class Converters {
    companion object {
        private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        private val displayDateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        
        // Format date for display
        fun formatDateForDisplay(date: Date?): String {
            return date?.let { displayDateFormat.format(it) } ?: "Unknown"
        }
        
        // Format date for database storage
        fun formatDateForStorage(date: Date?): String {
            return date?.let { dateFormat.format(it) } ?: ""
        }
    }
    
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
} 