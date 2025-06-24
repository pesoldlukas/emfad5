package com.emfad.app.bluetooth

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

/**
 * Bluetooth Berechtigungs-Handler für EMFAD App
 * Samsung S21 Ultra optimiert - Android 12+ kompatibel
 */
class BluetoothPermissionHandler(
    private val context: Context,
    private val onPermissionsGranted: () -> Unit,
    private val onPermissionsDenied: (List<String>) -> Unit,
    private val onBluetoothEnabled: () -> Unit
) {
    
    companion object {
        private const val TAG = "BluetoothPermissionHandler"
        const val REQUEST_ENABLE_BLUETOOTH = 1001
        const val REQUEST_LOCATION_PERMISSION = 1002
    }
    
    // Benötigte Berechtigungen basierend auf Android Version
    private val requiredPermissions: Array<String> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_ADVERTISE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    } else {
        arrayOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }
    
    // Activity Result Launcher für Berechtigungen
    private var permissionLauncher: ActivityResultLauncher<Array<String>>? = null
    private var bluetoothEnableLauncher: ActivityResultLauncher<Intent>? = null
    
    /**
     * Initialisierung mit Activity
     */
    fun initializeWithActivity(activity: androidx.activity.ComponentActivity) {
        permissionLauncher = activity.registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            handlePermissionResults(permissions)
        }
        
        bluetoothEnableLauncher = activity.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                onBluetoothEnabled()
            }
        }
    }
    
    /**
     * Initialisierung mit Fragment
     */
    fun initializeWithFragment(fragment: Fragment) {
        permissionLauncher = fragment.registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            handlePermissionResults(permissions)
        }
        
        bluetoothEnableLauncher = fragment.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                onBluetoothEnabled()
            }
        }
    }
    
    /**
     * Alle Bluetooth-Berechtigungen prüfen und anfordern
     */
    fun checkAndRequestPermissions() {
        val missingPermissions = getMissingPermissions()
        
        if (missingPermissions.isEmpty()) {
            // Alle Berechtigungen vorhanden, Bluetooth-Status prüfen
            checkBluetoothEnabled()
        } else {
            // Fehlende Berechtigungen anfordern
            requestPermissions(missingPermissions)
        }
    }
    
    /**
     * Fehlende Berechtigungen ermitteln
     */
    private fun getMissingPermissions(): List<String> {
        return requiredPermissions.filter { permission ->
            ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED
        }
    }
    
    /**
     * Berechtigungen anfordern
     */
    private fun requestPermissions(permissions: List<String>) {
        permissionLauncher?.launch(permissions.toTypedArray())
            ?: onPermissionsDenied(permissions)
    }
    
    /**
     * Berechtigungsergebnisse verarbeiten
     */
    private fun handlePermissionResults(permissions: Map<String, Boolean>) {
        val deniedPermissions = permissions.filter { !it.value }.keys.toList()
        
        if (deniedPermissions.isEmpty()) {
            // Alle Berechtigungen gewährt
            checkBluetoothEnabled()
        } else {
            // Einige Berechtigungen verweigert
            onPermissionsDenied(deniedPermissions)
        }
    }
    
    /**
     * Bluetooth-Status prüfen und aktivieren
     */
    private fun checkBluetoothEnabled() {
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        
        if (bluetoothAdapter == null) {
            onPermissionsDenied(listOf("Bluetooth nicht unterstützt"))
            return
        }
        
        if (!bluetoothAdapter.isEnabled) {
            requestBluetoothEnable()
        } else {
            onPermissionsGranted()
        }
    }
    
    /**
     * Bluetooth-Aktivierung anfordern
     */
    private fun requestBluetoothEnable() {
        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        bluetoothEnableLauncher?.launch(enableBtIntent)
    }
    
    /**
     * Prüfen ob alle Berechtigungen vorhanden sind
     */
    fun hasAllPermissions(): Boolean {
        return getMissingPermissions().isEmpty()
    }
    
    /**
     * Prüfen ob Bluetooth aktiviert ist
     */
    fun isBluetoothEnabled(): Boolean {
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        return bluetoothAdapter?.isEnabled == true
    }
    
    /**
     * Prüfen ob spezifische Berechtigung vorhanden ist
     */
    fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Prüfen ob Berechtigung dauerhaft verweigert wurde
     */
    fun isPermissionPermanentlyDenied(permission: String): Boolean {
        return if (context is Activity) {
            !ActivityCompat.shouldShowRequestPermissionRationale(context, permission) &&
            ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED
        } else {
            false
        }
    }
    
    /**
     * App-Einstellungen öffnen
     */
    fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = android.net.Uri.fromParts("package", context.packageName, null)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
    
    /**
     * Bluetooth-Einstellungen öffnen
     */
    fun openBluetoothSettings() {
        val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
    
    /**
     * Standort-Einstellungen öffnen
     */
    fun openLocationSettings() {
        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
    
    /**
     * Berechtigungsstatus-Bericht erstellen
     */
    fun getPermissionStatusReport(): Map<String, Boolean> {
        return requiredPermissions.associateWith { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    /**
     * Benutzerfreundliche Berechtigungsnamen
     */
    fun getPermissionDisplayName(permission: String): String {
        return when (permission) {
            Manifest.permission.BLUETOOTH -> "Bluetooth"
            Manifest.permission.BLUETOOTH_ADMIN -> "Bluetooth Administration"
            Manifest.permission.BLUETOOTH_SCAN -> "Bluetooth Scannen"
            Manifest.permission.BLUETOOTH_CONNECT -> "Bluetooth Verbindung"
            Manifest.permission.BLUETOOTH_ADVERTISE -> "Bluetooth Werbung"
            Manifest.permission.ACCESS_FINE_LOCATION -> "Genauer Standort"
            Manifest.permission.ACCESS_COARSE_LOCATION -> "Ungefährer Standort"
            else -> permission
        }
    }
    
    /**
     * Berechtigungserklärung für Benutzer
     */
    fun getPermissionExplanation(permission: String): String {
        return when (permission) {
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT -> 
                "Benötigt für die Kommunikation mit dem EMFAD Messgerät"
            
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.BLUETOOTH_ADVERTISE -> 
                "Benötigt für erweiterte Bluetooth-Funktionen"
            
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION -> 
                "Benötigt für Bluetooth Low Energy Geräte-Erkennung (Android Anforderung)"
            
            else -> "Benötigt für die App-Funktionalität"
        }
    }
    
    /**
     * Kritische Berechtigungen identifizieren
     */
    fun getCriticalPermissions(): List<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            listOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        } else {
            listOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }
    }
    
    /**
     * Ressourcen freigeben
     */
    fun cleanup() {
        permissionLauncher = null
        bluetoothEnableLauncher = null
    }
}
