package com.example.notes_app.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class PreferencesManager(context: Context) {

    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        PREF_NAME, Context.MODE_PRIVATE
    )
    
    // User preferences
    fun getUserPreferences(): UserPreferences {
        return UserPreferences(
            streakStartDate = sharedPreferences.getString(KEY_STREAK_START_DATE, "") ?: "",
            lastReadDate = sharedPreferences.getString(KEY_LAST_READ_DATE, "") ?: "",
            currentStreak = sharedPreferences.getInt(KEY_CURRENT_STREAK, 0),
            longestStreak = sharedPreferences.getInt(KEY_LONGEST_STREAK, 0),
            theme = sharedPreferences.getString(KEY_THEME, "light") ?: "light",
            defaultReadingGoal = sharedPreferences.getInt(KEY_DEFAULT_READING_GOAL, 20)
        )
    }
    
    fun saveUserPreferences(preferences: UserPreferences) {
        sharedPreferences.edit {
            putString(KEY_STREAK_START_DATE, preferences.streakStartDate)
            putString(KEY_LAST_READ_DATE, preferences.lastReadDate)
            putInt(KEY_CURRENT_STREAK, preferences.currentStreak)
            putInt(KEY_LONGEST_STREAK, preferences.longestStreak)
            putString(KEY_THEME, preferences.theme)
            putInt(KEY_DEFAULT_READING_GOAL, preferences.defaultReadingGoal)
        }
    }
    
    // Streak tracking
    fun updateReadingStreak(readDate: String) {
        val lastReadDate = sharedPreferences.getString(KEY_LAST_READ_DATE, "") ?: ""
        val currentStreak = sharedPreferences.getInt(KEY_CURRENT_STREAK, 0)
        
        // If this is a new streak or continuation of existing streak
        if (lastReadDate.isEmpty() || isNextDay(lastReadDate, readDate)) {
            val newStreak = currentStreak + 1
            val longestStreak = sharedPreferences.getInt(KEY_LONGEST_STREAK, 0)
            
            sharedPreferences.edit {
                putString(KEY_LAST_READ_DATE, readDate)
                putInt(KEY_CURRENT_STREAK, newStreak)
                if (newStreak > longestStreak) {
                    putInt(KEY_LONGEST_STREAK, newStreak)
                }
                if (currentStreak == 0) {
                    putString(KEY_STREAK_START_DATE, readDate)
                }
            }
        } else if (readDate != lastReadDate) {
            // If streak is broken, reset it
            sharedPreferences.edit {
                putString(KEY_LAST_READ_DATE, readDate)
                putInt(KEY_CURRENT_STREAK, 1)
                putString(KEY_STREAK_START_DATE, readDate)
            }
        }
    }
    
    // Helper method to check if a date is the next day
    private fun isNextDay(previousDate: String, currentDate: String): Boolean {
        // Simple implementation. In a real app, use a proper date library
        val prevParts = previousDate.split("-").map { it.toInt() }
        val currParts = currentDate.split("-").map { it.toInt() }
        
        if (prevParts.size != 3 || currParts.size != 3) return false
        
        // Check if current date is one day after previous date
        // This is simplified and doesn't account for month/year boundaries
        val prevDay = prevParts[2]
        val currDay = currParts[2]
        val prevMonth = prevParts[1]
        val currMonth = currParts[1]
        val prevYear = prevParts[0]
        val currYear = currParts[0]
        
        return when {
            // Same day
            prevYear == currYear && prevMonth == currMonth && prevDay == currDay -> false
            // Next day in same month
            prevYear == currYear && prevMonth == currMonth && currDay - prevDay == 1 -> true
            // First day of next month
            prevYear == currYear && currMonth - prevMonth == 1 && prevDay == daysInMonth(prevMonth, prevYear) && currDay == 1 -> true
            // First day of next year
            currYear - prevYear == 1 && prevMonth == 12 && currMonth == 1 && prevDay == 31 && currDay == 1 -> true
            // Otherwise, not consecutive
            else -> false
        }
    }
    
    // Helper to get days in month
    private fun daysInMonth(month: Int, year: Int): Int {
        return when (month) {
            1, 3, 5, 7, 8, 10, 12 -> 31
            4, 6, 9, 11 -> 30
            2 -> if (isLeapYear(year)) 29 else 28
            else -> 0
        }
    }
    
    // Helper to check leap year
    private fun isLeapYear(year: Int): Boolean {
        return year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)
    }
    
    companion object {
        private const val PREF_NAME = "reading_tracker_prefs"
        
        private const val KEY_STREAK_START_DATE = "streak_start_date"
        private const val KEY_LAST_READ_DATE = "last_read_date"
        private const val KEY_CURRENT_STREAK = "current_streak"
        private const val KEY_LONGEST_STREAK = "longest_streak"
        private const val KEY_THEME = "theme"
        private const val KEY_DEFAULT_READING_GOAL = "default_reading_goal"
        
        @Volatile
        private var INSTANCE: PreferencesManager? = null
        
        fun getInstance(context: Context): PreferencesManager {
            return INSTANCE ?: synchronized(this) {
                val instance = PreferencesManager(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
} 