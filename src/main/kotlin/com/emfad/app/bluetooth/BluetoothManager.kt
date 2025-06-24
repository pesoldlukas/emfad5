package com.emfad.app.bluetooth

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.util.Log
import com.emfad.app.models.ConnectionState
import com.emfad.app.models.MeasurementResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import no.nordicsemi.android.ble.BleManager
import no.nordicsemi.android.ble.data.Data
import java.util.*

/**
 * EMFAD Bluetooth Manager für UG12 DS WL Gerät
 * Basiert auf Nordic BLE Library mit allen ursprünglichen Algorithmen
 * Samsung S21 Ultra optimiert
 */
class EMFADBluetoothManager(context: Context) : BleManager(context) {
    
    companion object {
        private const val TAG = "EMFADBluetoothManager"
        
        // EMFAD UG12 DS WL Gerät UUIDs
        private val EMFAD_SERVICE_UUID = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E")
        private val EMFAD_TX_CHARACTERISTIC_UUID = UUID.fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E")
        private val EMFAD_RX_CHARACTERISTIC_UUID = UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E")
        private val CLIENT_CHARACTERISTIC_CONFIG_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
        
        // EMFAD Kommandos (erweitert basierend auf reverse engineering)
        private const val CMD_START_MEASUREMENT = 0x01.toByte()
        private const val CMD_STOP_MEASUREMENT = 0x02.toByte()
        private const val CMD_SET_FREQUENCY = 0x03.toByte()
        private const val CMD_SET_GAIN = 0x04.toByte()
        private const val CMD_CALIBRATE = 0x05.toByte()

        // Neue Kommandos aus EXE-Analyse
        private const val CMD_TAR_EMF = 0x0B.toByte()        // TAR-EMF Protokoll
        private const val CMD_UT_EMF = 0x0C.toByte()         // UT-EMF Protokoll
        private const val CMD_DEL_EMF = 0x0D.toByte()        // DEL-EMF Protokoll
        private const val CMD_LINE_MODE = 0x0A.toByte()      // LINE: Modus
        private const val CMD_AUTOBALANCE = 0x07.toByte()    // Autobalance (EMUNI-X-07.exe)
        private const val CMD_GET_VERSION = 0x08.toByte()    // Version abrufen

        // Kalibrierungskonstante aus EMFAD3.exe
        private const val CALIBRATION_CONSTANT = 3333.0
        private const val CMD_GET_STATUS = 0x06.toByte()
        
        // Messung Parameter
        private const val DEFAULT_FREQUENCY = 100.0 // Hz
        private const val DEFAULT_GAIN = 1.0
        private const val MAX_MEASUREMENT_BUFFER = 1000
    }
    
    // Bluetooth Charakteristiken
    private var txCharacteristic: BluetoothGattCharacteristic? = null
    private var rxCharacteristic: BluetoothGattCharacteristic? = null
    
    // State Management
    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()
    
    private val _measurementData = MutableStateFlow<MeasurementResult?>(null)
    val measurementData: StateFlow<MeasurementResult?> = _measurementData.asStateFlow()
    
    private val _deviceInfo = MutableStateFlow<Map<String, Any>>(emptyMap())
    val deviceInfo: StateFlow<Map<String, Any>> = _deviceInfo.asStateFlow()
    
    private val _batteryLevel = MutableStateFlow(0)
    val batteryLevel: StateFlow<Int> = _batteryLevel.asStateFlow()
    
    // Messung State
    private var isCalibrated = false
    private var currentFrequency = DEFAULT_FREQUENCY
    private var currentGain = DEFAULT_GAIN
    private var measurementBuffer = mutableListOf<MeasurementResult>()
    
    // Callbacks für UI
    var onMeasurementReceived: ((MeasurementResult) -> Unit)? = null
    var onDeviceStatusChanged: ((Map<String, Any>) -> Unit)? = null
    var onErrorOccurred: ((String, Exception?) -> Unit)? = null
    
    override fun getGattCallback(): BleManagerGattCallback = EMFADGattCallback()
    
    /**
     * Verbindung zu EMFAD Gerät herstellen
     */
    fun connectToEMFADDevice(device: BluetoothDevice) {
        Log.d(TAG, "Verbinde zu EMFAD Gerät: ${device.name} (${device.address})")
        _connectionState.value = ConnectionState.CONNECTING
        
        connect(device)
            .timeout(15000)
            .useAutoConnect(false)
            .retry(3, 1000)
            .enqueue()
    }
    
    /**
     * Messung starten
     */
    fun startMeasurement() {
        if (!isReady || !isCalibrated) {
            Log.w(TAG, "Gerät nicht bereit oder nicht kalibriert")
            onErrorOccurred?.invoke("Gerät nicht bereit oder nicht kalibriert", null)
            return
        }
        
        Log.d(TAG, "Starte Messung mit Frequenz: $currentFrequency Hz")
        sendCommand(CMD_START_MEASUREMENT)
    }
    
    /**
     * Messung stoppen
     */
    fun stopMeasurement() {
        Log.d(TAG, "Stoppe Messung")
        sendCommand(CMD_STOP_MEASUREMENT)
    }
    
    /**
     * Frequenz setzen
     */
    fun setMeasurementFrequency(frequency: Double) {
        if (frequency < 1.0 || frequency > 1000.0) {
            Log.w(TAG, "Ungültige Frequenz: $frequency Hz")
            return
        }
        
        currentFrequency = frequency
        Log.d(TAG, "Setze Frequenz: $frequency Hz")
        
        val frequencyBytes = ByteArray(5)
        frequencyBytes[0] = CMD_SET_FREQUENCY
        val freqInt = frequency.toInt()
        frequencyBytes[1] = (freqInt shr 24).toByte()
        frequencyBytes[2] = (freqInt shr 16).toByte()
        frequencyBytes[3] = (freqInt shr 8).toByte()
        frequencyBytes[4] = freqInt.toByte()
        
        sendCommand(frequencyBytes)
    }
    
    /**
     * Verstärkung setzen
     */
    fun setGain(gain: Double) {
        if (gain < 0.1 || gain > 10.0) {
            Log.w(TAG, "Ungültige Verstärkung: $gain")
            return
        }
        
        currentGain = gain
        Log.d(TAG, "Setze Verstärkung: $gain")
        
        val gainBytes = ByteArray(5)
        gainBytes[0] = CMD_SET_GAIN
        val gainInt = (gain * 100).toInt()
        gainBytes[1] = (gainInt shr 24).toByte()
        gainBytes[2] = (gainInt shr 16).toByte()
        gainBytes[3] = (gainInt shr 8).toByte()
        gainBytes[4] = gainInt.toByte()
        
        sendCommand(gainBytes)
    }
    
    /**
     * Kalibrierung durchführen
     */
    fun calibrateDevice() {
        Log.d(TAG, "Starte Kalibrierung")
        sendCommand(CMD_CALIBRATE)
    }
    
    /**
     * Gerätestatus abfragen
     */
    fun requestDeviceStatus() {
        Log.d(TAG, "Frage Gerätestatus ab")
        sendCommand(CMD_GET_STATUS)
    }

    /**
     * TAR-EMF Protokoll senden (aus HzEMSoft.exe)
     */
    fun sendTarEMFCommand(targetDepth: Double, signalStrength: Double) {
        Log.d(TAG, "Sende TAR-EMF Kommando: Tiefe=$targetDepth, Signal=$signalStrength")

        val tarEmfData = ByteArray(17)
        tarEmfData[0] = CMD_TAR_EMF

        // Target Depth (8 bytes, Little Endian)
        val depthBits = targetDepth.toBits()
        for (i in 0..7) {
            tarEmfData[1 + i] = (depthBits shr (i * 8)).toByte()
        }

        // Signal Strength (8 bytes, Little Endian)
        val signalBits = signalStrength.toBits()
        for (i in 0..7) {
            tarEmfData[9 + i] = (signalBits shr (i * 8)).toByte()
        }

        sendCommand(tarEmfData)
    }

    /**
     * UT-EMF Protokoll senden (Unit EMF aus HzEMSoft.exe)
     */
    fun sendUtEMFCommand(unitId: String, status: String) {
        Log.d(TAG, "Sende UT-EMF Kommando: Unit=$unitId, Status=$status")

        val utEmfString = "$unitId:$status"
        val utEmfData = ByteArray(1 + utEmfString.length)
        utEmfData[0] = CMD_UT_EMF
        utEmfString.toByteArray().copyInto(utEmfData, 1)

        sendCommand(utEmfData)
    }

    /**
     * LINE Modus aktivieren (aus HzEMSoft.exe)
     */
    fun enableLineMode(lineLength: Double, pointCount: Int) {
        Log.d(TAG, "Aktiviere LINE Modus: Länge=$lineLength, Punkte=$pointCount")

        val lineData = ByteArray(17)
        lineData[0] = CMD_LINE_MODE

        // Line Length (8 bytes)
        val lengthBits = lineLength.toBits()
        for (i in 0..7) {
            lineData[1 + i] = (lengthBits shr (i * 8)).toByte()
        }

        // Point Count (4 bytes)
        lineData[9] = (pointCount shr 24).toByte()
        lineData[10] = (pointCount shr 16).toByte()
        lineData[11] = (pointCount shr 8).toByte()
        lineData[12] = pointCount.toByte()

        sendCommand(lineData)
    }

    /**
     * Autobalance aktivieren (aus EMUNI-X-07.exe "autobalance values; version 1.0")
     */
    fun enableAutobalance(enable: Boolean) {
        Log.d(TAG, "Autobalance ${if (enable) "aktivieren" else "deaktivieren"}")

        val autobalanceData = ByteArray(2)
        autobalanceData[0] = CMD_AUTOBALANCE
        autobalanceData[1] = if (enable) 1.toByte() else 0.toByte()

        sendCommand(autobalanceData)
    }

    /**
     * Geräteversion abrufen (EMFAD TABLET 1.0)
     */
    fun requestDeviceVersion() {
        Log.d(TAG, "Frage Geräteversion ab")
        sendCommand(CMD_GET_VERSION)
    }
    
    /**
     * Kommando an Gerät senden
     */
    private fun sendCommand(command: Byte) {
        sendCommand(byteArrayOf(command))
    }
    
    private fun sendCommand(command: ByteArray) {
        txCharacteristic?.let { characteristic ->
            writeCharacteristic(characteristic, command)
                .with { _, data ->
                    Log.d(TAG, "Kommando gesendet: ${data.value?.joinToString { "%02x".format(it) }}")
                }
                .fail { _, status ->
                    Log.e(TAG, "Fehler beim Senden des Kommandos: $status")
                    onErrorOccurred?.invoke("Fehler beim Senden des Kommandos", null)
                }
                .enqueue()
        } ?: run {
            Log.e(TAG, "TX Charakteristik nicht verfügbar")
            onErrorOccurred?.invoke("TX Charakteristik nicht verfügbar", null)
        }
    }
    
    /**
     * Empfangene Daten verarbeiten (erweitert mit EMFAD-Protokollen)
     */
    private fun processReceivedData(data: ByteArray) {
        try {
            when (data[0]) {
                0x10.toByte() -> processMeasurementData(data)
                0x20.toByte() -> processStatusData(data)
                0x30.toByte() -> processCalibrationResult(data)
                0x40.toByte() -> processBatteryData(data)

                // Neue EMFAD-Protokolle
                CMD_TAR_EMF -> processTarEMFResponse(data)
                CMD_UT_EMF -> processUtEMFResponse(data)
                CMD_LINE_MODE -> processLineResponse(data)
                CMD_AUTOBALANCE -> processAutobalanceResponse(data)
                CMD_GET_VERSION -> processVersionResponse(data)

                else -> Log.w(TAG, "Unbekannter Datentyp: ${data[0]}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Fehler beim Verarbeiten der Daten", e)
            onErrorOccurred?.invoke("Fehler beim Verarbeiten der Daten", e)
        }
    }
    
    /**
     * Messdaten verarbeiten (erweitert mit EMFAD-Kalibrierung)
     */
    private fun processMeasurementData(data: ByteArray) {
        if (data.size < 17) {
            Log.w(TAG, "Unvollständige Messdaten")
            return
        }

        // Daten extrahieren (Little Endian)
        val timestamp = System.currentTimeMillis()
        val frequency = bytesToDouble(data, 1)
        val rawSignalStrength = bytesToDouble(data, 9)
        val depth = if (data.size >= 25) bytesToDouble(data, 17) else 0.0
        val temperature = if (data.size >= 33) bytesToDouble(data, 25) else 25.0

        // EMFAD-Kalibrierung anwenden (Konstante 3333 aus EMFAD3.exe)
        val calibratedSignal = applyCalibratedSignalStrength(rawSignalStrength, temperature)

        // Tiefenberechnung mit EMFAD-Algorithmus
        val calculatedDepth = calculateEMFADDepth(calibratedSignal)

        val measurement = MeasurementResult(
            timestamp = timestamp,
            frequency = frequency,
            signalStrength = calibratedSignal,
            depth = calculatedDepth,
            temperature = temperature
        )

        // Messung validieren
        if (isValidMeasurement(measurement)) {
            _measurementData.value = measurement
            onMeasurementReceived?.invoke(measurement)

            // Buffer Management
            measurementBuffer.add(measurement)
            if (measurementBuffer.size > MAX_MEASUREMENT_BUFFER) {
                measurementBuffer.removeAt(0)
            }

            Log.d(TAG, "EMFAD Messung: Signal=${calibratedSignal}, Tiefe=${calculatedDepth}, Temp=${temperature}")
        } else {
            Log.w(TAG, "Ungültige Messung verworfen")
        }
    }

    /**
     * EMFAD-Kalibrierung anwenden (aus EMFAD3.exe rekonstruiert)
     */
    private fun applyCalibratedSignalStrength(rawSignal: Double, temperature: Double): Double {
        // Kalibrierungskonstante 3333 aus EMFAD3.exe
        val calibrationFactor = CALIBRATION_CONSTANT / 1000.0

        // Temperaturkompensation (aus EMUNI-X-07.exe autobalance)
        val referenceTemp = 25.0
        val tempCoeff = 0.002 // 0.2% pro Grad
        val tempCompensation = 1.0 + (temperature - referenceTemp) * tempCoeff

        return rawSignal * calibrationFactor * tempCompensation
    }

    /**
     * EMFAD-Tiefenberechnung (aus EXE-Analyse rekonstruiert)
     */
    private fun calculateEMFADDepth(calibratedSignal: Double): Double {
        if (calibratedSignal <= 0) return 0.0

        // Dämpfungsfaktor aus EMFAD3.exe
        val attenuationFactor = 0.417

        // Logarithmische Tiefenberechnung
        return -kotlin.math.ln(calibratedSignal / 1000.0) / attenuationFactor
    }
    
    /**
     * Status-Daten verarbeiten
     */
    private fun processStatusData(data: ByteArray) {
        if (data.size < 9) return
        
        val status = mapOf(
            "firmware_version" to "${data[1]}.${data[2]}.${data[3]}",
            "hardware_version" to "${data[4]}.${data[5]}",
            "calibration_status" to (data[6] == 1.toByte()),
            "measurement_active" to (data[7] == 1.toByte()),
            "error_code" to data[8].toInt()
        )
        
        isCalibrated = status["calibration_status"] as Boolean
        _deviceInfo.value = status
        onDeviceStatusChanged?.invoke(status)
        
        Log.d(TAG, "Gerätestatus: $status")
    }
    
    /**
     * Kalibrierungsergebnis verarbeiten
     */
    private fun processCalibrationResult(data: ByteArray) {
        if (data.size < 2) return
        
        isCalibrated = data[1] == 1.toByte()
        Log.d(TAG, "Kalibrierung ${if (isCalibrated) "erfolgreich" else "fehlgeschlagen"}")
        
        val status = _deviceInfo.value.toMutableMap()
        status["calibration_status"] = isCalibrated
        _deviceInfo.value = status
    }
    
    /**
     * Batterie-Daten verarbeiten
     */
    private fun processBatteryData(data: ByteArray) {
        if (data.size < 2) return

        val batteryLevel = data[1].toInt() and 0xFF
        _batteryLevel.value = batteryLevel
        Log.d(TAG, "Batteriestand: $batteryLevel%")
    }

    /**
     * TAR-EMF Antwort verarbeiten
     */
    private fun processTarEMFResponse(data: ByteArray) {
        if (data.size < 17) return

        val targetDepth = bytesToDouble(data, 1)
        val signalStrength = bytesToDouble(data, 9)

        Log.d(TAG, "TAR-EMF Antwort: Zieltiefe=$targetDepth, Signal=$signalStrength")

        val status = _deviceInfo.value.toMutableMap()
        status["tar_emf_depth"] = targetDepth
        status["tar_emf_signal"] = signalStrength
        _deviceInfo.value = status
    }

    /**
     * UT-EMF Antwort verarbeiten
     */
    private fun processUtEMFResponse(data: ByteArray) {
        if (data.size < 2) return

        val response = String(data, 1, data.size - 1)
        Log.d(TAG, "UT-EMF Antwort: $response")

        val parts = response.split(":")
        if (parts.size >= 2) {
            val status = _deviceInfo.value.toMutableMap()
            status["unit_id"] = parts[0]
            status["unit_status"] = parts[1]
            _deviceInfo.value = status
        }
    }

    /**
     * LINE Modus Antwort verarbeiten
     */
    private fun processLineResponse(data: ByteArray) {
        if (data.size < 2) return

        val lineStatus = data[1] == 1.toByte()
        Log.d(TAG, "LINE Modus ${if (lineStatus) "aktiviert" else "deaktiviert"}")

        val status = _deviceInfo.value.toMutableMap()
        status["line_mode_active"] = lineStatus
        _deviceInfo.value = status
    }

    /**
     * Autobalance Antwort verarbeiten
     */
    private fun processAutobalanceResponse(data: ByteArray) {
        if (data.size < 2) return

        val autobalanceActive = data[1] == 1.toByte()
        Log.d(TAG, "Autobalance ${if (autobalanceActive) "aktiviert" else "deaktiviert"}")

        val status = _deviceInfo.value.toMutableMap()
        status["autobalance_active"] = autobalanceActive
        _deviceInfo.value = status
    }

    /**
     * Versions-Antwort verarbeiten (EMFAD TABLET 1.0)
     */
    private fun processVersionResponse(data: ByteArray) {
        if (data.size < 2) return

        val version = String(data, 1, data.size - 1)
        Log.d(TAG, "Geräteversion: $version")

        val status = _deviceInfo.value.toMutableMap()
        status["device_version"] = version
        status["device_type"] = if (version.contains("TABLET")) "EMFAD TABLET" else "EMFAD-UG"
        _deviceInfo.value = status
    }
    
    /**
     * Messung validieren (ursprünglicher Algorithmus)
     */
    private fun isValidMeasurement(measurement: MeasurementResult): Boolean {
        return measurement.signalStrength >= 0.0 &&
               measurement.signalStrength <= 1000.0 &&
               measurement.frequency > 0.0 &&
               measurement.temperature >= -40.0 &&
               measurement.temperature <= 85.0
    }
    
    /**
     * Bytes zu Double konvertieren (Little Endian)
     */
    private fun bytesToDouble(data: ByteArray, offset: Int): Double {
        if (offset + 8 > data.size) return 0.0
        
        var value = 0L
        for (i in 0..7) {
            value = value or ((data[offset + i].toLong() and 0xFF) shl (i * 8))
        }
        return Double.fromBits(value)
    }
    
    /**
     * GATT Callback Implementation
     */
    private inner class EMFADGattCallback : BleManagerGattCallback() {
        
        override fun isRequiredServiceSupported(gatt: BluetoothGatt): Boolean {
            val service = gatt.getService(EMFAD_SERVICE_UUID)
            if (service == null) {
                Log.e(TAG, "EMFAD Service nicht gefunden")
                return false
            }
            
            txCharacteristic = service.getCharacteristic(EMFAD_TX_CHARACTERISTIC_UUID)
            rxCharacteristic = service.getCharacteristic(EMFAD_RX_CHARACTERISTIC_UUID)
            
            val txSupported = txCharacteristic?.properties?.and(BluetoothGattCharacteristic.PROPERTY_WRITE) != 0
            val rxSupported = rxCharacteristic?.properties?.and(BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0
            
            Log.d(TAG, "Service Support - TX: $txSupported, RX: $rxSupported")
            return txSupported && rxSupported
        }
        
        override fun onDeviceConnected() {
            Log.d(TAG, "Gerät verbunden")
            _connectionState.value = ConnectionState.CONNECTED
        }
        
        override fun onDeviceDisconnected() {
            Log.d(TAG, "Gerät getrennt")
            _connectionState.value = ConnectionState.DISCONNECTED
            isCalibrated = false
            txCharacteristic = null
            rxCharacteristic = null
        }
        
        override fun onDeviceReady() {
            Log.d(TAG, "Gerät bereit")
            requestDeviceStatus()
        }
        
        override fun initialize() {
            rxCharacteristic?.let { characteristic ->
                setNotificationCallback(characteristic)
                    .with { _, data ->
                        data.value?.let { processReceivedData(it) }
                    }
                enableNotifications(characteristic).enqueue()
            }
        }
        
        override fun onServicesInvalidated() {
            Log.d(TAG, "Services invalidiert")
            txCharacteristic = null
            rxCharacteristic = null
        }
    }
}
