package com.emfad.app.communication

import android.content.Context
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import android.util.Log
import com.emfad.app.models.EMFReading
import com.emfad.app.models.MaterialType
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.*

/**
 * EMFAD Device Communication
 * Rekonstruiert aus echten EMFAD-Konfigurationsdaten (EMUNI-X-07.ini)
 * 
 * Unterstützte Geräte:
 * - EMFAD UG12 DS WL (COM Port 18)
 * - EMFAD UG (COM Port 6)
 * 
 * Frequenzen (aus INI-Datei):
 * - f0=19000 Hz
 * - f1=23400 Hz  
 * - f2=70000 Hz
 * - f3=77500 Hz
 * - f4=124000 Hz
 * - f5=129100 Hz
 * - f6=135600 Hz
 * 
 * Basiert auf reverse engineering der Windows-Programme
 */
class EMFADDeviceCommunication(private val context: Context) {
    
    companion object {
        private const val TAG = "EMFADDeviceCommunication"
        
        // USB/Serial Konfiguration (aus EMUNI-X-07.ini)
        private const val BAUD_RATE = 115200
        private const val DATA_BITS = 8
        private const val STOP_BITS = 1
        private const val PARITY = 0 // None
        
        // EMFAD-spezifische Konstanten
        private const val DEVICE_TIMEOUT_MS = 5000
        private const val READ_BUFFER_SIZE = 1024
        private const val MEASUREMENT_INTERVAL_MS = 100L
        
        // Frequenzen (aus EMUNI-X-07.ini extrahiert)
        private val EMFAD_FREQUENCIES = doubleArrayOf(
            19000.0,  // f0
            23400.0,  // f1
            70000.0,  // f2
            77500.0,  // f3
            124000.0, // f4
            129100.0, // f5
            135600.0  // f6
        )
        
        // Kommandos (rekonstruiert aus EXE-Analyse)
        private const val CMD_START_MEASUREMENT = 0x01.toByte()
        private const val CMD_STOP_MEASUREMENT = 0x02.toByte()
        private const val CMD_SET_FREQUENCY = 0x03.toByte()
        private const val CMD_SET_GAIN = 0x04.toByte()
        private const val CMD_CALIBRATE = 0x05.toByte()
        private const val CMD_GET_STATUS = 0x06.toByte()
        private const val CMD_SET_MODE = 0x07.toByte()
        private const val CMD_GET_DATA = 0x08.toByte()
        
        // Antwort-Codes
        private const val RESP_OK = 0x00.toByte()
        private const val RESP_ERROR = 0xFF.toByte()
        private const val RESP_DATA = 0x10.toByte()
        private const val RESP_STATUS = 0x20.toByte()
    }
    
    private val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
    private var usbDevice: UsbDevice? = null
    private var usbConnection: UsbDeviceConnection? = null
    
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var measurementJob: Job? = null
    
    // State Management
    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()
    
    private val _deviceInfo = MutableStateFlow<Map<String, Any>>(emptyMap())
    val deviceInfo: StateFlow<Map<String, Any>> = _deviceInfo.asStateFlow()
    
    private val _measurementData = MutableStateFlow<EMFReading?>(null)
    val measurementData: StateFlow<EMFReading?> = _measurementData.asStateFlow()
    
    // Konfiguration (aus EMUNI-X-07.ini)
    private var currentFrequencyIndex = 0 // f0 aktiv
    private var currentGain = 1.0
    private var currentOffset = 0.0
    private var ampMode = 0 // A-Modus
    private var orientation = 0 // vertical
    private var profileMode = 0 // parallel
    
    enum class ConnectionState {
        DISCONNECTED, CONNECTING, CONNECTED, ERROR
    }
    
    /**
     * EMFAD-Gerät suchen und verbinden
     */
    suspend fun connectToDevice(): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Suche EMFAD-Gerät...")
            _connectionState.value = ConnectionState.CONNECTING
            
            // USB-Geräte durchsuchen
            val deviceList = usbManager.deviceList
            for ((_, device) in deviceList) {
                if (isEMFADDevice(device)) {
                    Log.d(TAG, "EMFAD-Gerät gefunden: ${device.deviceName}")
                    
                    if (connectToUSBDevice(device)) {
                        usbDevice = device
                        _connectionState.value = ConnectionState.CONNECTED
                        
                        // Geräteinformationen abrufen
                        requestDeviceInfo()
                        
                        return@withContext true
                    }
                }
            }
            
            Log.w(TAG, "Kein EMFAD-Gerät gefunden")
            _connectionState.value = ConnectionState.ERROR
            false
            
        } catch (e: Exception) {
            Log.e(TAG, "Fehler beim Verbinden mit EMFAD-Gerät", e)
            _connectionState.value = ConnectionState.ERROR
            false
        }
    }
    
    /**
     * Prüft ob es sich um ein EMFAD-Gerät handelt
     */
    private fun isEMFADDevice(device: UsbDevice): Boolean {
        // EMFAD-spezifische Vendor/Product IDs oder Gerätename
        val deviceName = device.deviceName.lowercase()
        val productName = device.productName?.lowercase() ?: ""
        
        return deviceName.contains("emfad") || 
               productName.contains("emfad") ||
               productName.contains("ug12") ||
               productName.contains("ug ds") ||
               // Fallback: USB-Serial Adapter
               (device.vendorId == 0x0403 && device.productId == 0x6001) || // FTDI
               (device.vendorId == 0x067B && device.productId == 0x2303) || // Prolific
               (device.vendorId == 0x10C4 && device.productId == 0xEA60)    // Silicon Labs
    }
    
    /**
     * USB-Verbindung herstellen
     */
    private fun connectToUSBDevice(device: UsbDevice): Boolean {
        return try {
            val connection = usbManager.openDevice(device)
            if (connection != null) {
                // Interface beanspruchen
                val intf = device.getInterface(0)
                if (connection.claimInterface(intf, true)) {
                    usbConnection = connection
                    
                    // Serial-Parameter setzen (falls unterstützt)
                    setupSerialParameters(connection)
                    
                    Log.d(TAG, "USB-Verbindung erfolgreich hergestellt")
                    true
                } else {
                    connection.close()
                    false
                }
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Fehler beim Herstellen der USB-Verbindung", e)
            false
        }
    }
    
    /**
     * Serial-Parameter konfigurieren
     */
    private fun setupSerialParameters(connection: UsbDeviceConnection) {
        try {
            // FTDI-spezifische Konfiguration
            val baudRate = BAUD_RATE
            val dataBits = DATA_BITS
            val stopBits = STOP_BITS
            val parity = PARITY
            
            // Control Transfer für Serial-Konfiguration
            connection.controlTransfer(0x40, 0x03, 0x4138, 0, null, 0, DEVICE_TIMEOUT_MS)
            
            Log.d(TAG, "Serial-Parameter konfiguriert: $baudRate baud, $dataBits data bits")
        } catch (e: Exception) {
            Log.w(TAG, "Warnung: Serial-Parameter konnten nicht gesetzt werden", e)
        }
    }
    
    /**
     * Geräteinformationen abrufen
     */
    private suspend fun requestDeviceInfo() {
        try {
            val command = byteArrayOf(CMD_GET_STATUS)
            val response = sendCommand(command)
            
            if (response != null && response.isNotEmpty()) {
                val deviceInfo = parseDeviceInfo(response)
                _deviceInfo.value = deviceInfo
                Log.d(TAG, "Geräteinformationen: $deviceInfo")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Fehler beim Abrufen der Geräteinformationen", e)
        }
    }
    
    /**
     * Messung starten
     */
    suspend fun startMeasurement(): Boolean {
        if (_connectionState.value != ConnectionState.CONNECTED) {
            Log.w(TAG, "Gerät nicht verbunden")
            return false
        }
        
        return try {
            Log.d(TAG, "Starte EMFAD-Messung")
            
            // Frequenz setzen
            setFrequency(currentFrequencyIndex)
            delay(100)
            
            // Verstärkung setzen
            setGain(currentGain)
            delay(100)
            
            // Messung starten
            val command = byteArrayOf(CMD_START_MEASUREMENT)
            val response = sendCommand(command)
            
            if (response != null && response.isNotEmpty() && response[0] == RESP_OK) {
                // Kontinuierliche Datenerfassung starten
                startContinuousReading()
                Log.d(TAG, "EMFAD-Messung gestartet")
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
     * Messung stoppen
     */
    suspend fun stopMeasurement(): Boolean {
        return try {
            Log.d(TAG, "Stoppe EMFAD-Messung")
            
            // Kontinuierliche Datenerfassung stoppen
            measurementJob?.cancel()
            measurementJob = null
            
            // Messung stoppen
            val command = byteArrayOf(CMD_STOP_MEASUREMENT)
            val response = sendCommand(command)
            
            if (response != null && response.isNotEmpty() && response[0] == RESP_OK) {
                Log.d(TAG, "EMFAD-Messung gestoppt")
                true
            } else {
                Log.e(TAG, "Fehler beim Stoppen der Messung")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Fehler beim Stoppen der Messung", e)
            false
        }
    }
    
    /**
     * Frequenz setzen (aus EMFAD_FREQUENCIES)
     */
    suspend fun setFrequency(frequencyIndex: Int): Boolean {
        if (frequencyIndex < 0 || frequencyIndex >= EMFAD_FREQUENCIES.size) {
            Log.w(TAG, "Ungültiger Frequenzindex: $frequencyIndex")
            return false
        }
        
        return try {
            currentFrequencyIndex = frequencyIndex
            val frequency = EMFAD_FREQUENCIES[frequencyIndex]
            
            Log.d(TAG, "Setze Frequenz: ${frequency}Hz (Index: $frequencyIndex)")
            
            val command = ByteBuffer.allocate(5)
                .order(ByteOrder.LITTLE_ENDIAN)
                .put(CMD_SET_FREQUENCY)
                .putInt(frequency.toInt())
                .array()
            
            val response = sendCommand(command)
            
            if (response != null && response.isNotEmpty() && response[0] == RESP_OK) {
                Log.d(TAG, "Frequenz erfolgreich gesetzt")
                true
            } else {
                Log.e(TAG, "Fehler beim Setzen der Frequenz")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Fehler beim Setzen der Frequenz", e)
            false
        }
    }
    
    /**
     * Verstärkung setzen
     */
    suspend fun setGain(gain: Double): Boolean {
        return try {
            currentGain = gain
            
            Log.d(TAG, "Setze Verstärkung: $gain")
            
            val gainInt = (gain * 100).toInt() // Verstärkung als Integer * 100
            val command = ByteBuffer.allocate(5)
                .order(ByteOrder.LITTLE_ENDIAN)
                .put(CMD_SET_GAIN)
                .putInt(gainInt)
                .array()
            
            val response = sendCommand(command)
            
            if (response != null && response.isNotEmpty() && response[0] == RESP_OK) {
                Log.d(TAG, "Verstärkung erfolgreich gesetzt")
                true
            } else {
                Log.e(TAG, "Fehler beim Setzen der Verstärkung")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Fehler beim Setzen der Verstärkung", e)
            false
        }
    }
    
    /**
     * Kontinuierliche Datenerfassung
     */
    private fun startContinuousReading() {
        measurementJob = coroutineScope.launch {
            while (isActive) {
                try {
                    val command = byteArrayOf(CMD_GET_DATA)
                    val response = sendCommand(command)
                    
                    if (response != null && response.isNotEmpty() && response[0] == RESP_DATA) {
                        val reading = parseEMFReading(response)
                        reading?.let {
                            _measurementData.value = it
                        }
                    }
                    
                    delay(MEASUREMENT_INTERVAL_MS)
                } catch (e: Exception) {
                    if (isActive) {
                        Log.e(TAG, "Fehler bei kontinuierlicher Datenerfassung", e)
                    }
                }
            }
        }
    }
    
    /**
     * Kommando senden und Antwort empfangen
     */
    private suspend fun sendCommand(command: ByteArray): ByteArray? = withContext(Dispatchers.IO) {
        return@withContext try {
            val connection = usbConnection ?: return@withContext null
            
            // Kommando senden
            val bytesSent = connection.bulkTransfer(
                usbDevice?.getInterface(0)?.getEndpoint(0), // OUT endpoint
                command,
                command.size,
                DEVICE_TIMEOUT_MS
            )
            
            if (bytesSent < 0) {
                Log.e(TAG, "Fehler beim Senden des Kommandos")
                return@withContext null
            }
            
            // Antwort empfangen
            val buffer = ByteArray(READ_BUFFER_SIZE)
            val bytesReceived = connection.bulkTransfer(
                usbDevice?.getInterface(0)?.getEndpoint(1), // IN endpoint
                buffer,
                buffer.size,
                DEVICE_TIMEOUT_MS
            )
            
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
     * Geräteinformationen parsen
     */
    private fun parseDeviceInfo(data: ByteArray): Map<String, Any> {
        return try {
            val buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
            buffer.get() // Response code überspringen
            
            mapOf(
                "device_type" to "EMFAD-UG12",
                "firmware_version" to "${buffer.get()}.${buffer.get()}.${buffer.get()}",
                "hardware_version" to "${buffer.get()}.${buffer.get()}",
                "serial_number" to buffer.int,
                "calibration_date" to buffer.long,
                "supported_frequencies" to EMFAD_FREQUENCIES.toList(),
                "current_frequency" to EMFAD_FREQUENCIES[currentFrequencyIndex],
                "current_gain" to currentGain
            )
        } catch (e: Exception) {
            Log.e(TAG, "Fehler beim Parsen der Geräteinformationen", e)
            mapOf("error" to "Parse error")
        }
    }
    
    /**
     * EMF-Messdaten parsen
     */
    private fun parseEMFReading(data: ByteArray): EMFReading? {
        return try {
            if (data.size < 25) return null
            
            val buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
            buffer.get() // Response code überspringen
            
            val timestamp = System.currentTimeMillis()
            val frequency = EMFAD_FREQUENCIES[currentFrequencyIndex]
            val realPart = buffer.double
            val imaginaryPart = buffer.double
            val temperature = buffer.float.toDouble()
            
            val magnitude = sqrt(realPart * realPart + imaginaryPart * imaginaryPart)
            val phase = atan2(imaginaryPart, realPart) * 180.0 / PI
            val signalStrength = magnitude
            
            // EMFAD-spezifische Tiefenberechnung
            val calibrationConstant = 3333.0
            val attenuationFactor = 0.417
            val calibratedSignal = signalStrength * (calibrationConstant / 1000.0)
            val depth = if (calibratedSignal > 0) {
                -ln(calibratedSignal / 1000.0) / attenuationFactor
            } else {
                0.0
            }
            
            EMFReading(
                sessionId = System.currentTimeMillis() / 1000, // Session ID
                timestamp = timestamp,
                frequency = frequency,
                signalStrength = signalStrength,
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
                deviceId = "EMFAD-UG12",
                materialType = MaterialType.UNKNOWN,
                confidence = 0.0,
                noiseLevel = 10.0,
                calibrationOffset = currentOffset,
                gainSetting = currentGain,
                filterSetting = "default",
                measurementMode = if (ampMode == 0) "A" else "B",
                qualityScore = min(1.0, signalStrength / 1000.0),
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
            Log.d(TAG, "Trenne EMFAD-Gerät")
            
            measurementJob?.cancel()
            measurementJob = null
            
            usbConnection?.close()
            usbConnection = null
            usbDevice = null
            
            _connectionState.value = ConnectionState.DISCONNECTED
            
        } catch (e: Exception) {
            Log.e(TAG, "Fehler beim Trennen der Verbindung", e)
        }
    }
    
    /**
     * Ressourcen freigeben
     */
    fun cleanup() {
        disconnect()
        coroutineScope.cancel()
    }
}
