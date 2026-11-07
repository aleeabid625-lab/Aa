package com.example

import android.app.Application
import com.example.data.db.AppDatabase

class AbidMusicApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize Room Database
        AppDatabase.getDatabase(this)
    }
}
