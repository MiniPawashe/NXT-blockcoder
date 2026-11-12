package com.example.nxt.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.nxt.bluetooth.ConnectionState
import com.example.nxt.bluetooth.DiscoveredDevice
import com.example.nxt.bluetooth.NxtBluetoothManager
import com.example.nxt.model.BlockCategory
import com.example.nxt.model.BlockSerializer
import com.example.nxt.model.BlockType
import com.example.nxt.model.NxtBlock
import com.example.nxt.model.NxtCondition
import com.example.nxt.runner.ExecutionStoppedException
import com.example.nxt.runner.NxtRunner
import com.example.nxt.data.NxtAppDatabase
import com.example.nxt.data.ProgramRepository
import com.example.nxt.data.SavedProgramEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID

enum class ExampleType(val title: String, val description: String) {
    OBSTACLE_AVOID("Obstacle Avoider", "Drives forward with A+C until Touch 1 is pressed, then backs up and turns"),
    COLOR_STOPPER("Color Stopper", "Runs motor A until red color is detected on Port 3, then beeps"),
    DISTANCE_GUARD("Ultrasonic Guard", "Waits until an object is closer than 20cm, then plays alarm beeps"),
    MUSICAL_CHIME("Musical Fanfare", "Plays ascending musical tones and pulses motor B")
}

class NxtViewModel(application: Application) : AndroidViewModel(application) {

    val btManager = NxtBluetoothManager(application)

    private val prefs = application.getSharedPreferences("nxt_blocks_prefs", Context.MODE_PRIVATE)

    private val _program = MutableStateFlow<List<NxtBlock>>(emptyList())
    val program: StateFlow<List<NxtBlock>> = _program.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _activeBlockId = MutableStateFlow<String?>(null)
    val activeBlockId: StateFlow<String?> = _activeBlockId.asStateFlow()

    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs: StateFlow<List<String>> = _logs.asStateFlow()

    val connectionState: StateFlow<ConnectionState> = btManager.connectionState
    val batteryMv: StateFlow<Int?> = btManager.batteryMv
    val discoveredDevices: StateFlow<List<DiscoveredDevice>> = btManager.discoveredDevices

    // Telemetry state for Direct Control tab
    private val _touch1 = MutableStateFlow<Boolean?>(null)
    val touch1: StateFlow<Boolean?> = _touch1.asStateFlow()

    private val _touch2 = MutableStateFlow<Boolean?>(null)
    val touch2: StateFlow<Boolean?> = _touch2.asStateFlow()

    private val _color = MutableStateFlow<String?>(null)
    val color: StateFlow<String?> = _color.asStateFlow()

    private val _distanceCm = MutableStateFlow<Int?>(null)
    val distanceCm: StateFlow<Int?> = _distanceCm.asStateFlow()

    private var runner: NxtRunner? = null
    private var executionJob: Job? = null
    private var telemetryJob: Job? = null

    private val database = NxtAppDatabase.getDatabase(application)
    val repository = ProgramRepository(database.programDao())

    val savedProjects: StateFlow<List<SavedProgramEntity>> = repository.allPrograms
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _activeProjectName = MutableStateFlow("Obstacle Avoider")
    val activeProjectName: StateFlow<String> = _activeProjectName.asStateFlow()

    private val _activeProjectId = MutableStateFlow<Long?>(null)
    val activeProjectId: StateFlow<Long?> = _activeProjectId.asStateFlow()

    init {
        viewModelScope.launch {
            repository.seedTemplatesIfEmpty()
        }
        loadSavedProgram()
        addLog("NXT Blocks v2 ready.")
        addLog("Touch 1 & 2: ports 1, 2. Color: port 3. Distance: port 4. Motors: A, B, C.")
    }

    private fun loadSavedProgram() {
        val savedJson = prefs.getString("saved_program", null)
        if (!savedJson.isNullOrBlank()) {
            val loaded = BlockSerializer.fromJson(savedJson)
            if (loaded.isNotEmpty()) {
                _program.value = loaded
                return
            }
        }
        // Default initial program matching user's desktop example
        loadExample(ExampleType.OBSTACLE_AVOID)
    }

    fun saveProgram() {
        val json = BlockSerializer.toJson(_program.value)
        prefs.edit().putString("saved_program", json).apply()
    }

    fun addLog(msg: String) {
        val list = _logs.value.toMutableList()
        list.add(msg)
        if (list.size > 250) list.removeAt(0)
        _logs.value = list
    }

    fun clearLogs() {
        _logs.value = emptyList()
    }

    // --- Block Manipulation ---

    fun createDefaultBlock(type: BlockType): NxtBlock {
        return when (type) {
            BlockType.MOTOR_ON -> NxtBlock(type = type, motor = "A+C", power = 60)
            BlockType.MOTOR_FOR -> NxtBlock(type = type, motor = "A+C", power = 60, seconds = 1.0)
            BlockType.MOTOR_OFF -> NxtBlock(type = type, motor = "A+C")
            BlockType.WAIT -> NxtBlock(type = type, seconds = 1.0)
            BlockType.WAIT_UNTIL -> NxtBlock(
                type = type,
                condition = NxtCondition("t1", "pressed", "20")
            )
            BlockType.SHOW -> NxtBlock(type = type, sensorKey = "dist")
            BlockType.BEEP -> NxtBlock(type = type, frequencyHz = 440, durationMs = 300)
            BlockType.REPEAT -> NxtBlock(type = type, repeatCount = 10, children = emptyList())
            BlockType.FOREVER -> NxtBlock(type = type, children = emptyList())
            BlockType.IF -> NxtBlock(
                type = type,
                condition = NxtCondition("t1", "pressed", "20"),
                children = emptyList()
            )
            BlockType.UNTIL -> NxtBlock(
                type = type,
                condition = NxtCondition("t1", "pressed", "20"),
                children = emptyList()
            )
        }
    }

    fun addBlock(type: BlockType, targetParentId: String? = null) {
        val newBlock = createDefaultBlock(type)
        if (targetParentId == null) {
            _program.value = _program.value + newBlock
        } else {
            _program.value = insertChildIntoList(_program.value, targetParentId, newBlock)
        }
        saveProgram()
    }

    fun insertBlockAtIndex(type: BlockType, index: Int, targetParentId: String? = null) {
        val newBlock = createDefaultBlock(type)
        if (targetParentId == null) {
            val current = _program.value.toMutableList()
            val safeIndex = index.coerceIn(0, current.size)
            current.add(safeIndex, newBlock)
            _program.value = current
        } else {
            _program.value = insertChildAtIndexInList(_program.value, targetParentId, index, newBlock)
        }
        saveProgram()
    }

    private fun insertChildAtIndexInList(
        list: List<NxtBlock>,
        targetParentId: String,
        index: Int,
        newBlock: NxtBlock
    ): List<NxtBlock> {
        return list.map { blk ->
            if (blk.id == targetParentId) {
                val currentChildren = blk.children.toMutableList()
                val safeIndex = index.coerceIn(0, currentChildren.size)
                currentChildren.add(safeIndex, newBlock)
                blk.copy(children = currentChildren)
            } else if (blk.isContainerBlock) {
                blk.copy(children = insertChildAtIndexInList(blk.children, targetParentId, index, newBlock))
            } else {
                blk
            }
        }
    }

    fun moveBlockToIndex(blockId: String, targetIndex: Int, targetParentId: String? = null) {
        val foundBlock = findBlockById(_program.value, blockId) ?: return
        val withoutBlock = removeBlockFromList(_program.value, blockId)
        if (targetParentId == null) {
            val mutable = withoutBlock.toMutableList()
            val safeIndex = targetIndex.coerceIn(0, mutable.size)
            mutable.add(safeIndex, foundBlock)
            _program.value = mutable
        } else {
            _program.value = insertChildAtIndexInList(withoutBlock, targetParentId, targetIndex, foundBlock)
        }
        saveProgram()
    }

    private fun findBlockById(list: List<NxtBlock>, blockId: String): NxtBlock? {
        for (blk in list) {
            if (blk.id == blockId) return blk
            if (blk.isContainerBlock) {
                val found = findBlockById(blk.children, blockId)
                if (found != null) return found
            }
        }
        return null
    }

    private fun insertChildIntoList(
        list: List<NxtBlock>,
        targetParentId: String,
        newBlock: NxtBlock
    ): List<NxtBlock> {
        return list.map { blk ->
            if (blk.id == targetParentId) {
                blk.copy(children = blk.children + newBlock)
            } else if (blk.isContainerBlock) {
                blk.copy(children = insertChildIntoList(blk.children, targetParentId, newBlock))
            } else {
                blk
            }
        }
    }

    fun removeBlock(blockId: String) {
        _program.value = removeBlockFromList(_program.value, blockId)
        saveProgram()
    }

    private fun removeBlockFromList(list: List<NxtBlock>, blockId: String): List<NxtBlock> {
        return list.filterNot { it.id == blockId }.map { blk ->
            if (blk.isContainerBlock) {
                blk.copy(children = removeBlockFromList(blk.children, blockId))
            } else {
                blk
            }
        }
    }

    fun updateBlock(updatedBlock: NxtBlock) {
        _program.value = updateBlockInList(_program.value, updatedBlock)
        saveProgram()
    }

    private fun updateBlockInList(list: List<NxtBlock>, updated: NxtBlock): List<NxtBlock> {
        return list.map { blk ->
            if (blk.id == updated.id) {
                updated
            } else if (blk.isContainerBlock) {
                blk.copy(children = updateBlockInList(blk.children, updated))
            } else {
                blk
            }
        }
    }

    fun moveBlock(blockId: String, delta: Int) {
        _program.value = moveBlockInList(_program.value, blockId, delta)
        saveProgram()
    }

    private fun moveBlockInList(list: List<NxtBlock>, blockId: String, delta: Int): List<NxtBlock> {
        val index = list.indexOfFirst { it.id == blockId }
        if (index != -1) {
            val newIndex = (index + delta).coerceIn(0, list.size - 1)
            if (index == newIndex) return list
            val mutable = list.toMutableList()
            val item = mutable.removeAt(index)
            mutable.add(newIndex, item)
            return mutable
        }
        return list.map { blk ->
            if (blk.isContainerBlock) {
                blk.copy(children = moveBlockInList(blk.children, blockId, delta))
            } else {
                blk
            }
        }
    }

    fun clearProgram() {
        _program.value = emptyList()
        saveProgram()
    }

    fun loadExample(type: ExampleType) {
        val blocks = when (type) {
            ExampleType.OBSTACLE_AVOID -> {
                // Exact equivalent of desktop Python code example
                listOf(
                    NxtBlock(
                        type = BlockType.FOREVER,
                        children = listOf(
                            NxtBlock(
                                type = BlockType.IF,
                                condition = NxtCondition(sensorKey = "t1", operator = "pressed", value = "20"),
                                children = listOf(
                                    NxtBlock(type = BlockType.MOTOR_ON, motor = "A+C", power = 60)
                                )
                            ),
                            NxtBlock(
                                type = BlockType.IF,
                                condition = NxtCondition(sensorKey = "t1", operator = "not pressed", value = "20"),
                                children = listOf(
                                    NxtBlock(type = BlockType.MOTOR_OFF, motor = "A+C")
                                )
                            )
                        )
                    )
                )
            }
            ExampleType.COLOR_STOPPER -> {
                listOf(
                    NxtBlock(type = BlockType.BEEP, frequencyHz = 523, durationMs = 200),
                    NxtBlock(type = BlockType.MOTOR_ON, motor = "B", power = 40),
                    NxtBlock(
                        type = BlockType.WAIT_UNTIL,
                        condition = NxtCondition(sensorKey = "color", operator = "is", value = "red")
                    ),
                    NxtBlock(type = BlockType.MOTOR_OFF, motor = "B"),
                    NxtBlock(type = BlockType.BEEP, frequencyHz = 880, durationMs = 500),
                    NxtBlock(type = BlockType.SHOW, sensorKey = "color")
                )
            }
            ExampleType.DISTANCE_GUARD -> {
                listOf(
                    NxtBlock(
                        type = BlockType.REPEAT,
                        repeatCount = 5,
                        children = listOf(
                            NxtBlock(type = BlockType.SHOW, sensorKey = "dist"),
                            NxtBlock(
                                type = BlockType.WAIT_UNTIL,
                                condition = NxtCondition(sensorKey = "dist", operator = "<", value = "25")
                            ),
                            NxtBlock(type = BlockType.BEEP, frequencyHz = 1000, durationMs = 150),
                            NxtBlock(type = BlockType.WAIT, seconds = 0.2),
                            NxtBlock(type = BlockType.BEEP, frequencyHz = 1200, durationMs = 200)
                        )
                    )
                )
            }
            ExampleType.MUSICAL_CHIME -> {
                listOf(
                    NxtBlock(type = BlockType.BEEP, frequencyHz = 262, durationMs = 180),
                    NxtBlock(type = BlockType.BEEP, frequencyHz = 330, durationMs = 180),
                    NxtBlock(type = BlockType.BEEP, frequencyHz = 392, durationMs = 180),
                    NxtBlock(type = BlockType.BEEP, frequencyHz = 523, durationMs = 350),
                    NxtBlock(type = BlockType.MOTOR_FOR, motor = "A", power = 50, seconds = 0.5)
                )
            }
        }
        _program.value = blocks
        saveProgram()
        addLog("Loaded preset: ${type.title}")
    }

    fun loadFromJson(jsonStr: String): Boolean {
        val parsed = BlockSerializer.fromJson(jsonStr)
        if (parsed.isNotEmpty()) {
            _program.value = parsed
            saveProgram()
            addLog("Imported program (${parsed.size} root blocks).")
            return true
        }
        return false
    }

    fun getProgramJson(): String {
        return BlockSerializer.toJson(_program.value)
    }

    // --- Room Database Project Persistence ---

    fun saveCurrentProject(name: String, description: String = "") {
        viewModelScope.launch {
            val id = _activeProjectId.value ?: 0L
            val newId = repository.saveProgram(
                name = name,
                description = description,
                blocks = _program.value,
                existingId = id
            )
            _activeProjectId.value = newId
            _activeProjectName.value = name
            saveProgram()
            addLog("Saved project '$name' to database.")
        }
    }

    fun loadProject(id: Long) {
        viewModelScope.launch {
            val result = repository.loadProgram(id)
            if (result != null) {
                _program.value = result.second
                _activeProjectId.value = result.first.id
                _activeProjectName.value = result.first.name
                saveProgram()
                addLog("Loaded project '${result.first.name}'.")
            }
        }
    }

    fun deleteProject(id: Long) {
        viewModelScope.launch {
            repository.deleteProgram(id)
            if (_activeProjectId.value == id) {
                _activeProjectId.value = null
                _activeProjectName.value = "Untitled Project"
            }
            addLog("Deleted project from database.")
        }
    }

    fun createNewProject() {
        _program.value = emptyList()
        _activeProjectId.value = null
        _activeProjectName.value = "New Project"
        saveProgram()
        addLog("Created new empty project.")
    }

    // --- Execution Controls ---

    fun runProgram() {
        if (_isRunning.value) {
            addLog("Already running. Press Stop first.")
            return
        }
        if (!btManager.isConnected()) {
            addLog("Warning: Not connected to NXT brick! Please connect via Bluetooth first.")
            return
        }

        _isRunning.value = true
        addLog("Running program...")

        val newRunner = NxtRunner(
            btManager = btManager,
            onLog = { logMsg -> addLog(logMsg) },
            onActiveBlockChanged = { id -> _activeBlockId.value = id }
        )
        runner = newRunner

        executionJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                newRunner.run(_program.value)
                addLog("Finished.")
            } catch (e: ExecutionStoppedException) {
                addLog("Stopped.")
            } catch (e: Exception) {
                addLog("Error: ${e.message}")
            } finally {
                _isRunning.value = false
                _activeBlockId.value = null
            }
        }
    }

    fun stopProgram() {
        runner?.stop()
        executionJob?.cancel()
        _isRunning.value = false
        _activeBlockId.value = null
        viewModelScope.launch(Dispatchers.IO) {
            try {
                btManager.stopAllMotors()
            } catch (_: Exception) {}
        }
        addLog("Stopped.")
    }

    // --- Bluetooth Management ---

    fun getPairedDevices(): List<DiscoveredDevice> = btManager.getPairedDevices()

    fun startDiscovery() {
        addLog("Scanning for nearby LEGO NXT bricks...")
        btManager.startDiscovery()
    }

    fun stopDiscovery() {
        btManager.stopDiscovery()
    }

    fun autoConnectToNxt() {
        viewModelScope.launch {
            addLog("Auto-connecting to nearest LEGO NXT brick...")
            val success = btManager.autoConnectToNearestNxt()
            if (success) {
                val bat = btManager.batteryMv.value
                val batStr = if (bat != null) " (Battery: ${bat / 1000.0}V)" else ""
                addLog("Connected: ${btManager.connectionState.value}$batStr")
            }
        }
    }

    fun connectDevice(address: String, name: String) {
        viewModelScope.launch {
            addLog("Connecting to brick: $name ($address)...")
            btManager.connect(address, name)
            if (btManager.isConnected()) {
                val bat = btManager.batteryMv.value
                val batStr = if (bat != null) " (Battery: ${bat / 1000.0}V)" else ""
                addLog("Connected: $name$batStr")
            } else {
                addLog("Connection failed.")
            }
        }
    }

    fun disconnectDevice() {
        viewModelScope.launch {
            stopProgram()
            btManager.disconnect()
            addLog("Disconnected from NXT.")
        }
    }

    // --- Telemetry Polling (when monitoring sensors) ---

    fun setTelemetryEnabled(enabled: Boolean) {
        telemetryJob?.cancel()
        if (enabled && btManager.isConnected() && !_isRunning.value) {
            telemetryJob = viewModelScope.launch(Dispatchers.IO) {
                while (isActive && btManager.isConnected() && !_isRunning.value) {
                    try {
                        _touch1.value = btManager.readTouch(0)
                        delay(60)
                        _touch2.value = btManager.readTouch(1)
                        delay(60)
                        _color.value = btManager.readColor(2)
                        delay(60)
                        _distanceCm.value = btManager.readUltrasonic(3)
                        delay(120)
                    } catch (_: Exception) {
                        delay(200)
                    }
                }
            }
        }
    }

    // --- Manual / Direct Remote Controls ---

    fun directRunMotor(motor: String, power: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            if (btManager.isConnected()) {
                btManager.runMotors(motor, power)
            }
        }
    }

    fun directBrakeMotor(motor: String) {
        viewModelScope.launch(Dispatchers.IO) {
            if (btManager.isConnected()) {
                btManager.brakeMotors(motor)
            }
        }
    }

    fun directPlayBeep(hz: Int, durationMs: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            if (btManager.isConnected()) {
                btManager.playTone(hz, durationMs)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopProgram()
        telemetryJob?.cancel()
        viewModelScope.launch(Dispatchers.IO) {
            btManager.disconnect()
        }
    }
}
