package com.example.data.vision

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.example.data.model.ArmTarget
import com.example.data.model.BoundingBox
import com.example.data.model.DetectionResult
import com.example.data.model.PlantClass
import com.example.data.model.RobotSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

class WeedDetector {

    /**
     * Processes camera image using agricultural computer vision segmentation
     * Detects WEED, ONION, or UNKNOWN with precise bounding box and physical arm coordinates.
     */
    suspend fun analyzeImage(bitmap: Bitmap, settings: RobotSettings): DetectionResult = withContext(Dispatchers.Default) {
        val width = bitmap.width
        val height = bitmap.height

        // Downsample for fast and robust vegetation segmentation
        val sampleScale = 4
        val sampleW = width / sampleScale
        val sampleH = height / sampleScale
        val scaled = Bitmap.createScaledBitmap(bitmap, sampleW, sampleH, false)

        var plantPixelCount = 0
        var minX = sampleW
        var maxX = 0
        var minY = sampleH
        var maxY = 0

        var sumHue = 0.0
        var onionFeatureScore = 0f
        var weedFeatureScore = 0f

        val hsv = FloatArray(3)

        for (y in 0 until sampleH) {
            for (x in 0 until sampleW) {
                val pixel = scaled.getPixel(x, y)
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)

                // Excess Green Index (ExG = 2*G - R - B) common in agricultural robotics
                val exG = 2 * g - r - b

                if (exG > 18 && g > 45) {
                    plantPixelCount++
                    Color.RGBToHSV(r, g, b, hsv)
                    sumHue += hsv[0]

                    if (x < minX) minX = x
                    if (x > maxX) maxX = x
                    if (y < minY) minY = y
                    if (y > maxY) maxY = y

                    // Onion characteristics: upright vertical narrow structure, darker blue-green (hue 80-140)
                    // Weed characteristics: wider spread, bright yellowish green or irregular clusters
                    if (hsv[0] in 95f..145f && hsv[1] > 0.35f) {
                        onionFeatureScore += 1f
                    } else if (hsv[0] in 55f..94f || hsv[2] > 0.65f) {
                        weedFeatureScore += 1.2f
                    }
                }
            }
        }

        // Check if sufficient plant matter was detected
        val totalSamplePixels = sampleW * sampleH
        val plantCoverageRatio = plantPixelCount.toFloat() / totalSamplePixels

        if (plantPixelCount < 60 || plantCoverageRatio < 0.015f) {
            // No significant vegetation detected in frame
            val emptyBox = BoundingBox(width * 0.4f, height * 0.4f, width * 0.2f, height * 0.2f)
            val armTarget = calculateArmKinematics(emptyBox.xCenter, emptyBox.yCenter, width, height, settings)
            return@withContext DetectionResult(
                plantClass = PlantClass.UNKNOWN,
                confidence = 0.35f,
                boundingBox = emptyBox,
                armTarget = armTarget,
                rawImage = bitmap,
                annotatedImage = drawAnnotations(bitmap, emptyBox, PlantClass.UNKNOWN, 0.35f, armTarget)
            )
        }

        // Scale bounding box back to full resolution
        val boxLeft = (minX * sampleScale).toFloat().coerceIn(0f, width.toFloat() - 20f)
        val boxTop = (minY * sampleScale).toFloat().coerceIn(0f, height.toFloat() - 20f)
        val boxWidth = ((maxX - minX + 1) * sampleScale).toFloat().coerceAtLeast(40f).coerceAtMost(width.toFloat() - boxLeft)
        val boxHeight = ((maxY - minY + 1) * sampleScale).toFloat().coerceAtLeast(40f).coerceAtMost(height.toFloat() - boxTop)
        val box = BoundingBox(boxLeft, boxTop, boxWidth, boxHeight)

        // Aspect ratio (height/width): Onions are tall and narrow (aspect ratio > 1.4)
        // Weeds are broad, horizontal or spread out (aspect ratio <= 1.2)
        val aspectRatio = boxHeight / boxWidth.coerceAtLeast(1f)

        val detectedClass: PlantClass
        val confidence: Float

        if (aspectRatio > 1.35f && onionFeatureScore >= weedFeatureScore * 0.9f) {
            detectedClass = PlantClass.ONION
            confidence = (0.75f + (aspectRatio * 0.05f)).coerceIn(0.70f, 0.96f)
        } else if (weedFeatureScore > onionFeatureScore * 0.8f || aspectRatio < 1.15f) {
            detectedClass = PlantClass.WEED
            confidence = (0.72f + (plantCoverageRatio * 0.4f)).coerceIn(0.68f, 0.94f)
        } else {
            detectedClass = PlantClass.UNKNOWN
            confidence = 0.50f
        }

        val armTarget = calculateArmKinematics(box.xCenter, box.yCenter, width, height, settings)
        val annotated = drawAnnotations(bitmap, box, detectedClass, confidence, armTarget)

        DetectionResult(
            plantClass = detectedClass,
            confidence = confidence,
            boundingBox = box,
            armTarget = armTarget,
            rawImage = bitmap,
            annotatedImage = annotated
        )
    }

    /**
     * Maps camera pixel coordinates (Xcenter, Ycenter) to 3-DOF robot arm servo angles
     * Xcenter = X + Width / 2
     * Ycenter = Y + Height / 2
     */
    fun calculateArmKinematics(
        xCenter: Float,
        yCenter: Float,
        imageWidth: Int,
        imageHeight: Int,
        settings: RobotSettings
    ): ArmTarget {
        // Normalized offsets from image center (-1.0 .. +1.0)
        val normX = (xCenter - (imageWidth / 2f)) / (imageWidth / 2f)
        // Y from camera bottom (0 = ground near robot, 1 = farther horizon)
        val normY = (imageHeight - yCenter) / imageHeight.toFloat()

        // Physical ground plane coordinates in mm
        val targetXmm = (normX * 160f * settings.calibScaleX) + settings.calibOffsetX
        val targetYmm = (normY * 180f * settings.calibScaleY) + settings.calibOffsetY

        // Base servo angle (centered at 90 deg)
        val baseAngleRad = atan2(targetXmm.toDouble(), targetYmm.toDouble())
        val baseAngleDeg = 90 + Math.toDegrees(baseAngleRad).toInt()
        val clampedBase = baseAngleDeg.coerceIn(settings.armBaseMinAngle, settings.armBaseMaxAngle)

        // 2-Link planar arm reach (Link 1 = L1 = 120mm, Link 2 = L2 = 130mm)
        val groundDistance = hypot(targetXmm.toDouble(), targetYmm.toDouble())
        val zHeight = 35.0 // ground cutter clearance height mm
        val reachDistance = hypot(groundDistance, zHeight).coerceIn(90.0, 240.0)

        // Law of cosines for inverse kinematics
        val l1 = 120.0
        val l2 = 130.0
        val cosElbow = ((reachDistance * reachDistance) - (l1 * l1) - (l2 * l2)) / (2 * l1 * l2)
        val elbowAngleRad = Math.acos(cosElbow.coerceIn(-1.0, 1.0))

        val shoulderAngleRad = atan2(zHeight, groundDistance) +
                atan2(l2 * sin(elbowAngleRad), l1 + l2 * cos(elbowAngleRad))

        val shoulderDeg = Math.toDegrees(shoulderAngleRad).toInt().coerceIn(settings.armShoulderMinAngle, settings.armShoulderMaxAngle)
        val elbowDeg = Math.toDegrees(elbowAngleRad).toInt().coerceIn(settings.armElbowMinAngle, settings.armElbowMaxAngle)

        return ArmTarget(
            baseAngle = clampedBase,
            shoulderAngle = shoulderDeg,
            elbowAngle = elbowDeg,
            targetXmm = targetXmm,
            targetYmm = targetYmm
        )
    }

    private fun drawAnnotations(
        source: Bitmap,
        box: BoundingBox,
        plantClass: PlantClass,
        confidence: Float,
        armTarget: ArmTarget
    ): Bitmap {
        val annotated = source.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(annotated)

        val boxPaint = Paint().apply {
            style = Paint.Style.STROKE
            strokeWidth = 6f
            color = when (plantClass) {
                PlantClass.WEED -> Color.parseColor("#EF4444")   // Red for WEED target
                PlantClass.ONION -> Color.parseColor("#22C55E")  // Green for protected ONION
                PlantClass.UNKNOWN -> Color.parseColor("#F59E0B")// Amber for UNKNOWN
            }
            isAntiAlias = true
        }

        // Draw Bounding Box
        val rect = RectF(box.left, box.top, box.left + box.width, box.top + box.height)
        canvas.drawRect(rect, boxPaint)

        // Draw Center Target Crosshair
        val crossPaint = Paint().apply {
            color = boxPaint.color
            strokeWidth = 4f
            style = Paint.Style.STROKE
        }
        val crossSize = 20f
        canvas.drawLine(box.xCenter - crossSize, box.yCenter, box.xCenter + crossSize, box.yCenter, crossPaint)
        canvas.drawLine(box.xCenter, box.yCenter - crossSize, box.xCenter, box.yCenter + crossSize, crossPaint)

        // Draw Label Tag
        val tagBgPaint = Paint().apply {
            color = Color.parseColor("#CC111827")
            style = Paint.Style.FILL
        }
        val textPaint = Paint().apply {
            color = Color.WHITE
            textSize = 28f
            isFakeBoldText = true
            isAntiAlias = true
        }

        val labelText = "${plantClass.name} ${(confidence * 100).toInt()}%"
        val coordsText = "Center: (${box.xCenter.toInt()}, ${box.yCenter.toInt()}) | Arm: B${armTarget.baseAngle}° S${armTarget.shoulderAngle}°"

        val tagHeight = 70f
        val tagWidth = 420f
        canvas.drawRoundRect(box.left, (box.top - tagHeight).coerceAtLeast(0f), box.left + tagWidth, box.top.coerceAtLeast(tagHeight), 8f, 8f, tagBgPaint)
        canvas.drawText(labelText, box.left + 12f, (box.top - 38f).coerceAtLeast(32f), textPaint)
        
        val subTextPaint = Paint().apply {
            color = Color.parseColor("#A3E635")
            textSize = 20f
            isAntiAlias = true
        }
        canvas.drawText(coordsText, box.left + 12f, (box.top - 12f).coerceAtLeast(58f), subTextPaint)

        return annotated
    }
}
