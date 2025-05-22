package com.example.notes_app

import android.app.Application
import android.util.Log

class NotesApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Log.d("NotesApplication", "Application onCreate")
        
        // Initialize managers
        NotesManager.getInstance().initialize(this)
        ReadingTrackerManager.getInstance().initialize(this)
    }
} 