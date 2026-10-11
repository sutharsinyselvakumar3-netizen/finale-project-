package com.example.data.model

import android.graphics.Bitmap

enum class PlantClass {
    WEED,
    ONION,
    UNKNOWN
}

data class BoundingBox(
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float
) {
    val xCenter: Float get() = left + (width / 2f)
    val yCenter: Float get() = top + (height / 2f)
}

data class ArmTarget(
    val baseAngle: Int,
    val shoulderAngle: Int,
    val elbowAngle: Int,
    val targetXmm: Float,
    val targetYmm: Float
)

data class DetectionResult(
    val plantClass: PlantClass,
    val confidence: Float,
    val boundingBox: BoundingBox,
    val xCenter: Float = boundingBox.xCenter,
    val yCenter: Float = boundingBox.yCenter,
    val armTarget: ArmTarget,
    val timestamp: Long = System.currentTimeMillis(),
    val rawImage: Bitmap? = null,
    val annotatedImage: Bitmap? = null
)
