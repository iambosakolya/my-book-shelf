package com.example.notes_app

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class NotesManager private constructor() {

    private lateinit var sharedPreferences: SharedPreferences
    private val gson = Gson()
    private val allNotes = mutableMapOf<String, MutableList<String>>()

    companion object {
        @Volatile
        private var instance: NotesManager? = null
        private const val PREFS_NAME = "NotesPrefs"
        private const val NOTES_KEY = "notes"
        private const val TAG = "NotesManager"

        fun getInstance(): NotesManager {
            return instance ?: synchronized(this) {
                instance ?: NotesManager().also { instance = it }
            }
        }
    }

    fun initialize(context: Context) {
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        loadNotes()
        Log.d(TAG, "NotesManager initialized with ${allNotes.size} dates")
    }

    private fun loadNotes() {
        val json = sharedPreferences.getString(NOTES_KEY, null)
        if (json != null) {
            try {
                val type = object : TypeToken<Map<String, MutableList<String>>>() {}.type
                val loadedNotes = gson.fromJson<Map<String, MutableList<String>>>(json, type)
                allNotes.clear()
                for ((key, value) in loadedNotes) {
                    // Make sure we don't have duplicate notes
                    val uniqueNotes = value.distinct().toMutableList()
                    allNotes[key] = uniqueNotes
                }
                Log.d(TAG, "Loaded notes: $allNotes")
            } catch (e: Exception) {
                Log.e(TAG, "Error loading notes: ${e.message}")
                // If loading fails, start with empty notes
                allNotes.clear()
            }
        }
    }

    private fun saveNotes() {
        try {
            val json = gson.toJson(allNotes)
            sharedPreferences.edit().putString(NOTES_KEY, json).commit() // Use commit() for synchronous write
            Log.d(TAG, "Saved notes: $allNotes")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving notes: ${e.message}")
        }
    }

    // Add a new note for a specific date
    fun addNote(dateKey: String, noteText: String) {
        Log.d(TAG, "Adding note for date: $dateKey")
        if (!allNotes.containsKey(dateKey)) {
            allNotes[dateKey] = mutableListOf()
        }
        
        // Make sure we're working with the actual list in the map
        val notesList = allNotes[dateKey]!!
        
        // Check for duplicates before adding
        if (!notesList.contains(noteText)) {
            notesList.add(noteText)
            Log.d(TAG, "Note added. Total notes for $dateKey: ${notesList.size}")
            saveNotes()
        } else {
            Log.d(TAG, "Note already exists, not adding duplicate")
        }
    }

    // Get all notes for a specific date
    fun getNotesForDate(dateKey: String): List<String> {
        val result = allNotes[dateKey] ?: emptyList()
        Log.d(TAG, "Getting notes for $dateKey. Found: ${result.size}")
        return result
    }

    // Update an existing note
    fun updateNote(dateKey: String, index: Int, newText: String) {
        if (allNotes.containsKey(dateKey) && index >= 0 && index < (allNotes[dateKey]?.size ?: 0)) {
            allNotes[dateKey]?.set(index, newText)
            Log.d(TAG, "Updated note at index $index for date $dateKey")
            saveNotes()
        }
    }

    // Delete a specific note
    fun deleteNote(dateKey: String, noteText: String) {
        Log.d(TAG, "Deleting note for date $dateKey")
        val removed = allNotes[dateKey]?.remove(noteText) ?: false
        
        // Remove the date key if no notes remain
        if (allNotes[dateKey]?.isEmpty() == true) {
            allNotes.remove(dateKey)
            Log.d(TAG, "Removed date $dateKey as it has no more notes")
        }
        
        if (removed) {
            Log.d(TAG, "Note deleted successfully")
            saveNotes()
        } else {
            Log.d(TAG, "Note not found for deletion")
        }
    }

    // Get all dates that have notes
    fun getDatesWithNotes(): Set<String> {
        return allNotes.keys
    }

    // Check if a specific date has notes
    fun hasNotes(dateKey: String): Boolean {
        return allNotes.containsKey(dateKey) && allNotes[dateKey]?.isNotEmpty() == true
    }
    
    // Debug method to clear all notes (for testing)
    fun clearAllNotes() {
        allNotes.clear()
        saveNotes()
        Log.d(TAG, "All notes cleared")
    }
}