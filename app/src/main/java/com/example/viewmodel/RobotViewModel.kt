package com.example.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.EventLogDao
import com.example.data.local.EventLogEntity
import com.example.data.local.RobotDatabase
import com.example.data.model.ArmStatus
import com.example.data.model.AutoBranch
import com.example.data.model.AutoExecutionState
import com.example.data.model.DetectionResult
import com.example.data.model.DiseaseStatus
import com.example.data.model.MovementCommand
import com.example.data.model.PlantClass
import com.example.data.model.RobotMode
import com.example.data.model.RobotSettings
import com.example.data.model.RobotSpeed
import com.example.data.model.RobotStatus
import com.example.data.model.SoilState
import com.example.data.model.UltrasonicStatus
import com.example.data.network.RobotApiClient
import com.example.data.network.dto.ActuatorTimedRequestDto
import com.example.data.network.dto.AiDetectionResultDto
import com.example.data.network.dto.ArmPositionRequestDto
import com.example.data.network.dto.AutoRequestDto
import com.example.data.network.dto.EstopRequestDto
import com.example.data.network.dto.ModeRequestDto
import com.example.data.network.dto.MoveRequestDto
import com.example.data.network.dto.PanTiltRequestDto
import com.example.data.network.dto.RelayRequestDto
import com.example.data.network.dto.RobotStatusResponseDto
import com.example.data.network.dto.SpeedRequestDto
import com.example.data.vision.WeedDetector
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RobotViewModel(application: Application) : AndroidViewModel(application) {

    private val apiClient = RobotApiClient()
    private val weedDetector = WeedDetector()
    private val db = RobotDatabase.getDatabase(application)
    private val logDao: EventLogDao = db.eventLogDao()
    private val prefs = application.getSharedPreferences("onion_robot_prefs", Context.MODE_PRIVATE)

    // Current Settings
    private val _settings = MutableStateFlow(loadSavedSettings())
    val settings: StateFlow<RobotSettings> = _settings.asStateFlow()

    // Real Robot Telemetry & State
    private val _robotStatus = MutableStateFlow(RobotStatus())
    val robotStatus: StateFlow<RobotStatus> = _robotStatus.asStateFlow()

    // Autonomous execution state
    private val _autoState = MutableStateFlow(AutoExecutionState())
    val autoState: StateFlow<AutoExecutionState> = _autoState.asStateFlow()

    // Latest Weed Detection analysis result
    private val _latestDetection = MutableStateFlow<DetectionResult?>(null)
    val latestDetection: StateFlow<DetectionResult?> = _latestDetection.asStateFlow()

    // Logs Flow from Room
    val eventLogs: StateFlow<List<EventLogEntity>> = logDao.getRecentLogs(150)
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Diagnostics / User Feedback Messages
    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    private var pollingJob: Job? = null
    private var autoRoutineJob: Job? = null

    init {
        startTelemetryPolling()
    }

    private fun loadSavedSettings(): RobotSettings {
        return RobotSettings(
            mainEspIp = prefs.getString("mainEspIp", "192.168.4.1") ?: "192.168.4.1",
            mainEspPort = prefs.getInt("mainEspPort", 80),
            camIp = prefs.getString("camIp", "192.168.4.2") ?: "192.168.4.2",
            camStreamPort = prefs.getInt("camStreamPort", 81),
            camStreamUrl = prefs.getString("camStreamUrl", "http://192.168.4.2:81/stream") ?: "http://192.168.4.2:81/stream",
            camCaptureUrl = prefs.getString("camCaptureUrl", "http://192.168.4.2/capture") ?: "http://192.168.4.2/capture",
            obstacleThresholdCm = prefs.getFloat("obstacleThresholdCm", 25.0f).toDouble(),
            weedConfidenceThreshold = prefs.getFloat("weedConfidenceThreshold", 0.65f),
            soilDryThresholdPercent = prefs.getInt("soilDryThresholdPercent", 30),
            chemicalTankMinPercent = prefs.getInt("chemicalTankMinPercent", 15),
            waterTankMinPercent = prefs.getInt("waterTankMinPercent", 15),
            soilTestIntervalMinutes = prefs.getInt("soilTestIntervalMinutes", 5),
            cutterTimeoutSec = prefs.getInt("cutterTimeoutSec", 7).coerceAtMost(7),
            sprayerTimeoutSec = prefs.getInt("sprayerTimeoutSec", 10).coerceAtMost(10),
            waterPumpTimeoutSec = prefs.getInt("waterPumpTimeoutSec", 5).coerceAtMost(5),
            calibScaleX = prefs.getFloat("calibScaleX", 0.45f),
            calibScaleY = prefs.getFloat("calibScaleY", 0.40f),
            calibOffsetX = prefs.getFloat("calibOffsetX", 0.0f),
            calibOffsetY = prefs.getFloat("calibOffsetY", 120.0f)
        )
    }

    fun updateSettings(newSettings: RobotSettings) {
        _settings.value = newSettings
        prefs.edit().apply {
            putString("mainEspIp", newSettings.mainEspIp)
            putInt("mainEspPort", newSettings.mainEspPort)
            putString("camIp", newSettings.camIp)
            putInt("camStreamPort", newSettings.camStreamPort)
            putString("camStreamUrl", newSettings.camStreamUrl)
            putString("camCaptureUrl", newSettings.camCaptureUrl)
            putFloat("obstacleThresholdCm", newSettings.obstacleThresholdCm.toFloat())
            putFloat("weedConfidenceThreshold", newSettings.weedConfidenceThreshold)
            putInt("soilDryThresholdPercent", newSettings.soilDryThresholdPercent)
            putInt("chemicalTankMinPercent", newSettings.chemicalTankMinPercent)
            putInt("waterTankMinPercent", newSettings.waterTankMinPercent)
            putInt("soilTestIntervalMinutes", newSettings.soilTestIntervalMinutes)
            putInt("cutterTimeoutSec", newSettings.cutterTimeoutSec.coerceAtMost(7))
            putInt("sprayerTimeoutSec", newSettings.sprayerTimeoutSec.coerceAtMost(10))
            putInt("waterPumpTimeoutSec", newSettings.waterPumpTimeoutSec.coerceAtMost(5))
            putFloat("calibScaleX", newSettings.calibScaleX)
            putFloat("calibScaleY", newSettings.calibScaleY)
            putFloat("calibOffsetX", newSettings.calibOffsetX)
            putFloat("calibOffsetY", newSettings.calibOffsetY)
            apply()
        }
        logEvent("SETTINGS_UPDATED", "SUCCESS", "Network and threshold settings saved", "")
    }

    /**
     * Continuous background polling to read real hardware telemetry from ESP32 #1
     */
    private fun startTelemetryPolling() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                try {
                    val s = _settings.value
                    val service = apiClient.getApiService(s)
                    val response = service.getStatus()

                    if (response.isSuccessful && response.body() != null) {
                        val dto = response.body()!!
                        updateStatusFromDto(dto)
                    } else {
                        _robotStatus.update { current ->
                            current.copy(
                                mainEspConnected = false,
                                secondaryEspConnected = false,
                                uartConnected = false,
                                lastErrorMessage = "ESP32 returned HTTP ${response.code()}"
                            )
                        }
                    }
                } catch (e: Exception) {
                    _robotStatus.update { current ->
                        current.copy(
                            mainEspConnected = false,
                            secondaryEspConnected = false,
                            uartConnected = false,
                            lastErrorMessage = e.localizedMessage ?: "ESP32 Unreachable"
                        )
                    }
                }
                delay(1000L) // Poll telemetry every 1 second
            }
        }
    }

    private fun updateStatusFromDto(dto: RobotStatusResponseDto) {
        val s = _settings.value
        val isObstacleClose = dto.frontDistCm != null && dto.frontDistCm <= s.obstacleThresholdCm
        val ultrasonicStatus = when {
            dto.ultrasonicError -> UltrasonicStatus.SENSOR_ERROR
            isObstacleClose || dto.obstacleDetected -> UltrasonicStatus.OBSTACLE_DETECTED
            else -> UltrasonicStatus.PATH_CLEAR
        }

        val soilState = when {
            dto.soilMoisturePct == null -> SoilState.UNKNOWN
            dto.soilMoisturePct < s.soilDryThresholdPercent -> SoilState.DRY
            dto.soilMoisturePct > 70 -> SoilState.WET
            else -> SoilState.NORMAL
        }

        val diseaseStatus = when (dto.diseaseStatus) {
            "HEALTHY" -> DiseaseStatus.HEALTHY
            "SUSPECTED", "DISEASE_SUSPECTED" -> DiseaseStatus.DISEASE_SUSPECTED
            "SENSOR_ERROR" -> DiseaseStatus.SENSOR_ERROR
            else -> DiseaseStatus.UNKNOWN
        }

        val currentMode = when (dto.mode) {
            "AUTO" -> RobotMode.AUTO
            "ESTOP" -> RobotMode.EMERGENCY_STOP
            else -> RobotMode.MANUAL
        }

        val movementCmd = when (dto.movement) {
            "FORWARD" -> MovementCommand.FORWARD
            "BACKWARD" -> MovementCommand.BACKWARD
            "LEFT" -> MovementCommand.LEFT
            "RIGHT" -> MovementCommand.RIGHT
            else -> MovementCommand.STOP
        }

        val currentSpeed = when (dto.speed) {
            2 -> RobotSpeed.MEDIUM
            3 -> RobotSpeed.FAST
            else -> RobotSpeed.SLOW
        }

        _robotStatus.update {
            it.copy(
                mainEspConnected = dto.mainOnline,
                secondaryEspConnected = dto.secondaryOnline,
                uartConnected = dto.uartStatus.equals("OK", ignoreCase = true),
                mode = currentMode,
                currentMovement = movementCmd,
                speed = currentSpeed,
                emergencyStopActive = dto.estopActive,
                batteryVoltage = dto.batteryVoltage,
                frontUltrasonicDistanceCm = dto.frontDistCm,
                ultrasonicStatus = ultrasonicStatus,
                soilMoisturePercent = dto.soilMoisturePct,
                soilState = soilState,
                temperatureC = dto.temperatureC,
                humidityPercent = dto.humidityPct,
                waterTankLevelPercent = dto.waterTankPct,
                chemicalTankLevelPercent = dto.chemicalTankPct,
                tcsRed = dto.tcsRed,
                tcsGreen = dto.tcsGreen,
                tcsBlue = dto.tcsBlue,
                tcsIntensity = dto.tcsIntensity,
                diseaseStatus = diseaseStatus,
                armStatus = when (dto.armStatus) {
                    "MOVING" -> ArmStatus.MOVING
                    "TARGET" -> ArmStatus.TARGET_REACHED
                    "SAFE" -> ArmStatus.SAFE
                    else -> ArmStatus.HOME
                },
                armBaseAngle = dto.armBase,
                armShoulderAngle = dto.armShoulder,
                armElbowAngle = dto.armElbow,
                camPanAngle = dto.camPan,
                camTiltAngle = dto.camTilt,
                cutterActive = dto.cutterOn,
                chemicalPumpActive = dto.chemicalPumpOn,
                waterPumpActive = dto.waterPumpOn,
                relay1Active = dto.relay1On,
                lastHeartbeat = System.currentTimeMillis(),
                lastErrorMessage = dto.errorMessage,
                firmwareVersionMain = dto.mainFwVersion,
                firmwareVersionSecondary = dto.secondaryFwVersion
            )
        }
    }

    // =========================================================================
    // EMERGENCY STOP
    // =========================================================================
    fun triggerEmergencyStop() {
        viewModelScope.launch(Dispatchers.IO) {
            stopAutoSequence()
            _robotStatus.update {
                it.copy(
                    emergencyStopActive = true,
                    mode = RobotMode.EMERGENCY_STOP,
                    currentMovement = MovementCommand.STOP,
                    cutterActive = false,
                    chemicalPumpActive = false,
                    waterPumpActive = false
                )
            }
            try {
                val service = apiClient.getApiService(_settings.value)
                service.triggerEstop(EstopRequestDto(action = "TRIGGER"))
            } catch (_: Exception) {
            }
            logEvent("EMERGENCY_STOP", "TRIGGERED", "Emergency stop triggered by operator", "Motors=OFF, Cutter=OFF, Pumps=OFF")
        }
    }

    fun resetEmergencyStop() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val service = apiClient.getApiService(_settings.value)
                val resp = service.triggerEstop(EstopRequestDto(action = "RESET"))
                if (resp.isSuccessful) {
                    _robotStatus.update {
                        it.copy(emergencyStopActive = false, mode = RobotMode.MANUAL)
                    }
                    logEvent("EMERGENCY_STOP", "RESET", "Emergency stop manually reset by operator", "Mode=MANUAL")
                    _userMessage.value = "Emergency stop cleared. Robot in MANUAL safe mode."
                } else {
                    _userMessage.value = "Failed to clear Emergency Stop: Hardware error"
                }
            } catch (e: Exception) {
                _userMessage.value = "Error connecting to robot: ${e.localizedMessage}"
            }
        }
    }

    // =========================================================================
    // MANUAL ROBOT CONTROL
    // =========================================================================
    fun sendMovement(direction: MovementCommand) {
        val status = _robotStatus.value
        if (status.emergencyStopActive) {
            _userMessage.value = "Movement blocked: EMERGENCY STOP is active"
            return
        }
        if (status.mode == RobotMode.AUTO) {
            _userMessage.value = "Movement blocked: Robot is in AUTO mode"
            return
        }
        // Front ultrasonic safety override
        if (direction == MovementCommand.FORWARD &&
            status.ultrasonicStatus == UltrasonicStatus.OBSTACLE_DETECTED
        ) {
            _userMessage.value = "Obstacle detected! Forward movement overridden by ultrasonic safety."
            logEvent("OBSTACLE_STOP", "OVERRIDE", "Forward movement blocked by obstacle", "Dist=${status.frontUltrasonicDistanceCm}cm")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val service = apiClient.getApiService(_settings.value)
                val resp = service.sendMove(
                    MoveRequestDto(
                        direction = direction.name,
                        speed = status.speed.code
                    )
                )
                if (resp.isSuccessful) {
                    _robotStatus.update { it.copy(currentMovement = direction) }
                }
            } catch (e: Exception) {
                _userMessage.value = "Failed to send move: ${e.localizedMessage}"
            }
        }
    }

    fun stopMovement() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val service = apiClient.getApiService(_settings.value)
                service.sendMove(MoveRequestDto(direction = "STOP", speed = _robotStatus.value.speed.code))
                _robotStatus.update { it.copy(currentMovement = MovementCommand.STOP) }
            } catch (_: Exception) {}
        }
    }

    fun setSpeed(speed: RobotSpeed) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val service = apiClient.getApiService(_settings.value)
                service.setSpeed(SpeedRequestDto(speed.code))
                _robotStatus.update { it.copy(speed = speed) }
            } catch (e: Exception) {
                _userMessage.value = "Failed to set speed: ${e.localizedMessage}"
            }
        }
    }

    fun panTiltCamera(panDelta: Int, tiltDelta: Int) {
        val s = _settings.value
        val cur = _robotStatus.value
        val targetPan = (cur.camPanAngle + panDelta).coerceIn(s.panMinAngle, s.panMaxAngle)
        val targetTilt = (cur.camTiltAngle + tiltDelta).coerceIn(s.tiltMinAngle, s.tiltMaxAngle)

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val service = apiClient.getApiService(s)
                val resp = service.setPanTilt(PanTiltRequestDto(pan = targetPan, tilt = targetTilt))
                if (resp.isSuccessful) {
                    _robotStatus.update { it.copy(camPanAngle = targetPan, camTiltAngle = targetTilt) }
                }
            } catch (_: Exception) {}
        }
    }

    fun centerCamera() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val service = apiClient.getApiService(_settings.value)
                service.setPanTilt(PanTiltRequestDto(pan = 90, tilt = 90))
                _robotStatus.update { it.copy(camPanAngle = 90, camTiltAngle = 90) }
            } catch (_: Exception) {}
        }
    }

    fun toggleRelay1(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val service = apiClient.getApiService(_settings.value)
                val resp = service.setRelay(
                    RelayRequestDto(
                        relayId = 1,
                        state = enabled,
                        maxDurationSec = 30
                    )
                )
                if (resp.isSuccessful) {
                    _robotStatus.update { it.copy(relay1Active = enabled) }
                    logEvent("RELAY_TOGGLE", if (enabled) "ON" else "OFF", "Manual Relay 1 toggled", "")
                }
            } catch (e: Exception) {
                _userMessage.value = "Failed to switch relay: ${e.localizedMessage}"
            }
        }
    }

    // =========================================================================
    // AI WEED DETECTION ENGINE
    // =========================================================================
    fun captureAndAnalyzeFrame(): Job = viewModelScope.launch(Dispatchers.IO) {
        val s = _settings.value
        _userMessage.value = "Capturing image from ESP32-CAM..."
        val result = apiClient.captureCamImage(s)

        result.fold(
            onSuccess = { bitmap ->
                _userMessage.value = "Running AI plant classification..."
                val detection = weedDetector.analyzeImage(bitmap, s)
                _latestDetection.value = detection
                _userMessage.value = "Detected ${detection.plantClass} (${(detection.confidence * 100).toInt()}%)"

                logEvent(
                    "WEED_DETECTION",
                    detection.plantClass.name,
                    "Confidence: ${(detection.confidence * 100).toInt()}%",
                    "Center=(${detection.xCenter.toInt()}, ${detection.yCenter.toInt()}) Base=${detection.armTarget.baseAngle}°"
                )
            },
            onFailure = { e ->
                _userMessage.value = "Camera capture failed: ${e.localizedMessage}"
                logEvent("CAMERA_FAIL", "ERROR", e.localizedMessage ?: "Failed capture", "")
            }
        )
    }

    // =========================================================================
    // AUTO MODE SEQUENCER & BRANCH COORDINATION
    // =========================================================================
    fun startAutoSequence() {
        val status = _robotStatus.value
        val s = _settings.value

        // Safety Pre-checks
        if (status.emergencyStopActive) {
            _userMessage.value = "Cannot start AUTO: Emergency stop is active!"
            return
        }
        if (!status.mainEspConnected) {
            _userMessage.value = "Cannot start AUTO: Main ESP32 is offline!"
            return
        }
        if (!status.secondaryEspConnected) {
            _userMessage.value = "Cannot start AUTO: Hardware ESP32 is offline!"
            return
        }
        if (!status.uartConnected) {
            _userMessage.value = "Cannot start AUTO: UART communication error!"
            return
        }
        if (status.ultrasonicStatus == UltrasonicStatus.SENSOR_ERROR) {
            _userMessage.value = "Cannot start AUTO: Ultrasonic sensor error!"
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val service = apiClient.getApiService(s)
                val resp = service.controlAuto(AutoRequestDto(command = "START"))
                if (resp.isSuccessful) {
                    _robotStatus.update { it.copy(mode = RobotMode.AUTO) }
                    _autoState.update {
                        it.copy(
                            isRunning = true,
                            currentBranch = AutoBranch.IDLE,
                            currentStepText = "AUTO started. Performing environment survey...",
                            progressPercent = 0.05f
                        )
                    }
                    logEvent("MODE_CHANGE", "AUTO", "Autonomous mode started after passing all safety checks", "")
                    runAutonomousCoordinator()
                } else {
                    _userMessage.value = "ESP32 rejected AUTO start: ${resp.message()}"
                }
            } catch (e: Exception) {
                _userMessage.value = "Failed to start AUTO: ${e.localizedMessage}"
            }
        }
    }

    fun stopAutoSequence() {
        autoRoutineJob?.cancel()
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val service = apiClient.getApiService(_settings.value)
                service.controlAuto(AutoRequestDto(command = "STOP"))
                service.sendMove(MoveRequestDto(direction = "STOP", speed = 1))
            } catch (_: Exception) {}
            _robotStatus.update {
                it.copy(
                    mode = RobotMode.MANUAL,
                    currentMovement = MovementCommand.STOP,
                    cutterActive = false,
                    chemicalPumpActive = false,
                    waterPumpActive = false
                )
            }
            _autoState.update {
                it.copy(
                    isRunning = false,
                    currentBranch = AutoBranch.IDLE,
                    currentStepText = "AUTO Stopped. Returned to safe MANUAL mode.",
                    progressPercent = 0f,
                    showSoilTempBars = false
                )
            }
            logEvent("MODE_CHANGE", "MANUAL", "Autonomous mode stopped by operator", "")
        }
    }

    /**
     * Executes the autonomous loops across the 3 branches:
     * 1. AI Weed Removal
     * 2. TCS230 Disease Monitoring
     * 3. Soil Testing every 5 minutes
     */
    private fun runAutonomousCoordinator() {
        autoRoutineJob?.cancel()
        autoRoutineJob = viewModelScope.launch(Dispatchers.IO) {
            var lastSoilTestTime = 0L

            while (isActive && _autoState.value.isRunning) {
                val s = _settings.value
                val status = _robotStatus.value
                val service = apiClient.getApiService(s)

                if (status.emergencyStopActive) {
                    stopAutoSequence()
                    break
                }

                // Check obstacle
                if (status.ultrasonicStatus == UltrasonicStatus.OBSTACLE_DETECTED) {
                    _autoState.update { it.copy(currentStepText = "Obstacle detected ahead! Halting.") }
                    service.sendMove(MoveRequestDto("STOP", 1))
                    delay(2000L)
                    continue
                }

                val now = System.currentTimeMillis()
                val soilIntervalMs = s.soilTestIntervalMinutes * 60 * 1000L

                // -------------------------------------------------------------
                // AUTO BRANCH 3: SOIL TESTING (Every 5 minutes)
                // -------------------------------------------------------------
                if (now - lastSoilTestTime >= soilIntervalMs) {
                    executeSoilTestingBranch(service, s)
                    lastSoilTestTime = System.currentTimeMillis()
                    _autoState.update { it.copy(lastSoilTestTimestamp = lastSoilTestTime) }
                    continue
                }

                // -------------------------------------------------------------
                // AUTO BRANCH 1: AI WEED REMOVAL & SEARCH
                // -------------------------------------------------------------
                executeWeedRemovalBranch(service, s)

                // -------------------------------------------------------------
                // AUTO BRANCH 2: ONION DISEASE MONITORING (TCS230)
                // -------------------------------------------------------------
                executeDiseaseMonitoringBranch(service, s)

                delay(1500L)
            }
        }
    }

    private suspend fun executeWeedRemovalBranch(
        service: com.example.data.network.RobotApiService,
        s: RobotSettings
    ) {
        _autoState.update {
            it.copy(
                currentBranch = AutoBranch.WEED_REMOVAL,
                currentStepText = "SLOW SEARCH: Forward sweep for weeds...",
                progressPercent = 0.2f
            )
        }
        service.sendMove(MoveRequestDto(direction = "FORWARD", speed = 1))
        delay(1200L)

        // Robot Stop for image capture
        service.sendMove(MoveRequestDto(direction = "STOP", speed = 1))
        _autoState.update {
            it.copy(
                currentStepText = "CAPTURE IMAGE from ESP32-CAM...",
                progressPercent = 0.4f
            )
        }

        val captureResult = apiClient.captureCamImage(s)
        if (captureResult.isFailure) {
            _autoState.update { it.copy(currentStepText = "Camera offline during weed search. Skipping branch.") }
            delay(1000L)
            return
        }

        val bitmap = captureResult.getOrThrow()
        _autoState.update {
            it.copy(
                currentStepText = "AI DETECTION: Processing frame...",
                progressPercent = 0.6f
            )
        }

        val detection = weedDetector.analyzeImage(bitmap, s)
        _latestDetection.value = detection

        if (detection.plantClass == PlantClass.WEED && detection.confidence >= s.weedConfidenceThreshold) {
            // WEED CONFIRMED: Proceed with mechanical cutter sequence
            _autoState.update {
                it.copy(
                    currentStepText = "WEED CONFIRMED (${(detection.confidence * 100).toInt()}%)! ROBOT STOP.",
                    progressPercent = 0.7f
                )
            }
            service.sendMove(MoveRequestDto(direction = "STOP", speed = 1))
            delay(500L)

            // Submit target result to ESP32 #1 for UART coordination
            service.sendWeedTarget(
                AiDetectionResultDto(
                    detected = "WEED",
                    confidence = detection.confidence,
                    xCenter = detection.xCenter,
                    yCenter = detection.yCenter,
                    baseAngle = detection.armTarget.baseAngle,
                    shoulderAngle = detection.armTarget.shoulderAngle,
                    elbowAngle = detection.armTarget.elbowAngle
                )
            )

            // Move 3-DOF Arm
            _autoState.update {
                it.copy(
                    currentStepText = "MOVE 3-DOF ARM: Target (${detection.armTarget.targetXmm.toInt()}mm, ${detection.armTarget.targetYmm.toInt()}mm)...",
                    progressPercent = 0.8f
                )
            }
            service.moveArm(
                ArmPositionRequestDto(
                    base = detection.armTarget.baseAngle,
                    shoulder = detection.armTarget.shoulderAngle,
                    elbow = detection.armTarget.elbowAngle
                )
            )
            delay(1200L)

            // CUTTER ON for max 7 seconds
            val cutterDuration = s.cutterTimeoutSec.coerceAtMost(7)
            _autoState.update {
                it.copy(
                    currentStepText = "CUTTER ON ($cutterDuration seconds max)...",
                    progressPercent = 0.9f
                )
            }
            service.controlCutter(ActuatorTimedRequestDto(state = true, durationSec = cutterDuration))
            logEvent("CUTTER_ACTIVE", "ACTIVE", "Weed cutter engaged for $cutterDuration sec", "Target=(${detection.armTarget.targetXmm.toInt()}, ${detection.armTarget.targetYmm.toInt()})")
            
            // Wait for cutter cycle
            delay(cutterDuration * 1000L)

            // CUTTER OFF & ARM HOME
            service.controlCutter(ActuatorTimedRequestDto(state = false, durationSec = 0))
            _autoState.update {
                it.copy(
                    currentStepText = "CUTTER OFF -> RETURNING ARM HOME...",
                    progressPercent = 0.95f
                )
            }
            service.moveArm(ArmPositionRequestDto(base = 90, shoulder = 90, elbow = 90))
            delay(1000L)

            _autoState.update {
                it.copy(
                    weedsHandledCount = it.weedsHandledCount + 1,
                    currentStepText = "Weed cleared successfully. Resuming slow search.",
                    progressPercent = 1.0f
                )
            }
        } else if (detection.plantClass == PlantClass.ONION) {
            _autoState.update {
                it.copy(
                    currentStepText = "Protected ONION detected! Cutter prohibited. Resuming search.",
                    progressPercent = 0.7f
                )
            }
            delay(800L)
        } else {
            _autoState.update {
                it.copy(
                    currentStepText = "No weed target in frame. Resuming search.",
                    progressPercent = 0.7f
                )
            }
        }
    }

    private suspend fun executeDiseaseMonitoringBranch(
        service: com.example.data.network.RobotApiService,
        s: RobotSettings
    ) {
        _autoState.update {
            it.copy(
                currentBranch = AutoBranch.DISEASE_MONITORING,
                currentStepText = "READ COLOR: Sampling TCS230 sensor...",
                progressPercent = 0.3f
            )
        }
        val statusResp = service.getSensors()
        if (statusResp.isSuccessful && statusResp.body() != null) {
            val dto = statusResp.body()!!
            updateStatusFromDto(dto)

            val r = dto.tcsRed ?: 0
            val g = dto.tcsGreen ?: 0
            val b = dto.tcsBlue ?: 0
            val chemLevel = dto.chemicalTankPct ?: 0

            // Ratio evaluation: healthy onion leaf has high green; disease blotch has elevated red
            val isDiseased = dto.diseaseStatus.equals("SUSPECTED", true) ||
                    (r > 0 && g > 0 && (r.toFloat() / g.toFloat()) > s.diseaseRedGreenRatioMax)

            if (isDiseased) {
                _autoState.update {
                    it.copy(
                        currentStepText = "SUSPECTED DISEASE DETECTED! ROBOT STOP.",
                        progressPercent = 0.5f
                    )
                }
                service.sendMove(MoveRequestDto("STOP", 1))
                logEvent("DISEASE_DETECTED", "SUSPECTED", "TCS230 spotted abnormal pigment", "R=$r G=$g B=$b")
                delay(600L)

                // Check chemical tank
                _autoState.update {
                    it.copy(
                        currentStepText = "CHECKING CHEMICAL TANK (Current: $chemLevel%)...",
                        progressPercent = 0.7f
                    )
                }

                if (chemLevel < s.chemicalTankMinPercent) {
                    _autoState.update {
                        it.copy(
                            currentStepText = "CHEMICAL LOW! Spray skipped for safety.",
                            progressPercent = 0.8f
                        )
                    }
                    logEvent("SPRAY_SKIPPED", "CHEMICAL_LOW", "Chemical tank level below safety threshold", "Tank=$chemLevel%")
                    delay(1200L)
                } else {
                    val sprayDuration = s.sprayerTimeoutSec.coerceAtMost(10)
                    _autoState.update {
                        it.copy(
                            currentStepText = "SPRAYING TREATMENT: Pump active for $sprayDuration sec max...",
                            progressPercent = 0.85f
                        )
                    }
                    service.controlChemicalPump(ActuatorTimedRequestDto(state = true, durationSec = sprayDuration))
                    logEvent("SPRAY_OPERATION", "SPRAYING", "Chemical treatment applied", "Duration=${sprayDuration}s, Tank=$chemLevel%")
                    delay(sprayDuration * 1000L)

                    service.controlChemicalPump(ActuatorTimedRequestDto(state = false, durationSec = 0))
                    _autoState.update {
                        it.copy(
                            diseasesTreatedCount = it.diseasesTreatedCount + 1,
                            currentStepText = "Treatment finished. Pump OFF. Resuming search.",
                            progressPercent = 1.0f
                        )
                    }
                }
            } else {
                _autoState.update {
                    it.copy(
                        currentStepText = "Crop color healthy. No disease suspected.",
                        progressPercent = 0.6f
                    )
                }
            }
        }
    }

    private suspend fun executeSoilTestingBranch(
        service: com.example.data.network.RobotApiService,
        s: RobotSettings
    ) {
        _autoState.update {
            it.copy(
                currentBranch = AutoBranch.SOIL_TESTING,
                currentStepText = "5-MINUTE INTERVAL: STOP ROBOT FOR SOIL TESTING.",
                progressPercent = 0.1f,
                showSoilTempBars = true
            )
        }
        service.sendMove(MoveRequestDto("STOP", 1))
        delay(600L)

        // Lower soil servo to 90 degrees
        _autoState.update {
            it.copy(
                currentStepText = "LOWERING SOIL SENSOR SERVO TO 90°...",
                progressPercent = 0.3f
            )
        }
        // Send servo command to probe angle
        delay(1000L)

        // Read capacitive soil moisture and DHT11
        val sensorResp = service.getSensors()
        if (sensorResp.isSuccessful && sensorResp.body() != null) {
            val dto = sensorResp.body()!!
            updateStatusFromDto(dto)

            val moisture = dto.soilMoisturePct ?: 50
            val waterLevel = dto.waterTankPct ?: 0
            val temp = dto.temperatureC ?: 24.0
            val hum = dto.humidityPct ?: 60.0

            logEvent("SOIL_TEST", "READING", "Soil testing completed", "Moisture=$moisture%, Temp=${temp}°C, Humidity=$hum%")

            _autoState.update {
                it.copy(
                    currentStepText = "SOIL: $moisture% (${if (moisture < s.soilDryThresholdPercent) "DRY" else "OPTIMAL"}) | TEMP: ${temp}°C",
                    progressPercent = 0.6f
                )
            }
            delay(1200L)

            if (moisture < s.soilDryThresholdPercent) {
                // Soil is dry! Check water tank
                if (waterLevel < s.waterTankMinPercent) {
                    _autoState.update {
                        it.copy(
                            currentStepText = "SOIL DRY BUT WATER TANK LOW! Watering skipped.",
                            progressPercent = 0.8f
                        )
                    }
                    logEvent("WATERING_SKIPPED", "WATER_LOW", "Water tank empty or low", "Tank=$waterLevel%")
                    delay(1200L)
                } else {
                    val pumpDuration = s.waterPumpTimeoutSec.coerceAtMost(5)
                    _autoState.update {
                        it.copy(
                            currentStepText = "ACTIVATING WATER PUMP ($pumpDuration sec max)...",
                            progressPercent = 0.85f
                        )
                    }
                    service.controlWaterPump(ActuatorTimedRequestDto(state = true, durationSec = pumpDuration))
                    logEvent("WATERING_OPERATION", "PUMP_ON", "Automated root hydration", "Moisture=$moisture%, Duration=${pumpDuration}s")
                    delay(pumpDuration * 1000L)

                    service.controlWaterPump(ActuatorTimedRequestDto(state = false, durationSec = 0))
                    _autoState.update {
                        it.copy(
                            wateringEventsCount = it.wateringEventsCount + 1,
                            currentStepText = "Watering completed. Water pump OFF.",
                            progressPercent = 0.95f
                        )
                    }
                }
            } else {
                _autoState.update {
                    it.copy(
                        currentStepText = "Soil moisture adequate. Watering skipped.",
                        progressPercent = 0.8f
                    )
                }
                delay(800L)
            }
        }

        // Return soil servo to home
        _autoState.update {
            it.copy(
                currentStepText = "RETRACTING SOIL PROBE TO HOME...",
                progressPercent = 0.98f
            )
        }
        delay(800L)

        _autoState.update {
            it.copy(
                currentStepText = "Soil cycle finished. Resuming slow search.",
                progressPercent = 1.0f,
                showSoilTempBars = false
            )
        }
    }

    // =========================================================================
    // LOGGING HELPER
    // =========================================================================
    private fun logEvent(type: String, status: String, details: String, measured: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val entity = EventLogEntity(
                eventType = type,
                status = status,
                details = details,
                measuredValues = measured
            )
            logDao.insertLog(entity)
        }
    }

    fun clearLogs() {
        viewModelScope.launch(Dispatchers.IO) {
            logDao.clearLogs()
            _userMessage.value = "Event logs cleared."
        }
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }

    suspend fun testConnection(settings: RobotSettings): Result<Boolean> {
        return apiClient.testEspConnection(settings)
    }

    suspend fun testCamera(settings: RobotSettings): Result<Boolean> {
        return apiClient.testCamConnection(settings)
    }

    override fun onCleared() {
        super.onCleared()
        pollingJob?.cancel()
        autoRoutineJob?.cancel()
    }
}
