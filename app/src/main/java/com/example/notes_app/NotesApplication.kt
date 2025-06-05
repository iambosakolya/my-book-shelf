package com.example.notes_app

import android.app.Application
import android.util.Log
import com.example.notes_app.data.AppDatabase
import com.example.notes_app.data.BookRepository
import com.example.notes_app.data.PreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

class NotesApplication : Application() {
    // ApplicationScope for initialization tasks
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    // Lazy initialized database instance
    val database by lazy { 
        Log.d("NotesApplication", "Initializing database instance")
        AppDatabase.getDatabase(this) 
    }
    
    // Lazy repository initialization
    val bookRepository by lazy { 
        BookRepository(
            database.bookDao(), 
            database.bookProgressDao()
        )
    }
    
    // Preferences manager
    val preferencesManager by lazy { PreferencesManager.getInstance(this) }
    
    override fun onCreate() {
        super.onCreate()
        Log.d("NotesApplication", "Application created")
        
        // Initialize the NotesManager (legacy storage)
        NotesManager.getInstance().initialize(applicationContext)
        
        // Initialize the ReadingTrackerManager
        ReadingTrackerManager.getInstance().initialize(applicationContext)
        
        // Test database access
        try {
            val dao = database.bookDao()
            Log.d("NotesApplication", "Database initialized successfully")
            
            // Run data migration in the background
            initializeDatabase()
        } catch (e: Exception) {
            Log.e("NotesApplication", "Error initializing database: ${e.message}")
            e.printStackTrace()
        }
    }
    
    private fun initializeDatabase() {
        applicationScope.launch {
            try {
                // Force access to DAO to verify database is open
                val count = database.bookDao().getBooksCount()
                Log.d("NotesApplication", "Database contains $count books")
                
                // Check if we need to migrate data from the old system
                migrateDataIfNeeded()
            } catch (e: Exception) {
                Log.e("NotesApplication", "Error initializing database: ${e.message}")
                e.printStackTrace()
            }
        }
    }
    
    private suspend fun migrateDataIfNeeded() {
        // Check if we've already migrated
        val prefs = getSharedPreferences("db_migration", MODE_PRIVATE)
        val isMigrated = prefs.getBoolean("is_data_migrated", false)
        
        if (!isMigrated) {
            try {
                // Get all books from the old system
                val notesManager = NotesManager.getInstance()
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                
                // For each date with notes
                val datesWithNotes = notesManager.getDatesWithNotes()
                var migrationCount = 0
                
                for (date in datesWithNotes) {
                    // Get all notes for this date
                    val notes = notesManager.getNotesForDate(date)
                    
                    // Process each note (book)
                    for (noteData in notes) {
                        try {
                            // Convert the old format to new Book entity
                            val book = bookRepository.convertFromLegacyBookData(
                                noteData, 
                                date, 
                                dateFormat
                            )
                            
                            // Insert into the new database
                            bookRepository.insertBook(book)
                            migrationCount++
                        } catch (e: Exception) {
                            Log.e("NotesApplication", "Failed to migrate book: $e")
                        }
                    }
                }
                
                Log.d("NotesApplication", "Data migration complete. Migrated $migrationCount books")
                
                // Mark as migrated
                prefs.edit().putBoolean("is_data_migrated", true).apply()
            } catch (e: Exception) {
                Log.e("NotesApplication", "Data migration failed: $e")
            }
        }
    }
} 