package com.emfad.app.services

import android.bluetooth.*
import android.content.Context
import android.util.Log
import com.emfad.app.utils.Constants
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import no.nordicsemi.android.ble.BleManager
import no.nordicsemi.android.ble.BleManagerCallbacks
import no.nordicsemi.android.ble.data.Data
import no.nordicsemi.android.ble.callback.DataReceivedCallback
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BluetoothService @Inject constructor(
    private val context: Context
) {
    private var bleManager: EMFADBleManager? = null
    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected

    private val _discoveredDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<BluetoothDevice>> = _discoveredDevices

    private val _measurementData = MutableStateFlow<FloatArray?>(null)
    val measurementData: StateFlow<FloatArray?> = _measurementData

    private var measurementCharacteristic: BluetoothGattCharacteristic? = null

    suspend fun startScan() {
        if (_isScanning.value) return

        _isScanning.value = true
        _discoveredDevices.value = emptyList()

        try {
            val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            if (bluetoothAdapter?.isEnabled == true) {
                bluetoothAdapter.startDiscovery()
            }
        } catch (e: Exception) {
            Log.e("BluetoothService", "Error starting scan", e)
            _isScanning.value = false
            throw e
        }
    }

    suspend fun stopScan() {
        if (!_isScanning.value) return

        try {
            val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            bluetoothAdapter?.cancelDiscovery()
        } catch (e: Exception) {
            Log.e("BluetoothService", "Error stopping scan", e)
            throw e
        } finally {
            _isScanning.value = false
        }
    }

    suspend fun connect(address: String) {
        try {
            val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            val device = bluetoothAdapter?.getRemoteDevice(address)
                ?: throw IllegalStateException("Device not found")

            bleManager = EMFADBleManager(context).apply {
                setConnectionCallback(object : BleManagerCallbacks {
                    override fun onDeviceConnected() {
                        Log.d("BluetoothService", "Device connected")
                    }

                    override fun onDeviceDisconnected() {
                        Log.d("BluetoothService", "Device disconnected")
                    }

                    override fun onDeviceFailedToConnect() {
                        Log.e("BluetoothService", "Failed to connect")
                    }

                    override fun onDeviceReady() {
                        Log.d("BluetoothService", "Device ready")
                    }

                    override fun onBatteryValueReceived(value: Int) {
                        // Not used in this implementation
                    }

                    override fun onServicesDiscovered(optionalServicesFound: Boolean) {
                        // Not used in this implementation
                    }
                })
            }

            bleManager?.connect(device)
                ?.retry(3, 100)
                ?.useAutoConnect(false)
                ?.timeout(10000)
                ?.enqueue()
        } catch (e: Exception) {
            Log.e("BluetoothService", "Error connecting to device", e)
            throw e
        }
    }

    suspend fun disconnect() {
        try {
            bleManager?.disconnect()
        } catch (e: Exception) {
            Log.e("BluetoothService", "Error disconnecting", e)
            throw e
        }
    }

    private inner class EMFADBleManager(context: Context) : BleManager(context) {
        override fun getGattCallback(): BleManagerGattCallback {
            return object : BleManagerGattCallback() {
                override fun isRequiredServiceSupported(gatt: BluetoothGatt): Boolean {
                    val service = gatt.getService(UUID.fromString(Constants.EMFAD_SERVICE_UUID))
                    if (service != null) {
                        measurementCharacteristic = service.getCharacteristic(
                            UUID.fromString(Constants.EMFAD_MEASUREMENT_CHARACTERISTIC_UUID)
                        )
                        return measurementCharacteristic != null
                    }
                    return false
                }

                override fun initialize() {
                    measurementCharacteristic?.let { characteristic ->
                        setNotificationCallback(characteristic).with { _, data ->
                            onMeasurementReceived(data)
                        }
                        enableNotifications(characteristic).enqueue()
                    }
                }

                override fun onServicesInvalidated() {
                    measurementCharacteristic = null
                }
            }
        }

        override fun onDeviceConnected() {
            super.onDeviceConnected()
            _isConnected.value = true
        }

        override fun onDeviceDisconnected() {
            super.onDeviceDisconnected()
            _isConnected.value = false
        }
    }

    private fun onMeasurementReceived(data: Data) {
        try {
            // Parse EMFAD measurement data
            // Assuming data format: [x, y, z, conductivity] as 4 floats (16 bytes)
            if (data.size() >= 16) {
                val bytes = data.value ?: return
                val measurements = FloatArray(4)

                for (i in 0..3) {
                    val startIndex = i * 4
                    measurements[i] = java.nio.ByteBuffer.wrap(
                        bytes.sliceArray(startIndex until startIndex + 4)
                    ).order(java.nio.ByteOrder.LITTLE_ENDIAN).float
                }

                _measurementData.value = measurements
                Log.d("BluetoothService", "Received measurement: ${measurements.contentToString()}")
            }
        } catch (e: Exception) {
            Log.e("BluetoothService", "Error parsing measurement data", e)
        }
    }

    fun getCurrentMeasurement(): FloatArray? = _measurementData.value
} 