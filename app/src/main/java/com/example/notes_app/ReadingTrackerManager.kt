package com.example.notes_app

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.*

class ReadingTrackerManager private constructor() {

    private lateinit var sharedPreferences: SharedPreferences
    private val gson = Gson()
    private val readingDays = mutableSetOf<String>()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    companion object {
        @Volatile
        private var instance: ReadingTrackerManager? = null
        private const val PREFS_NAME = "ReadingTrackerPrefs"
        private const val READING_DAYS_KEY = "reading_days"
        private const val TAG = "ReadingTrackerManager"

        fun getInstance(): ReadingTrackerManager {
            return instance ?: synchronized(this) {
                instance ?: ReadingTrackerManager().also { instance = it }
            }
        }
    }

    fun initialize(context: Context) {
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        loadReadingDays()
        Log.d(TAG, "ReadingTrackerManager initialized with ${readingDays.size} reading days")
    }

    private fun loadReadingDays() {
        val json = sharedPreferences.getString(READING_DAYS_KEY, null)
        if (json != null) {
            try {
                val type = object : TypeToken<Set<String>>() {}.type
                val loadedDays = gson.fromJson<Set<String>>(json, type)
                readingDays.clear()
                readingDays.addAll(loadedDays)
                Log.d(TAG, "Loaded reading days: $readingDays")
            } catch (e: Exception) {
                Log.e(TAG, "Error loading reading days: ${e.message}")
                readingDays.clear()
            }
        }
    }

    private fun saveReadingDays() {
        try {
            val json = gson.toJson(readingDays)
            sharedPreferences.edit().putString(READING_DAYS_KEY, json).commit()
            Log.d(TAG, "Saved reading days: $readingDays")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving reading days: ${e.message}")
        }
    }

    // Mark a day as a reading day
    fun markDayAsRead(dateKey: String) {
        Log.d(TAG, "Marking day as read: $dateKey")
        readingDays.add(dateKey)
        saveReadingDays()
    }

    // Unmark a day as read
    fun unmarkDayAsRead(dateKey: String) {
        Log.d(TAG, "Unmarking day as read: $dateKey")
        if (readingDays.remove(dateKey)) {
            saveReadingDays()
        }
    }

    // Check if a day is marked as read
    fun isDayRead(dateKey: String): Boolean {
        return readingDays.contains(dateKey)
    }

    // Calculate current reading streak
    fun getCurrentStreak(): Int {
        val today = Calendar.getInstance()
        today.set(Calendar.HOUR_OF_DAY, 0)
        today.set(Calendar.MINUTE, 0)
        today.set(Calendar.SECOND, 0)
        today.set(Calendar.MILLISECOND, 0)

        var streak = 0
        var currentDay = today.clone() as Calendar

        // Start from today and go backwards
        while (true) {
            val dateKey = dateFormat.format(currentDay.time)

            if (isDayRead(dateKey)) {
                streak++
                currentDay.add(Calendar.DAY_OF_MONTH, -1) // Check previous day
            } else {
                break // Streak is broken
            }
        }

        return streak
    }

    // Get total reading days
    fun getTotalReadingDays(): Int {
        return readingDays.size
    }

    // Get all reading days
    fun getAllReadingDays(): Set<String> {
        return readingDays.toSet()
    }
}
