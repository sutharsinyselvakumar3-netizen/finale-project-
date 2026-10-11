package com.example.data.network.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RobotStatusResponseDto(
    @Json(name = "mainOnline") val mainOnline: Boolean = true,
    @Json(name = "secondaryOnline") val secondaryOnline: Boolean = false,
    @Json(name = "uartStatus") val uartStatus: String = "ERROR", // "OK", "ERROR", "DISCONNECTED"
    @Json(name = "mode") val mode: String = "MANUAL",            // "MANUAL", "AUTO", "ESTOP"
    @Json(name = "movement") val movement: String = "STOP",      // "STOP", "FORWARD", etc.
    @Json(name = "speed") val speed: Int = 1,                    // 1: SLOW, 2: MED, 3: FAST
    @Json(name = "estopActive") val estopActive: Boolean = false,
    @Json(name = "batteryVoltage") val batteryVoltage: Double? = null,
    
    // Front Ultrasonic
    @Json(name = "frontDistCm") val frontDistCm: Double? = null,
    @Json(name = "obstacleDetected") val obstacleDetected: Boolean = false,
    @Json(name = "ultrasonicError") val ultrasonicError: Boolean = false,
    
    // Soil & Climate
    @Json(name = "soilMoisturePct") val soilMoisturePct: Int? = null,
    @Json(name = "temperatureC") val temperatureC: Double? = null,
    @Json(name = "humidityPct") val humidityPct: Double? = null,
    
    // Tanks
    @Json(name = "waterTankPct") val waterTankPct: Int? = null,
    @Json(name = "chemicalTankPct") val chemicalTankPct: Int? = null,
    
    // TCS230
    @Json(name = "tcsRed") val tcsRed: Int? = null,
    @Json(name = "tcsGreen") val tcsGreen: Int? = null,
    @Json(name = "tcsBlue") val tcsBlue: Int? = null,
    @Json(name = "tcsIntensity") val tcsIntensity: Int? = null,
    @Json(name = "diseaseStatus") val diseaseStatus: String? = null, // "HEALTHY", "SUSPECTED", etc.
    
    // Actuators
    @Json(name = "armStatus") val armStatus: String = "HOME",
    @Json(name = "armBase") val armBase: Int = 90,
    @Json(name = "armShoulder") val armShoulder: Int = 90,
    @Json(name = "armElbow") val armElbow: Int = 90,
    @Json(name = "camPan") val camPan: Int = 90,
    @Json(name = "camTilt") val camTilt: Int = 90,
    
    @Json(name = "cutterOn") val cutterOn: Boolean = false,
    @Json(name = "chemicalPumpOn") val chemicalPumpOn: Boolean = false,
    @Json(name = "waterPumpOn") val waterPumpOn: Boolean = false,
    @Json(name = "relay1On") val relay1On: Boolean = false,
    
    @Json(name = "autoActive") val autoActive: Boolean = false,
    @Json(name = "autoBranch") val autoBranch: String = "IDLE",
    @Json(name = "autoStep") val autoStep: String = "Standby",
    
    @Json(name = "mainFwVersion") val mainFwVersion: String = "1.0.0",
    @Json(name = "secondaryFwVersion") val secondaryFwVersion: String = "1.0.0",
    @Json(name = "systemUptimeSec") val systemUptimeSec: Long = 0L,
    @Json(name = "errorMessage") val errorMessage: String? = null
)

@JsonClass(generateAdapter = true)
data class SimpleApiResponseDto(
    @Json(name = "success") val success: Boolean = true,
    @Json(name = "message") val message: String = "",
    @Json(name = "timestamp") val timestamp: Long = System.currentTimeMillis()
)
