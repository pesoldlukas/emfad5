package com.emfad.app.bluetooth

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.ParcelUuid
import android.util.Log
import androidx.core.app.ActivityCompat
import com.emfad.app.models.BluetoothDevice as EMFADBluetoothDevice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.*

/**
 * EMFAD Bluetooth Scanner für UG12 DS WL Geräte
 * Samsung S21 Ultra optimiert mit allen Berechtigungen
 */
class EMFADBluetoothScanner(private val context: Context) {
    
    companion object {
        private const val TAG = "EMFADBluetoothScanner"
        private val EMFAD_SERVICE_UUID = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E")
        private const val SCAN_TIMEOUT_MS = 30000L // 30 Sekunden
    }
    
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter = bluetoothManager.adapter
    private val bluetoothLeScanner: BluetoothLeScanner? = bluetoothAdapter?.bluetoothLeScanner
    
    // State Management
    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()
    
    private val _discoveredDevices = MutableStateFlow<List<EMFADBluetoothDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<EMFADBluetoothDevice>> = _discoveredDevices.asStateFlow()
    
    private val _scanError = MutableStateFlow<String?>(null)
    val scanError: StateFlow<String?> = _scanError.asStateFlow()
    
    private val discoveredDevicesMap = mutableMapOf<String, EMFADBluetoothDevice>()
    
    // Callbacks
    var onDeviceFound: ((EMFADBluetoothDevice) -> Unit)? = null
    var onScanComplete: ((List<EMFADBluetoothDevice>) -> Unit)? = null
    var onScanError: ((String) -> Unit)? = null
    
    /**
     * Scan nach EMFAD Geräten starten
     */
    fun startScan() {
        if (!isBluetoothEnabled()) {
            val error = "Bluetooth ist nicht aktiviert"
            Log.e(TAG, error)
            _scanError.value = error
            onScanError?.invoke(error)
            return
        }
        
        if (!hasRequiredPermissions()) {
            val error = "Bluetooth-Berechtigungen fehlen"
            Log.e(TAG, error)
            _scanError.value = error
            onScanError?.invoke(error)
            return
        }
        
        if (_isScanning.value) {
            Log.w(TAG, "Scan bereits aktiv")
            return
        }
        
        Log.d(TAG, "Starte Scan nach EMFAD Geräten")
        _isScanning.value = true
        _scanError.value = null
        discoveredDevicesMap.clear()
        _discoveredDevices.value = emptyList()
        
        val scanFilters = listOf(
            ScanFilter.Builder()
                .setServiceUuid(ParcelUuid(EMFAD_SERVICE_UUID))
                .build(),
            ScanFilter.Builder()
                .setDeviceName("EMFAD")
                .build(),
            ScanFilter.Builder()
                .setDeviceName("UG12")
                .build()
        )
        
        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
            .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
            .setNumOfMatches(ScanSettings.MATCH_NUM_MAX_ADVERTISEMENT)
            .setReportDelay(0)
            .build()
        
        try {
            bluetoothLeScanner?.startScan(scanFilters, scanSettings, scanCallback)
            
            // Timeout nach 30 Sekunden
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                if (_isScanning.value) {
                    stopScan()
                }
            }, SCAN_TIMEOUT_MS)
            
        } catch (e: SecurityException) {
            Log.e(TAG, "Sicherheitsfehler beim Starten des Scans", e)
            _isScanning.value = false
            _scanError.value = "Sicherheitsfehler: ${e.message}"
            onScanError?.invoke("Sicherheitsfehler: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Fehler beim Starten des Scans", e)
            _isScanning.value = false
            _scanError.value = "Scan-Fehler: ${e.message}"
            onScanError?.invoke("Scan-Fehler: ${e.message}")
        }
    }
    
    /**
     * Scan stoppen
     */
    fun stopScan() {
        if (!_isScanning.value) {
            return
        }
        
        Log.d(TAG, "Stoppe Scan")
        _isScanning.value = false
        
        try {
            bluetoothLeScanner?.stopScan(scanCallback)
        } catch (e: SecurityException) {
            Log.e(TAG, "Sicherheitsfehler beim Stoppen des Scans", e)
        } catch (e: Exception) {
            Log.e(TAG, "Fehler beim Stoppen des Scans", e)
        }
        
        onScanComplete?.invoke(_discoveredDevices.value)
    }
    
    /**
     * Bluetooth Status prüfen
     */
    fun isBluetoothEnabled(): Boolean {
        return bluetoothAdapter?.isEnabled == true
    }
    
    /**
     * Berechtigungen prüfen
     */
    fun hasRequiredPermissions(): Boolean {
        val permissions = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        } else {
            arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }
        
        return permissions.all { permission ->
            ActivityCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    /**
     * Benötigte Berechtigungen abrufen
     */
    fun getRequiredPermissions(): Array<String> {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        } else {
            arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }
    }
    
    /**
     * Gepaarte Geräte abrufen
     */
    fun getPairedEMFADDevices(): List<EMFADBluetoothDevice> {
        if (!hasRequiredPermissions()) {
            return emptyList()
        }
        
        return try {
            bluetoothAdapter?.bondedDevices?.mapNotNull { device ->
                if (isEMFADDevice(device)) {
                    EMFADBluetoothDevice(
                        name = device.name ?: "Unbekannt",
                        address = device.address,
                        rssi = 0,
                        isConnected = false,
                        isPaired = true,
                        deviceType = "EMFAD UG12 DS WL",
                        lastSeen = System.currentTimeMillis()
                    )
                } else null
            } ?: emptyList()
        } catch (e: SecurityException) {
            Log.e(TAG, "Sicherheitsfehler beim Abrufen gepaarter Geräte", e)
            emptyList()
        }
    }
    
    /**
     * Prüfen ob Gerät ein EMFAD Gerät ist
     */
    private fun isEMFADDevice(device: BluetoothDevice): Boolean {
        return try {
            val name = device.name?.uppercase()
            name?.contains("EMFAD") == true || 
            name?.contains("UG12") == true ||
            device.uuids?.any { it.uuid == EMFAD_SERVICE_UUID } == true
        } catch (e: SecurityException) {
            false
        }
    }
    
    /**
     * Scan Callback
     */
    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            try {
                val device = result.device
                val rssi = result.rssi
                val scanRecord = result.scanRecord
                
                if (isEMFADDevice(device)) {
                    val emfadDevice = EMFADBluetoothDevice(
                        name = device.name ?: scanRecord?.deviceName ?: "EMFAD Gerät",
                        address = device.address,
                        rssi = rssi,
                        isConnected = false,
                        isPaired = device.bondState == BluetoothDevice.BOND_BONDED,
                        deviceType = determineDeviceType(scanRecord),
                        lastSeen = System.currentTimeMillis()
                    )
                    
                    // Gerät zur Liste hinzufügen oder aktualisieren
                    discoveredDevicesMap[device.address] = emfadDevice
                    _discoveredDevices.value = discoveredDevicesMap.values.toList()
                    
                    onDeviceFound?.invoke(emfadDevice)
                    
                    Log.d(TAG, "EMFAD Gerät gefunden: ${emfadDevice.name} (${emfadDevice.address}) RSSI: $rssi")
                }
            } catch (e: SecurityException) {
                Log.e(TAG, "Sicherheitsfehler beim Verarbeiten des Scan-Ergebnisses", e)
            } catch (e: Exception) {
                Log.e(TAG, "Fehler beim Verarbeiten des Scan-Ergebnisses", e)
            }
        }
        
        override fun onBatchScanResults(results: MutableList<ScanResult>) {
            Log.d(TAG, "Batch Scan Ergebnisse: ${results.size}")
            results.forEach { result ->
                onScanResult(ScanSettings.CALLBACK_TYPE_ALL_MATCHES, result)
            }
        }
        
        override fun onScanFailed(errorCode: Int) {
            val error = when (errorCode) {
                SCAN_FAILED_ALREADY_STARTED -> "Scan bereits gestartet"
                SCAN_FAILED_APPLICATION_REGISTRATION_FAILED -> "App-Registrierung fehlgeschlagen"
                SCAN_FAILED_FEATURE_UNSUPPORTED -> "Feature nicht unterstützt"
                SCAN_FAILED_INTERNAL_ERROR -> "Interner Fehler"
                SCAN_FAILED_OUT_OF_HARDWARE_RESOURCES -> "Hardware-Ressourcen erschöpft"
                SCAN_FAILED_SCANNING_TOO_FREQUENTLY -> "Zu häufiges Scannen"
                else -> "Unbekannter Fehler: $errorCode"
            }
            
            Log.e(TAG, "Scan fehlgeschlagen: $error")
            _isScanning.value = false
            _scanError.value = error
            onScanError?.invoke(error)
        }
    }
    
    /**
     * Gerätetyp bestimmen
     */
    private fun determineDeviceType(scanRecord: android.bluetooth.le.ScanRecord?): String {
        return scanRecord?.let { record ->
            val deviceName = record.deviceName?.uppercase()
            when {
                deviceName?.contains("UG12") == true -> "EMFAD UG12 DS WL"
                deviceName?.contains("EMFAD") == true -> "EMFAD Gerät"
                record.serviceUuids?.any { it.uuid == EMFAD_SERVICE_UUID } == true -> "EMFAD Kompatibel"
                else -> "Unbekannt"
            }
        } ?: "Unbekannt"
    }
    
    /**
     * Ressourcen freigeben
     */
    fun cleanup() {
        stopScan()
        discoveredDevicesMap.clear()
        _discoveredDevices.value = emptyList()
    }
}
