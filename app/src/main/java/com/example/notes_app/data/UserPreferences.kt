package com.example.notes_app.data

data class UserPreferences(
    val streakStartDate: String = "",
    val lastReadDate: String = "",
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val theme: String = "light", // "light" or "dark"
    val defaultReadingGoal: Int = 20 // Default pages per day
) 