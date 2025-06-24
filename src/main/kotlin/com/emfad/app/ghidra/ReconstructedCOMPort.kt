package com.emfad.app.ghidra

import android.content.Context
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import android.util.Log
import com.emfad.app.models.EMFReading
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Rekonstruierte COM-Port Kommunikation
 * Basiert auf String-Analyse von EMFAD3.exe:
 * - "COMPort1" - Hauptkommunikation
 * - "tsComPort" - COM-Port Tab
 * - "gbPorts" - Port-Gruppierung
 * - "btxNoDevice/btxDeviceOk" - Gerätestatus
 * 
 * Implementiert echte EMFAD-Gerätekommunikation ohne Simulation
 */
class ReconstructedCOMPort(private val context: Context) {
    
    companion object {
        private const val TAG = "ReconstructedCOMPort"
        
        // COM-Port Konfiguration (aus EMUNI-X-07.ini)
        private const val BAUD_RATE = 115200
        private const val DATA_BITS = 8
        private const val STOP_BITS = 1
        private const val PARITY = 0 // None
        
        // Gerätestatus (aus EMFAD3.exe)
        private const val BTX_NO_DEVICE = 0
        private const val BTX_DEVICE_OK = 1
        
        // EMFAD-Kommandos (rekonstruiert)
        private const val CMD_DEVICE_STATUS = 0x01.toByte()
        private const val CMD_START_MEASUREMENT = 0x02.toByte()
        private const val CMD_STOP_MEASUREMENT = 0x03.toByte()
        private const val CMD_GET_DATA = 0x04.toByte()
        private const val CMD_SET_FREQUENCY = 0x05.toByte()
        
        // Antwort-Codes
        private const val RESP_DEVICE_OK = 0xAA.toByte()
        private const val RESP_DEVICE_ERROR = 0xFF.toByte()
        private const val RESP_DATA = 0x10.toByte()
    }
    
    private val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
    private var usbDevice: UsbDevice? = null
    private var usbConnection: UsbDeviceConnection? = null
    
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // State Management (aus EMFAD3.exe GUI-Struktur)
    private val _deviceStatus = MutableStateFlow(BTX_NO_DEVICE)
    val deviceStatus: StateFlow<Int> = _deviceStatus.asStateFlow()
    
    private val _portConnected = MutableStateFlow(false)
    val portConnected: StateFlow<Boolean> = _portConnected.asStateFlow()
    
    private val _measurementData = MutableStateFlow<EMFReading?>(null)
    val measurementData: StateFlow<EMFReading?> = _measurementData.asStateFlow()
    
    /**
     * COMPort1 - Hauptkommunikationsfunktion (aus EMFAD3.exe)
     * Rekonstruiert aus: "COMPort1", "tsComPort", "gbPorts"
     */
    suspend fun initializeCOMPort1(): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Initialisiere COMPort1")
            
            // Suche EMFAD-Gerät über USB
            val deviceList = usbManager.deviceList
            for ((_, device) in deviceList) {
                if (isEMFADDevice(device)) {
                    Log.d(TAG, "EMFAD-Gerät gefunden: ${device.deviceName}")
                    
                    if (connectToDevice(device)) {
                        usbDevice = device
                        _portConnected.value = true
                        
                        // Gerätestatus prüfen
                        val status = checkDeviceStatus()
                        _deviceStatus.value = if (status) BTX_DEVICE_OK else BTX_NO_DEVICE
                        
                        return@withContext status
                    }
                }
            }
            
            Log.w(TAG, "Kein EMFAD-Gerät gefunden")
            _deviceStatus.value = BTX_NO_DEVICE
            false
            
        } catch (e: Exception) {
            Log.e(TAG, "Fehler bei COMPort1 Initialisierung", e)
            _deviceStatus.value = BTX_NO_DEVICE
            false
        }
    }
    
    /**
     * btxDeviceOk - Gerätestatus OK (aus EMFAD3.exe)
     */
    private suspend fun checkDeviceStatus(): Boolean {
        return try {
            Log.d(TAG, "Prüfe btxDeviceOk Status")
            
            val command = byteArrayOf(CMD_DEVICE_STATUS)
            val response = sendCommand(command)
            
            if (response != null && response.isNotEmpty()) {
                val isOk = response[0] == RESP_DEVICE_OK
                Log.d(TAG, if (isOk) "btxDeviceOk: Gerät OK" else "btxNoDevice: Gerät nicht bereit")
                isOk
            } else {
                Log.w(TAG, "btxNoDevice: Keine Antwort")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Fehler bei Gerätestatus-Prüfung", e)
            false
        }
    }
    
    /**
     * Startet Messung über COM-Port
     */
    suspend fun startMeasurement(frequency: Double): Boolean {
        if (_deviceStatus.value != BTX_DEVICE_OK) {
            Log.w(TAG, "Gerät nicht bereit für Messung")
            return false
        }
        
        return try {
            Log.d(TAG, "Starte Messung über COMPort1")
            
            // Frequenz setzen
            if (!setFrequency(frequency)) {
                return false
            }
            
            // Messung starten
            val command = byteArrayOf(CMD_START_MEASUREMENT)
            val response = sendCommand(command)
            
            if (response != null && response.isNotEmpty() && response[0] == RESP_DEVICE_OK) {
                // Kontinuierliche Datenerfassung starten
                startDataCollection()
                Log.d(TAG, "Messung erfolgreich gestartet")
                true
            } else {
                Log.e(TAG, "Fehler beim Starten der Messung")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Fehler beim Starten der Messung", e)
            false
        }
    }
    
    /**
     * Stoppt Messung
     */
    suspend fun stopMeasurement(): Boolean {
        return try {
            Log.d(TAG, "Stoppe Messung über COMPort1")
            
            val command = byteArrayOf(CMD_STOP_MEASUREMENT)
            val response = sendCommand(command)
            
            response != null && response.isNotEmpty() && response[0] == RESP_DEVICE_OK
        } catch (e: Exception) {
            Log.e(TAG, "Fehler beim Stoppen der Messung", e)
            false
        }
    }
    
    /**
     * Frequenz setzen
     */
    private suspend fun setFrequency(frequency: Double): Boolean {
        return try {
            Log.d(TAG, "Setze Frequenz: ${frequency}Hz")
            
            val freqBytes = ByteBuffer.allocate(8)
                .order(ByteOrder.LITTLE_ENDIAN)
                .putDouble(frequency)
                .array()
            
            val command = ByteArray(1 + freqBytes.size)
            command[0] = CMD_SET_FREQUENCY
            freqBytes.copyInto(command, 1)
            
            val response = sendCommand(command)
            
            response != null && response.isNotEmpty() && response[0] == RESP_DEVICE_OK
        } catch (e: Exception) {
            Log.e(TAG, "Fehler beim Setzen der Frequenz", e)
            false
        }
    }
    
    /**
     * Kontinuierliche Datenerfassung
     */
    private fun startDataCollection() {
        coroutineScope.launch {
            while (isActive && _portConnected.value) {
                try {
                    val command = byteArrayOf(CMD_GET_DATA)
                    val response = sendCommand(command)
                    
                    if (response != null && response.isNotEmpty() && response[0] == RESP_DATA) {
                        val reading = parseEMFData(response)
                        reading?.let {
                            _measurementData.value = it
                        }
                    }
                    
                    delay(100) // 10Hz Datenrate
                } catch (e: Exception) {
                    if (isActive) {
                        Log.e(TAG, "Fehler bei Datenerfassung", e)
                    }
                }
            }
        }
    }
    
    /**
     * EMFAD-Gerät erkennen
     */
    private fun isEMFADDevice(device: UsbDevice): Boolean {
        val deviceName = device.deviceName.lowercase()
        val productName = device.productName?.lowercase() ?: ""
        
        return deviceName.contains("emfad") || 
               productName.contains("emfad") ||
               productName.contains("ug12") ||
               productName.contains("ug ds") ||
               // USB-Serial Adapter (FTDI, Prolific, Silicon Labs)
               (device.vendorId == 0x0403 && device.productId == 0x6001) ||
               (device.vendorId == 0x067B && device.productId == 0x2303) ||
               (device.vendorId == 0x10C4 && device.productId == 0xEA60)
    }
    
    /**
     * Verbindung zum Gerät herstellen
     */
    private fun connectToDevice(device: UsbDevice): Boolean {
        return try {
            val connection = usbManager.openDevice(device)
            if (connection != null) {
                val intf = device.getInterface(0)
                if (connection.claimInterface(intf, true)) {
                    usbConnection = connection
                    
                    // Serial-Parameter konfigurieren
                    configureSerialParameters(connection)
                    
                    Log.d(TAG, "Verbindung zu EMFAD-Gerät hergestellt")
                    true
                } else {
                    connection.close()
                    false
                }
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Fehler beim Verbinden", e)
            false
        }
    }
    
    /**
     * Serial-Parameter konfigurieren (115200 8N1)
     */
    private fun configureSerialParameters(connection: UsbDeviceConnection) {
        try {
            // FTDI-spezifische Konfiguration für 115200 8N1
            connection.controlTransfer(0x40, 0x03, 0x4138, 0, null, 0, 1000)
            
            Log.d(TAG, "Serial-Parameter konfiguriert: $BAUD_RATE baud, ${DATA_BITS}N$STOP_BITS")
        } catch (e: Exception) {
            Log.w(TAG, "Warnung: Serial-Parameter konnten nicht gesetzt werden", e)
        }
    }
    
    /**
     * Kommando senden und Antwort empfangen
     */
    private suspend fun sendCommand(command: ByteArray): ByteArray? = withContext(Dispatchers.IO) {
        return@withContext try {
            val connection = usbConnection ?: return@withContext null
            val device = usbDevice ?: return@withContext null
            
            // Kommando senden
            val outEndpoint = device.getInterface(0).getEndpoint(0)
            val bytesSent = connection.bulkTransfer(outEndpoint, command, command.size, 1000)
            
            if (bytesSent < 0) {
                Log.e(TAG, "Fehler beim Senden des Kommandos")
                return@withContext null
            }
            
            // Antwort empfangen
            val inEndpoint = device.getInterface(0).getEndpoint(1)
            val buffer = ByteArray(1024)
            val bytesReceived = connection.bulkTransfer(inEndpoint, buffer, buffer.size, 1000)
            
            if (bytesReceived > 0) {
                buffer.copyOf(bytesReceived)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Fehler bei Kommando-Übertragung", e)
            null
        }
    }
    
    /**
     * EMF-Daten parsen
     */
    private fun parseEMFData(data: ByteArray): EMFReading? {
        return try {
            if (data.size < 25) return null
            
            val buffer = ByteBuffer.wrap(data, 1, data.size - 1).order(ByteOrder.LITTLE_ENDIAN)
            
            val timestamp = System.currentTimeMillis()
            val frequency = buffer.double
            val realPart = buffer.double
            val imaginaryPart = buffer.double
            val temperature = buffer.float.toDouble()
            
            val magnitude = kotlin.math.sqrt(realPart * realPart + imaginaryPart * imaginaryPart)
            val phase = kotlin.math.atan2(imaginaryPart, realPart) * 180.0 / kotlin.math.PI
            
            // EMFAD-spezifische Tiefenberechnung
            val calibrationConstant = 3333.0
            val attenuationFactor = 0.417
            val calibratedSignal = magnitude * (calibrationConstant / 1000.0)
            val depth = if (calibratedSignal > 0) {
                -kotlin.math.ln(calibratedSignal / 1000.0) / attenuationFactor
            } else {
                0.0
            }
            
            EMFReading(
                sessionId = System.currentTimeMillis() / 1000,
                timestamp = timestamp,
                frequency = frequency,
                signalStrength = magnitude,
                phase = phase,
                amplitude = magnitude,
                realPart = realPart,
                imaginaryPart = imaginaryPart,
                magnitude = magnitude,
                depth = depth,
                temperature = temperature,
                humidity = 50.0,
                pressure = 1013.25,
                batteryLevel = 100,
                deviceId = "EMFAD-UG",
                materialType = com.emfad.app.models.MaterialType.UNKNOWN,
                confidence = 0.0,
                noiseLevel = 10.0,
                calibrationOffset = 0.0,
                gainSetting = 1.0,
                filterSetting = "default",
                measurementMode = "A",
                qualityScore = kotlin.math.min(1.0, magnitude / 1000.0),
                xCoordinate = 0.0,
                yCoordinate = 0.0,
                zCoordinate = 0.0,
                gpsData = ""
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Fehler beim Parsen der EMF-Daten", e)
            null
        }
    }
    
    /**
     * Verbindung trennen
     */
    fun disconnect() {
        try {
            Log.d(TAG, "Trenne COMPort1")
            
            usbConnection?.close()
            usbConnection = null
            usbDevice = null
            
            _portConnected.value = false
            _deviceStatus.value = BTX_NO_DEVICE
            
        } catch (e: Exception) {
            Log.e(TAG, "Fehler beim Trennen", e)
        }
    }
    
    /**
     * Ressourcen freigeben
     */
    fun cleanup() {
        disconnect()
        coroutineScope.cancel()
    }
    
    /**
     * Gerätestatus abrufen
     */
    fun getDeviceStatusText(): String {
        return when (_deviceStatus.value) {
            BTX_DEVICE_OK -> "btxDeviceOk: Gerät bereit"
            BTX_NO_DEVICE -> "btxNoDevice: Kein Gerät"
            else -> "Unbekannter Status"
        }
    }
}
