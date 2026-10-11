package com.example.data.network.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ModeRequestDto(
    @Json(name = "mode") val mode: String // "MANUAL", "AUTO"
)

@JsonClass(generateAdapter = true)
data class MoveRequestDto(
    @Json(name = "direction") val direction: String, // "STOP", "FORWARD", "BACKWARD", "LEFT", "RIGHT"
    @Json(name = "speed") val speed: Int             // 1: SLOW, 2: MED, 3: FAST
)

@JsonClass(generateAdapter = true)
data class SpeedRequestDto(
    @Json(name = "speed") val speed: Int
)

@JsonClass(generateAdapter = true)
data class PanTiltRequestDto(
    @Json(name = "pan") val pan: Int,
    @Json(name = "tilt") val tilt: Int
)

@JsonClass(generateAdapter = true)
data class RelayRequestDto(
    @Json(name = "relayId") val relayId: Int = 1,
    @Json(name = "state") val state: Boolean,
    @Json(name = "maxDurationSec") val maxDurationSec: Int = 30
)

@JsonClass(generateAdapter = true)
data class EstopRequestDto(
    @Json(name = "action") val action: String // "TRIGGER", "RESET"
)

@JsonClass(generateAdapter = true)
data class AutoRequestDto(
    @Json(name = "command") val command: String // "START", "STOP"
)

@JsonClass(generateAdapter = true)
data class AiDetectionResultDto(
    @Json(name = "detected") val detected: String,      // "WEED", "ONION", "UNKNOWN"
    @Json(name = "confidence") val confidence: Float,
    @Json(name = "xCenter") val xCenter: Float,
    @Json(name = "yCenter") val yCenter: Float,
    @Json(name = "baseAngle") val baseAngle: Int,
    @Json(name = "shoulderAngle") val shoulderAngle: Int,
    @Json(name = "elbowAngle") val elbowAngle: Int
)

@JsonClass(generateAdapter = true)
data class ArmPositionRequestDto(
    @Json(name = "base") val base: Int,
    @Json(name = "shoulder") val shoulder: Int,
    @Json(name = "elbow") val elbow: Int
)

@JsonClass(generateAdapter = true)
data class ActuatorTimedRequestDto(
    @Json(name = "state") val state: Boolean,
    @Json(name = "durationSec") val durationSec: Int
)
