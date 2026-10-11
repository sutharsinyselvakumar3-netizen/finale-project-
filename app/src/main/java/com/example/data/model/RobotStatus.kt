package com.example.data.model

enum class RobotMode {
    MANUAL,
    AUTO,
    EMERGENCY_STOP
}

enum class MovementCommand {
    STOP,
    FORWARD,
    BACKWARD,
    LEFT,
    RIGHT
}

enum class RobotSpeed(val code: Int, val label: String) {
    SLOW(1, "SLOW"),
    MEDIUM(2, "MEDIUM"),
    FAST(3, "FAST")
}

enum class UltrasonicStatus {
    PATH_CLEAR,
    OBSTACLE_DETECTED,
    SENSOR_ERROR
}

enum class SoilState {
    DRY,
    NORMAL,
    WET,
    UNKNOWN
}

enum class DiseaseStatus {
    HEALTHY,
    DISEASE_SUSPECTED,
    UNKNOWN,
    SENSOR_ERROR
}

enum class ArmStatus {
    HOME,
    MOVING,
    TARGET_REACHED,
    SAFE
}

enum class AutoBranch {
    IDLE,
    WEED_REMOVAL,
    DISEASE_MONITORING,
    SOIL_TESTING
}

data class RobotStatus(
    val mainEspConnected: Boolean = false,
    val secondaryEspConnected: Boolean = false,
    val uartConnected: Boolean = false,
    val cameraConnected: Boolean = false,
    val mode: RobotMode = RobotMode.MANUAL,
    val currentMovement: MovementCommand = MovementCommand.STOP,
    val speed: RobotSpeed = RobotSpeed.SLOW,
    val emergencyStopActive: Boolean = false,

    // Real hardware telemetry (null when unavailable or error)
    val batteryVoltage: Double? = null,
    val frontUltrasonicDistanceCm: Double? = null,
    val ultrasonicStatus: UltrasonicStatus = UltrasonicStatus.PATH_CLEAR,
    
    // Soil & climate telemetry
    val soilMoisturePercent: Int? = null,
    val soilState: SoilState = SoilState.UNKNOWN,
    val temperatureC: Double? = null,
    val humidityPercent: Double? = null,
    
    // Tank levels
    val waterTankLevelPercent: Int? = null,
    val chemicalTankLevelPercent: Int? = null,
    
    // TCS230 color sensor
    val tcsRed: Int? = null,
    val tcsGreen: Int? = null,
    val tcsBlue: Int? = null,
    val tcsIntensity: Int? = null,
    val diseaseStatus: DiseaseStatus = DiseaseStatus.UNKNOWN,
    
    // Actuators
    val armStatus: ArmStatus = ArmStatus.HOME,
    val armBaseAngle: Int = 90,
    val armShoulderAngle: Int = 90,
    val armElbowAngle: Int = 90,
    val camPanAngle: Int = 90,
    val camTiltAngle: Int = 90,
    
    val cutterActive: Boolean = false,
    val chemicalPumpActive: Boolean = false,
    val waterPumpActive: Boolean = false,
    val relay1Active: Boolean = false,
    
    // Error / Diagnostic info
    val lastHeartbeat: Long = 0L,
    val lastErrorMessage: String? = null,
    val firmwareVersionMain: String = "v1.0.0-MAIN",
    val firmwareVersionSecondary: String = "v1.0.0-HW"
)

data class AutoExecutionState(
    val isRunning: Boolean = false,
    val currentBranch: AutoBranch = AutoBranch.IDLE,
    val currentStepText: String = "Ready to start",
    val progressPercent: Float = 0.0f,
    val lastSoilTestTimestamp: Long = 0L,
    val weedsHandledCount: Int = 0,
    val diseasesTreatedCount: Int = 0,
    val wateringEventsCount: Int = 0,
    val showSoilTempBars: Boolean = false
)
