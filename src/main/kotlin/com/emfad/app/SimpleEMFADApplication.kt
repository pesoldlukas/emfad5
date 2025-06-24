package com.emfad.app

import android.app.Application
import android.util.Log

/**
 * EMFAD® Application - Simplified
 * Vereinfachte Version für Samsung S21 Ultra Testing
 */

class SimpleEMFADApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        Log.d("EMFAD", "EMFAD Application started on Samsung S21 Ultra")
        Log.d("EMFAD", "Package: ${packageName}")
        Log.d("EMFAD", "Version: ${BuildConfig.VERSION_NAME}")
    }
}
