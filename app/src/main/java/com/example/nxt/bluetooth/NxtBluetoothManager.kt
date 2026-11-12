package com.example.nxt.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.nxt.protocol.NxtCommand
import com.example.nxt.protocol.NxtResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

sealed interface ConnectionState {
    object Disconnected : ConnectionState
    data class Scanning(val foundCount: Int = 0) : ConnectionState
    data class Connecting(val deviceName: String, val address: String = "") : ConnectionState
    data class Connected(
        val deviceName: String,
        val address: String,
        val batteryMv: Int? = null
    ) : ConnectionState
    data class Reconnecting(
        val deviceName: String,
        val address: String,
        val attempt: Int,
        val maxAttempts: Int = 3
    ) : ConnectionState
    data class Error(val message: String) : ConnectionState
}

data class DiscoveredDevice(
    val name: String,
    val address: String,
    val isBonded: Boolean,
    val rssi: Short = 0
)

/**
 * Robust Bluetooth Classic RFCOMM (SPP) Manager for LEGO Mindstorms NXT 2.0.
 * Handles active discovery, auto-connect to nearest NXT brick, auto-reconnection on link loss,
 * and background keep-alive telemetry.
 */
class NxtBluetoothManager(private val context: Context) {

    companion object {
        private const val TAG = "NxtBluetoothManager"
        // Standard Bluetooth Serial Port Profile (SPP) UUID
        val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

        fun hasBluetoothPermissions(context: Context): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
            } else {
                ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            }
        }
    }

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        BluetoothAdapter.getDefaultAdapter()
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val sendMutex = Mutex()

    private var socket: BluetoothSocket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _batteryMv = MutableStateFlow<Int?>(null)
    val batteryMv: StateFlow<Int?> = _batteryMv.asStateFlow()

    private val _discoveredDevices = MutableStateFlow<List<DiscoveredDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<DiscoveredDevice>> = _discoveredDevices.asStateFlow()

    private var lastConnectedAddress: String? = null
    private var lastConnectedName: String? = null
    private var isUserInitiatedDisconnect = false

    private var keepAliveJob: Job? = null
    private var discoveryReceiverRegistered = false

    // Broadcast receiver for active Bluetooth discovery
    private val discoveryReceiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action ?: return
            when (action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }
                    val rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE)

                    if (device != null) {
                        val name = device.name ?: "Unknown Device"
                        val address = device.address ?: return
                        val isBonded = device.bondState == BluetoothDevice.BOND_BONDED

                        val currentList = _discoveredDevices.value.toMutableList()
                        val existingIndex = currentList.indexOfFirst { it.address == address }
                        val discovered = DiscoveredDevice(name = name, address = address, isBonded = isBonded, rssi = rssi)

                        if (existingIndex >= 0) {
                            currentList[existingIndex] = discovered
                        } else {
                            currentList.add(discovered)
                        }

                        // Sort NXT bricks to top, then by signal strength (RSSI)
                        _discoveredDevices.value = currentList.sortedWith(
                            compareByDescending<DiscoveredDevice> { it.name.contains("NXT", ignoreCase = true) || it.name.contains("LEGO", ignoreCase = true) }
                                .thenByDescending { it.isBonded }
                                .thenByDescending { it.rssi }
                        )

                        val cur = _connectionState.value
                        if (cur is ConnectionState.Scanning) {
                            _connectionState.value = ConnectionState.Scanning(foundCount = _discoveredDevices.value.size)
                        }
                    }
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    val cur = _connectionState.value
                    if (cur is ConnectionState.Scanning) {
                        if (_discoveredDevices.value.isEmpty()) {
                            _connectionState.value = ConnectionState.Disconnected
                        }
                    }
                }
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                    if (state == BluetoothAdapter.STATE_OFF || state == BluetoothAdapter.STATE_TURNING_OFF) {
                        scope.launch { disconnect() }
                        _connectionState.value = ConnectionState.Error("Bluetooth turned off")
                    }
                }
            }
        }
    }

    fun isBluetoothSupported(): Boolean = bluetoothAdapter != null

    fun isBluetoothEnabled(): Boolean = bluetoothAdapter?.isEnabled == true

    /**
     * Start active Bluetooth discovery and refresh bonded devices.
     */
    @SuppressLint("MissingPermission")
    fun startDiscovery(): Boolean {
        val adapter = bluetoothAdapter ?: return false
        if (!adapter.isEnabled) return false

        // Load currently bonded devices first
        refreshBondedDevices()

        // Register discovery broadcast receiver
        if (!discoveryReceiverRegistered) {
            val filter = IntentFilter().apply {
                addAction(BluetoothDevice.ACTION_FOUND)
                addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
                addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            }
            context.registerReceiver(discoveryReceiver, filter)
            discoveryReceiverRegistered = true
        }

        try {
            if (adapter.isDiscovering) {
                adapter.cancelDiscovery()
            }
            _connectionState.value = ConnectionState.Scanning(foundCount = _discoveredDevices.value.size)
            return adapter.startDiscovery()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start discovery", e)
            return false
        }
    }

    /**
     * Stop active Bluetooth discovery.
     */
    @SuppressLint("MissingPermission")
    fun stopDiscovery() {
        val adapter = bluetoothAdapter
        try {
            if (adapter?.isDiscovering == true) {
                adapter.cancelDiscovery()
            }
        } catch (_: Exception) {}

        if (discoveryReceiverRegistered) {
            try {
                context.unregisterReceiver(discoveryReceiver)
            } catch (_: Exception) {}
            discoveryReceiverRegistered = false
        }
    }

    @SuppressLint("MissingPermission")
    fun refreshBondedDevices(): List<DiscoveredDevice> {
        val adapter = bluetoothAdapter ?: return emptyList()
        return try {
            val bonded = adapter.bondedDevices.map { device ->
                DiscoveredDevice(
                    name = device.name ?: "NXT Brick",
                    address = device.address,
                    isBonded = true
                )
            }.sortedByDescending { it.name.contains("NXT", ignoreCase = true) || it.name.contains("LEGO", ignoreCase = true) }

            _discoveredDevices.value = bonded
            bonded
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get bonded devices", e)
            emptyList()
        }
    }

    fun getPairedDevices(): List<DiscoveredDevice> = refreshBondedDevices()

    /**
     * Automatically detect and connect to the nearest or first matching LEGO NXT brick.
     */
    @SuppressLint("MissingPermission")
    suspend fun autoConnectToNearestNxt(): Boolean {
        return withContext(Dispatchers.IO) {
            val adapter = bluetoothAdapter ?: return@withContext false
            if (!adapter.isEnabled) {
                _connectionState.value = ConnectionState.Error("Bluetooth is disabled. Please turn Bluetooth on.")
                return@withContext false
            }

            // Step 1: Check paired devices for NXT
            val bondedNxt = refreshBondedDevices().firstOrNull {
                it.name.contains("NXT", ignoreCase = true) || it.name.contains("LEGO", ignoreCase = true)
            }

            if (bondedNxt != null) {
                Log.i(TAG, "Auto-connecting to bonded NXT: ${bondedNxt.name} (${bondedNxt.address})")
                connect(bondedNxt.address, bondedNxt.name)
                return@withContext isConnected()
            }

            // Step 2: Start discovery if not bonded
            startDiscovery()
            _connectionState.value = ConnectionState.Scanning(foundCount = _discoveredDevices.value.size)

            // Wait up to 6 seconds for an NXT device to appear in scan
            val startTime = System.currentTimeMillis()
            var detectedNxt: DiscoveredDevice? = null
            while (System.currentTimeMillis() - startTime < 6000L) {
                detectedNxt = _discoveredDevices.value.firstOrNull {
                    it.name.contains("NXT", ignoreCase = true) || it.name.contains("LEGO", ignoreCase = true)
                }
                if (detectedNxt != null) break
                delay(300)
            }

            stopDiscovery()

            if (detectedNxt != null) {
                Log.i(TAG, "Auto-connecting to discovered NXT: ${detectedNxt.name} (${detectedNxt.address})")
                connect(detectedNxt.address, detectedNxt.name)
                return@withContext isConnected()
            } else {
                _connectionState.value = ConnectionState.Error("No LEGO NXT brick detected nearby. Make sure your brick's Bluetooth is set to Visible.")
                return@withContext false
            }
        }
    }

    /**
     * Connect to the selected Bluetooth device address over RFCOMM SPP.
     */
    @SuppressLint("MissingPermission")
    suspend fun connect(address: String, deviceName: String = "NXT Brick") {
        withContext(Dispatchers.IO) {
            disconnectInternal(isUserDisconnect = false)
            isUserInitiatedDisconnect = false
            _connectionState.value = ConnectionState.Connecting(deviceName, address)

            val adapter = bluetoothAdapter
            if (adapter == null) {
                _connectionState.value = ConnectionState.Error("Bluetooth not supported on this device")
                return@withContext
            }

            try {
                // Cancel discovery to maximize RFCOMM throughput
                stopDiscovery()

                val device = adapter.getRemoteDevice(address)
                var newSocket: BluetoothSocket? = null

                // Attempt 1: Standard SPP UUID
                try {
                    newSocket = device.createRfcommSocketToServiceRecord(SPP_UUID)
                    newSocket.connect()
                } catch (e: Exception) {
                    Log.w(TAG, "Standard SPP connection failed, attempting reflection channel 1: ${e.message}")
                    try {
                        newSocket?.close()
                    } catch (_: Exception) {}

                    // Attempt 2: Channel 1 reflection fallback for NXT firmware compatibility
                    val method = device.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
                    newSocket = method.invoke(device, 1) as BluetoothSocket
                    newSocket.connect()
                }

                socket = newSocket
                inputStream = newSocket.inputStream
                outputStream = newSocket.outputStream

                lastConnectedAddress = address
                lastConnectedName = device.name ?: deviceName

                // Stream stabilization delay
                delay(150)

                // Initialize standard sensors (ports 1, 2 = touch, 3 = color, 4 = ultrasonic)
                initDefaultSensors()

                // Read battery level
                val bat = readBattery()
                _batteryMv.value = bat

                _connectionState.value = ConnectionState.Connected(
                    deviceName = device.name ?: deviceName,
                    address = address,
                    batteryMv = bat
                )
                Log.i(TAG, "Connected successfully to NXT: ${device.name} ($address)")

                // Launch keep-alive heartbeat loop
                startKeepAliveLoop()

            } catch (e: Exception) {
                Log.e(TAG, "Connection failed: ${e.message}", e)
                disconnectInternal(isUserDisconnect = false)
                _connectionState.value = ConnectionState.Error("Failed to connect: ${e.localizedMessage ?: e.message}")
            }
        }
    }

    /**
     * Progressive auto-reconnection with retry attempts upon unexpected link drop.
     */
    private suspend fun triggerAutoReconnect() {
        if (isUserInitiatedDisconnect) return
        val address = lastConnectedAddress ?: return
        val name = lastConnectedName ?: "NXT Brick"

        withContext(Dispatchers.IO) {
            disconnectInternal(isUserDisconnect = false)

            val maxAttempts = 3
            val backoffMs = listOf(2000L, 4000L, 8000L)

            for (attempt in 1..maxAttempts) {
                if (isUserInitiatedDisconnect) break
                _connectionState.value = ConnectionState.Reconnecting(
                    deviceName = name,
                    address = address,
                    attempt = attempt,
                    maxAttempts = maxAttempts
                )

                delay(backoffMs[attempt - 1])

                try {
                    val adapter = bluetoothAdapter ?: break
                    if (!adapter.isEnabled) break
                    val device = adapter.getRemoteDevice(address)

                    var newSocket: BluetoothSocket? = null
                    try {
                        newSocket = device.createRfcommSocketToServiceRecord(SPP_UUID)
                        newSocket.connect()
                    } catch (_: Exception) {
                        val method = device.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
                        newSocket = method.invoke(device, 1) as BluetoothSocket
                        newSocket.connect()
                    }

                    socket = newSocket
                    inputStream = newSocket.inputStream
                    outputStream = newSocket.outputStream

                    delay(150)
                    initDefaultSensors()
                    val bat = readBattery()
                    _batteryMv.value = bat

                    _connectionState.value = ConnectionState.Connected(name, address, bat)
                    startKeepAliveLoop()
                    Log.i(TAG, "Successfully auto-reconnected on attempt $attempt")
                    return@withContext

                } catch (e: Exception) {
                    Log.w(TAG, "Auto-reconnect attempt $attempt failed: ${e.message}")
                }
            }

            _connectionState.value = ConnectionState.Error("Lost connection to $name. Tap to reconnect.")
        }
    }

    private fun startKeepAliveLoop() {
        keepAliveJob?.cancel()
        keepAliveJob = scope.launch {
            while (isActive && isConnected()) {
                delay(12000L) // NXT auto-off timer is typically 2-10 minutes; keep-alive every 12s is optimal
                if (isActive && isConnected()) {
                    val ok = sendCommand(NxtCommand.keepAlive())
                    if (!ok) {
                        Log.w(TAG, "Keep-alive failed; triggering auto-reconnect")
                        triggerAutoReconnect()
                        break
                    }
                }
            }
        }
    }

    /**
     * User-initiated disconnect.
     */
    suspend fun disconnect() {
        withContext(Dispatchers.IO) {
            isUserInitiatedDisconnect = true
            stopDiscovery()
            disconnectInternal(isUserDisconnect = true)
            _connectionState.value = ConnectionState.Disconnected
        }
    }

    private suspend fun disconnectInternal(isUserDisconnect: Boolean) {
        keepAliveJob?.cancel()
        keepAliveJob = null

        try {
            if (socket?.isConnected == true) {
                stopAllMotors()
            }
        } catch (_: Exception) {}

        try {
            inputStream?.close()
        } catch (_: Exception) {}
        try {
            outputStream?.close()
        } catch (_: Exception) {}
        try {
            socket?.close()
        } catch (_: Exception) {}

        socket = null
        inputStream = null
        outputStream = null
        _batteryMv.value = null
    }

    fun isConnected(): Boolean {
        return socket?.isConnected == true && _connectionState.value is ConnectionState.Connected
    }

    suspend fun sendCommand(packet: ByteArray): Boolean {
        return withContext(Dispatchers.IO) {
            sendMutex.withLock {
                val out = outputStream ?: return@withLock false
                try {
                    out.write(packet)
                    out.flush()
                    true
                } catch (e: Exception) {
                    Log.e(TAG, "Send failed", e)
                    false
                }
            }
        }
    }

    suspend fun sendAndReceive(packet: ByteArray, timeoutMs: Long = 700): ByteArray? {
        return withContext(Dispatchers.IO) {
            sendMutex.withLock {
                val out = outputStream ?: return@withLock null
                val inStream = inputStream ?: return@withLock null

                try {
                    while (inStream.available() > 0) {
                        inStream.read()
                    }

                    out.write(packet)
                    out.flush()

                    val startTime = System.currentTimeMillis()
                    while (inStream.available() < 2) {
                        if (System.currentTimeMillis() - startTime > timeoutMs) {
                            return@withLock null
                        }
                        delay(5)
                    }

                    val lenLsb = inStream.read()
                    val lenMsb = inStream.read()
                    if (lenLsb < 0 || lenMsb < 0) return@withLock null
                    val length = (lenLsb and 0xFF) or ((lenMsb and 0xFF) shl 8)
                    if (length <= 0 || length > 1024) return@withLock null

                    val payload = ByteArray(length)
                    var bytesRead = 0
                    while (bytesRead < length) {
                        if (System.currentTimeMillis() - startTime > timeoutMs) {
                            return@withLock null
                        }
                        val read = inStream.read(payload, bytesRead, length - bytesRead)
                        if (read < 0) break
                        bytesRead += read
                        if (bytesRead < length) delay(5)
                    }

                    if (bytesRead == length) payload else null

                } catch (e: Exception) {
                    Log.e(TAG, "SendAndReceive failed; link dropped", e)
                    // Unexpected link drop during I/O
                    scope.launch { triggerAutoReconnect() }
                    null
                }
            }
        }
    }

    suspend fun initDefaultSensors() {
        sendCommand(NxtCommand.setInputMode(0, NxtCommand.SENSOR_TYPE_SWITCH, NxtCommand.SENSOR_MODE_BOOLEAN))
        delay(30)
        sendCommand(NxtCommand.setInputMode(1, NxtCommand.SENSOR_TYPE_SWITCH, NxtCommand.SENSOR_MODE_BOOLEAN))
        delay(30)
        sendCommand(NxtCommand.setInputMode(2, NxtCommand.SENSOR_TYPE_COLORFULL, NxtCommand.SENSOR_MODE_RAW))
        delay(30)
        sendCommand(NxtCommand.setInputMode(3, NxtCommand.SENSOR_TYPE_LOWSPEED_9V, NxtCommand.SENSOR_MODE_RAW))
        delay(30)
    }

    suspend fun readBattery(): Int? {
        val reply = sendAndReceive(NxtCommand.getBatteryLevel(), timeoutMs = 600) ?: return null
        val res = NxtResponse.parseBattery(reply)
        return if (res.success) {
            _batteryMv.value = res.millivolts
            res.millivolts
        } else null
    }

    suspend fun readTouch(port: Int): Boolean? {
        val reply = sendAndReceive(NxtCommand.getInputValues(port.toByte()), timeoutMs = 500) ?: return null
        val res = NxtResponse.parseSensorValues(reply)
        return if (res.success) {
            res.scaledValue == 1 || res.rawValue < 500
        } else null
    }

    suspend fun readColor(port: Int = 2): String? {
        val reply = sendAndReceive(NxtCommand.getInputValues(port.toByte()), timeoutMs = 500) ?: return null
        val res = NxtResponse.parseSensorValues(reply)
        return if (res.success) {
            NxtResponse.colorNameFromScaled(res.scaledValue)
        } else null
    }

    suspend fun readUltrasonic(port: Int = 3): Int? {
        sendCommand(NxtCommand.ultrasonicInitRead(port.toByte()))
        delay(30)

        val statusReply = sendAndReceive(NxtCommand.lsGetStatus(port.toByte()), timeoutMs = 400)
        if (statusReply != null) {
            val status = NxtResponse.parseLsStatus(statusReply)
            if (status.success && status.bytesReady > 0) {
                val readReply = sendAndReceive(NxtCommand.lsRead(port.toByte()), timeoutMs = 400)
                if (readReply != null) {
                    val ultra = NxtResponse.parseUltrasonic(readReply)
                    if (ultra.success && ultra.distanceCm in 0..255) {
                        return ultra.distanceCm
                    }
                }
            }
        }

        val fallbackReply = sendAndReceive(NxtCommand.getInputValues(port.toByte()), timeoutMs = 300)
        if (fallbackReply != null) {
            val res = NxtResponse.parseSensorValues(fallbackReply)
            if (res.success && res.scaledValue in 1..255) {
                return res.scaledValue
            }
        }
        return null
    }

    suspend fun runMotors(motorName: String, power: Int) {
        val parts = motorName.split("+")
        for (part in parts) {
            val portByte = when (part.trim().uppercase()) {
                "A" -> NxtCommand.MOTOR_A
                "B" -> NxtCommand.MOTOR_B
                "C" -> NxtCommand.MOTOR_C
                else -> continue
            }
            sendCommand(NxtCommand.setOutputState(portByte, power))
        }
    }

    suspend fun brakeMotors(motorName: String) {
        val parts = motorName.split("+")
        for (part in parts) {
            val portByte = when (part.trim().uppercase()) {
                "A" -> NxtCommand.MOTOR_A
                "B" -> NxtCommand.MOTOR_B
                "C" -> NxtCommand.MOTOR_C
                else -> continue
            }
            sendCommand(NxtCommand.brakeMotor(portByte))
        }
    }

    suspend fun stopAllMotors() {
        sendCommand(NxtCommand.brakeMotor(NxtCommand.MOTOR_A))
        sendCommand(NxtCommand.brakeMotor(NxtCommand.MOTOR_B))
        sendCommand(NxtCommand.brakeMotor(NxtCommand.MOTOR_C))
    }

    suspend fun playTone(hz: Int, durationMs: Int) {
        sendCommand(NxtCommand.playTone(hz, durationMs))
    }
}
