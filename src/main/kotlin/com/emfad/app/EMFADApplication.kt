package com.emfad.app

import android.app.Application
import android.content.Context
import android.os.StrictMode
import dagger.hilt.android.HiltAndroidApp

import timber.log.Timber

/**
 * EMFAD® Application Class
 * Initialisiert Hilt DI, Timber Logging
 * Optimiert für Samsung S21 Ultra Performance
 */

@HiltAndroidApp
class EMFADApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Timber Logging initialisieren
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        // StrictMode für Debug-Builds
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .detectNetwork()
                    .penaltyLog()
                    .build()
            )
            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder()
                    .detectLeakedSqlLiteObjects()
                    .detectLeakedClosableObjects()
                    .penaltyLog()
                    .build()
            )
        }

        // OSMDroid Konfiguration
        try {
            val ctx: Context = applicationContext
            org.osmdroid.config.Configuration.getInstance().load(ctx, getSharedPreferences("osmdroid", MODE_PRIVATE))
            org.osmdroid.config.Configuration.getInstance().userAgentValue = "EMFAD_Android_App"
        } catch (e: Exception) {
            Timber.e(e, "Fehler bei OSMDroid Konfiguration")
        }

        Timber.d("EMFAD Application gestartet auf Samsung S21 Ultra")
    }
}