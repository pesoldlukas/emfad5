package com.emfad.app.services.communication

import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import android.os.IBinder
import android.util.Log
import com.hoho.android.usbserial.driver.UsbSerialDriver
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.Serializable
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.*

/**
 * EMFAD® Device Communication Service
 * Basiert auf COM-Port-Kommunikation der originalen Windows-Software
 * Implementiert USB-Serial und Bluetooth BLE für EMFAD-UG12 DS WL Geräte
 */

@Serializable
data class EMFADDeviceInfo(
    val deviceId: String,
    val name: String,
    val type: DeviceType,
    val vendorId: Int,
    val productId: Int,
    val serialNumber: String? = null,
    val firmwareVersion: String? = null,
    val isConnected: Boolean = false
)

enum class DeviceType {
    USB_SERIAL_FTDI,
    USB_SERIAL_PROLIFIC,
    USB_SERIAL_SILICON_LABS,
    BLUETOOTH_LE,
    EMFAD_DIRECT,
    UNKNOWN
}

enum class ConnectionStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR,
    TIMEOUT
}

@Serializable
data class EMFADCommand(
    val command: String,
    val frequency: Double? = null,
    val mode: String? = null,
    val parameters: Map<String, String> = emptyMap()
) {
    fun toByteArray(): ByteArray {
        // EMFAD-Protokoll: Sync-Byte (0xAA) + Command + Parameters + Checksum
        val buffer = ByteBuffer.allocate(256).order(ByteOrder.LITTLE_ENDIAN)
        
        // Sync-Byte (aus originaler Software)
        buffer.put(0xAA.toByte())
        
        // Command-Code
        val commandCode = when (command) {
            "SET_FREQUENCY" -> 0x01
            "START_MEASUREMENT" -> 0x02
            "STOP_MEASUREMENT" -> 0x03
            "GET_STATUS" -> 0x04
            "CALIBRATE" -> 0x05
            "RESET" -> 0x06
            else -> 0x00
        }
        buffer.put(commandCode.toByte())
        
        // Frequenz (falls vorhanden)
        frequency?.let {
            buffer.putDouble(it)
        } ?: buffer.putDouble(0.0)
        
        // Modus
        val modeCode = when (mode) {
            "A" -> 0x01
            "B" -> 0x02
            "A-B" -> 0x03
            "B-A" -> 0x04
            "A&B" -> 0x05
            else -> 0x00
        }
        buffer.put(modeCode.toByte())
        
        // Parameter (vereinfacht)
        buffer.put(parameters.size.toByte())
        
        // Checksum (einfache XOR)
        val data = buffer.array().copyOf(buffer.position())
        val checksum = data.fold(0) { acc, byte -> acc xor byte.toInt() }
        buffer.put(checksum.toByte())
        
        return buffer.array().copyOf(buffer.position())
    }
}

@Serializable
data class EMFADResponse(
    val status: String,
    val data: ByteArray,
    val timestamp: Long = System.currentTimeMillis()
) {
    companion object {
        fun fromByteArray(data: ByteArray): EMFADResponse? {
            if (data.isEmpty()) return null
            
            return try {
                val buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
                
                // Sync-Byte prüfen
                val syncByte = buffer.get()
                if (syncByte != 0xAA.toByte()) return null
                
                // Status-Code
                val statusCode = buffer.get()
                val status = when (statusCode.toInt()) {
                    0x00 -> "OK"
                    0x01 -> "ERROR"
                    0x02 -> "BUSY"
                    0x03 -> "TIMEOUT"
                    else -> "UNKNOWN"
                }
                
                // Restliche Daten
                val remainingData = ByteArray(buffer.remaining())
                buffer.get(remainingData)
                
                EMFADResponse(status, remainingData)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse EMFAD response", e)
                null
            }
        }
    }
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as EMFADResponse
        return status == other.status && data.contentEquals(other.data)
    }
    
    override fun hashCode(): Int {
        var result = status.hashCode()
        result = 31 * result + data.contentHashCode()
        return result
    }
}

@AndroidEntryPoint
@Singleton
class DeviceCommunicationService @Inject constructor(
    private val context: Context
) : Service() {
    
    companion object {
        private const val TAG = "EMFADDeviceComm"
        private const val BAUD_RATE = 115200
        private const val DATA_BITS = 8
        private const val STOP_BITS = UsbSerialPort.STOPBITS_1
        private const val PARITY = UsbSerialPort.PARITY_NONE
        private const val TIMEOUT_MS = 5000
        
        // Unterstützte USB-Serial Geräte
        private val SUPPORTED_DEVICES = mapOf(
            // FTDI Devices
            Pair(0x0403, 0x6001) to DeviceType.USB_SERIAL_FTDI,
            Pair(0x0403, 0x6010) to DeviceType.USB_SERIAL_FTDI,
            Pair(0x0403, 0x6011) to DeviceType.USB_SERIAL_FTDI,
            
            // Prolific Devices
            Pair(0x067B, 0x2303) to DeviceType.USB_SERIAL_PROLIFIC,
            Pair(0x067B, 0x04BB) to DeviceType.USB_SERIAL_PROLIFIC,
            
            // Silicon Labs Devices
            Pair(0x10C4, 0xEA60) to DeviceType.USB_SERIAL_SILICON_LABS,
            Pair(0x10C4, 0xEA70) to DeviceType.USB_SERIAL_SILICON_LABS,
            
            // Direkte EMFAD-Geräte (falls verfügbar)
            Pair(0x1234, 0x5678) to DeviceType.EMFAD_DIRECT
        )
    }
    
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
    
    // State Management
    private val _connectionStatus = MutableStateFlow(ConnectionStatus.DISCONNECTED)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()
    
    private val _connectedDevice = MutableStateFlow<EMFADDeviceInfo?>(null)
    val connectedDevice: StateFlow<EMFADDeviceInfo?> = _connectedDevice.asStateFlow()
    
    private val _availableDevices = MutableStateFlow<List<EMFADDeviceInfo>>(emptyList())
    val availableDevices: StateFlow<List<EMFADDeviceInfo>> = _availableDevices.asStateFlow()
    
    private val _responses = MutableSharedFlow<EMFADResponse>()
    val responses: SharedFlow<EMFADResponse> = _responses.asSharedFlow()
    
    // USB-Serial Connection
    private var usbSerialPort: UsbSerialPort? = null
    private var usbConnection: UsbDeviceConnection? = null
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "DeviceCommunicationService created")
        startDeviceScanning()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        disconnectDevice()
        serviceScope.cancel()
        Log.d(TAG, "DeviceCommunicationService destroyed")
    }
    
    /**
     * Scannt nach verfügbaren EMFAD-Geräten
     */
    fun scanForDevices() {
        serviceScope.launch {
            try {
                val devices = mutableListOf<EMFADDeviceInfo>()
                
                // USB-Serial Geräte scannen
                val usbDevices = usbManager.deviceList.values
                for (usbDevice in usbDevices) {
                    val deviceType = SUPPORTED_DEVICES[Pair(usbDevice.vendorId, usbDevice.productId)]
                    if (deviceType != null) {
                        devices.add(
                            EMFADDeviceInfo(
                                deviceId = usbDevice.deviceName,
                                name = "${usbDevice.manufacturerName ?: "Unknown"} ${usbDevice.productName ?: "EMFAD Device"}",
                                type = deviceType,
                                vendorId = usbDevice.vendorId,
                                productId = usbDevice.productId,
                                serialNumber = usbDevice.serialNumber
                            )
                        )
                    }
                }
                
                _availableDevices.value = devices
                Log.d(TAG, "Found ${devices.size} EMFAD devices")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error scanning for devices", e)
            }
        }
    }
    
    /**
     * Verbindet mit einem EMFAD-Gerät
     */
    suspend fun connectToDevice(deviceInfo: EMFADDeviceInfo): Boolean {
        return try {
            _connectionStatus.value = ConnectionStatus.CONNECTING
            
            when (deviceInfo.type) {
                DeviceType.USB_SERIAL_FTDI,
                DeviceType.USB_SERIAL_PROLIFIC,
                DeviceType.USB_SERIAL_SILICON_LABS,
                DeviceType.EMFAD_DIRECT -> connectUSBSerial(deviceInfo)
                DeviceType.BLUETOOTH_LE -> connectBluetooth(deviceInfo)
                else -> false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to connect to device", e)
            _connectionStatus.value = ConnectionStatus.ERROR
            false
        }
    }
    
    private suspend fun connectUSBSerial(deviceInfo: EMFADDeviceInfo): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val usbDevice = usbManager.deviceList[deviceInfo.deviceId]
                    ?: return@withContext false
                
                // USB-Serial Driver finden
                val availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager)
                val driver = availableDrivers.find { it.device == usbDevice }
                    ?: return@withContext false
                
                // Verbindung öffnen
                usbConnection = usbManager.openDevice(usbDevice)
                    ?: return@withContext false
                
                usbSerialPort = driver.ports[0]
                usbSerialPort?.open(usbConnection)
                
                // Serial-Parameter konfigurieren (115200 8N1)
                usbSerialPort?.setParameters(BAUD_RATE, DATA_BITS, STOP_BITS, PARITY)
                
                // DTR und RTS setzen (wichtig für EMFAD-Geräte)
                usbSerialPort?.dtr = true
                usbSerialPort?.rts = true
                
                // Verbindung testen
                val testCommand = EMFADCommand("GET_STATUS")
                val response = sendCommandInternal(testCommand)
                
                if (response?.status == "OK") {
                    _connectionStatus.value = ConnectionStatus.CONNECTED
                    _connectedDevice.value = deviceInfo.copy(isConnected = true)
                    
                    // Response-Listener starten
                    startResponseListener()
                    
                    Log.d(TAG, "Successfully connected to USB device: ${deviceInfo.name}")
                    true
                } else {
                    disconnectDevice()
                    false
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "USB connection failed", e)
                disconnectDevice()
                false
            }
        }
    }
    
    private suspend fun connectBluetooth(deviceInfo: EMFADDeviceInfo): Boolean {
        // TODO: Bluetooth BLE Implementation
        // Wird in separatem BluetoothService implementiert
        return false
    }
    
    /**
     * Trennt die Verbindung zum Gerät
     */
    fun disconnectDevice() {
        serviceScope.launch {
            try {
                usbSerialPort?.close()
                usbConnection?.close()
                
                usbSerialPort = null
                usbConnection = null
                
                _connectionStatus.value = ConnectionStatus.DISCONNECTED
                _connectedDevice.value = null
                
                Log.d(TAG, "Device disconnected")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error disconnecting device", e)
            }
        }
    }
    
    /**
     * Sendet ein Kommando an das EMFAD-Gerät
     */
    suspend fun sendCommand(command: EMFADCommand): EMFADResponse? {
        return if (_connectionStatus.value == ConnectionStatus.CONNECTED) {
            sendCommandInternal(command)
        } else {
            Log.w(TAG, "Cannot send command: device not connected")
            null
        }
    }
    
    private suspend fun sendCommandInternal(command: EMFADCommand): EMFADResponse? {
        return withContext(Dispatchers.IO) {
            try {
                val commandBytes = command.toByteArray()
                
                // Kommando senden
                val bytesSent = usbSerialPort?.write(commandBytes, TIMEOUT_MS) ?: 0
                if (bytesSent != commandBytes.size) {
                    Log.w(TAG, "Not all bytes sent: $bytesSent/${commandBytes.size}")
                }
                
                // Antwort lesen
                val responseBuffer = ByteArray(1024)
                val bytesRead = usbSerialPort?.read(responseBuffer, TIMEOUT_MS) ?: 0
                
                if (bytesRead > 0) {
                    val responseData = responseBuffer.copyOf(bytesRead)
                    EMFADResponse.fromByteArray(responseData)
                } else {
                    null
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send command", e)
                null
            }
        }
    }
    
    private fun startResponseListener() {
        serviceScope.launch {
            val buffer = ByteArray(1024)
            
            while (_connectionStatus.value == ConnectionStatus.CONNECTED) {
                try {
                    val bytesRead = usbSerialPort?.read(buffer, 100) ?: 0
                    
                    if (bytesRead > 0) {
                        val responseData = buffer.copyOf(bytesRead)
                        val response = EMFADResponse.fromByteArray(responseData)
                        
                        response?.let {
                            _responses.emit(it)
                        }
                    }
                    
                    delay(10) // Kurze Pause um CPU zu schonen
                    
                } catch (e: Exception) {
                    Log.e(TAG, "Error in response listener", e)
                    break
                }
            }
        }
    }
    
    private fun startDeviceScanning() {
        serviceScope.launch {
            while (true) {
                scanForDevices()
                delay(5000) // Alle 5 Sekunden scannen
            }
        }
    }
}
