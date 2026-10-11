package com.example.data.model

data class RobotSettings(
    // Network Settings
    val mainEspIp: String = "192.168.4.1",
    val mainEspPort: Int = 80,
    val camIp: String = "192.168.4.2",
    val camStreamPort: Int = 81,
    val camStreamUrl: String = "http://192.168.4.2:81/stream",
    val camCaptureUrl: String = "http://192.168.4.2/capture",
    val connectionTimeoutMs: Long = 3000L,
    val requestTimeoutMs: Long = 5000L,

    // Safety & Operational Thresholds
    val obstacleThresholdCm: Double = 25.0,
    val weedConfidenceThreshold: Float = 0.65f,
    val soilDryThresholdPercent: Int = 30,
    val chemicalTankMinPercent: Int = 15,
    val waterTankMinPercent: Int = 15,
    
    // Timeouts (Firmware & App Enforced)
    val soilTestIntervalMinutes: Int = 5,
    val cutterTimeoutSec: Int = 7,       // Max 7 sec per requirement
    val sprayerTimeoutSec: Int = 10,     // Max 10 sec per requirement
    val waterPumpTimeoutSec: Int = 5,    // Max 5 sec per requirement
    
    // Camera to 3-DOF Arm Calibration
    val calibScaleX: Float = 0.45f,
    val calibScaleY: Float = 0.40f,
    val calibOffsetX: Float = 0.0f,
    val calibOffsetY: Float = 120.0f, // mm forward offset
    
    // Servo Limits
    val panMinAngle: Int = 10,
    val panMaxAngle: Int = 170,
    val tiltMinAngle: Int = 20,
    val tiltMaxAngle: Int = 160,
    val armBaseMinAngle: Int = 15,
    val armBaseMaxAngle: Int = 165,
    val armShoulderMinAngle: Int = 20,
    val armShoulderMaxAngle: Int = 160,
    val armElbowMinAngle: Int = 20,
    val armElbowMaxAngle: Int = 160,

    // TCS230 Color Sensor Disease Calibration
    val healthyGreenRatioMin: Float = 1.3f, // Green distinctly higher than Red
    val diseaseRedGreenRatioMax: Float = 0.85f // Yellowish/brown diseased spots have high red & lower green
)
