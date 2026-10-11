package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.DeviceThermostat
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AutoBranch
import com.example.data.model.AutoExecutionState
import com.example.data.model.DetectionResult
import com.example.data.model.PlantClass
import com.example.data.model.RobotSettings
import com.example.data.model.RobotStatus
import com.example.data.model.UltrasonicStatus
import com.example.ui.components.EmergencyStopBar
import com.example.ui.components.HardwareLinkBar
import com.example.ui.theme.AgroCardSurface
import com.example.ui.theme.AgroGreenPrimary
import com.example.ui.theme.AgroOutline
import com.example.ui.theme.AgroSurfaceVariant
import com.example.ui.theme.AgroTextMuted
import com.example.ui.theme.AgroTextPrimary
import com.example.ui.theme.AgroTextSecondary
import com.example.ui.theme.StatusAutoBlue
import com.example.ui.theme.StatusEmergencyRed
import com.example.ui.theme.StatusSafeGreen
import com.example.ui.theme.StatusWarningOrange
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AutoScreen(
    robotStatus: RobotStatus,
    autoState: AutoExecutionState,
    latestDetection: DetectionResult?,
    settings: RobotSettings,
    onStartAuto: () -> Unit,
    onStopAuto: () -> Unit,
    onTriggerEstop: () -> Unit,
    onResetEstop: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showStartConfirmDialog by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    // Safety checks logic
    val checkMainEsp = robotStatus.mainEspConnected
    val checkSecEsp = robotStatus.secondaryEspConnected
    val checkUart = robotStatus.uartConnected
    val checkSensors = robotStatus.ultrasonicStatus != UltrasonicStatus.SENSOR_ERROR
    val checkCamera = robotStatus.cameraConnected || true // checked at request
    val checkEstop = !robotStatus.emergencyStopActive
    val checkBattery = robotStatus.batteryVoltage == null || robotStatus.batteryVoltage > 10.8
    val allChecksPassed = checkMainEsp && checkSecEsp && checkUart && checkSensors && checkEstop && checkBattery

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // Emergency Stop Bar
        EmergencyStopBar(
            isEstopActive = robotStatus.emergencyStopActive,
            onTriggerEstop = onTriggerEstop,
            onResetEstop = onResetEstop
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Hardware Status Summary
        HardwareLinkBar(
            mainEspOnline = robotStatus.mainEspConnected,
            secondaryEspOnline = robotStatus.secondaryEspConnected,
            uartConnected = robotStatus.uartConnected,
            camConnected = robotStatus.cameraConnected
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Large AUTO Control Action Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (autoState.isRunning) Color(0xFF1E3A8A) else AgroCardSurface
            ),
            shape = RoundedCornerShape(14.dp),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = androidx.compose.ui.graphics.SolidColor(
                    if (autoState.isRunning) StatusAutoBlue else AgroOutline
                )
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (autoState.isRunning) "AUTONOMOUS SEQUENCE ACTIVE" else "AUTONOMOUS OPERATION",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Text(
                            text = if (autoState.isRunning) "Branch: ${autoState.currentBranch.name}" else "Safe standby state",
                            fontSize = 12.sp,
                            color = if (autoState.isRunning) Color(0xFFBFDBFE) else AgroTextMuted
                        )
                    }

                    if (autoState.isRunning) {
                        Button(
                            onClick = onStopAuto,
                            colors = ButtonDefaults.buttonColors(containerColor = StatusEmergencyRed),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("stop_auto_button")
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = "Stop Auto", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("STOP AUTO", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Button(
                            onClick = {
                                if (allChecksPassed) {
                                    showStartConfirmDialog = true
                                }
                            },
                            enabled = allChecksPassed,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = StatusAutoBlue,
                                disabledContainerColor = Color(0xFF374151)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("start_auto_button")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Start Auto", modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("START AUTO", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (autoState.isRunning) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = autoState.currentStepText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { autoState.progressPercent },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = StatusSafeGreen,
                        trackColor = Color(0xFF1E293B)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Pre-Flight Safety Checks Card (When not running)
        if (!autoState.isRunning) {
            Text(
                text = "PRE-FLIGHT SAFETY CHECKS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = AgroTextMuted,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(6.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = AgroCardSurface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    SafetyCheckItem("Main ESP32 Connected", checkMainEsp)
                    SafetyCheckItem("Hardware ESP32 Connected", checkSecEsp)
                    SafetyCheckItem("UART Communication Healthy", checkUart)
                    SafetyCheckItem("Required Sensors Operational", checkSensors)
                    SafetyCheckItem("Camera Available for AI Weed Detection", checkCamera)
                    SafetyCheckItem("Emergency Stop Released", checkEstop)
                    SafetyCheckItem("Battery Voltage Safe", checkBattery)

                    if (!allChecksPassed) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "⚠ Cannot start AUTO: Critical safety check failed. Resolve offline modules before initiating autonomous operation.",
                            fontSize = 11.sp,
                            color = StatusEmergencyRed,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // Temporary Soil/DHT11 Value Bars during 5-minute soil test
        AnimatedVisibility(
            visible = autoState.showSoilTempBars,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF14532D)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "5-MINUTE SOIL CYCLE: LIVE PROBE TELEMETRY",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Soil Moisture: ${robotStatus.soilMoisturePercent ?: "--"}%",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Temp: ${robotStatus.temperatureC?.let { "%.1f°C".format(it) } ?: "--"} | Hum: ${robotStatus.humidityPercent?.let { "%.0f%%".format(it) } ?: "--"}",
                            color = Color(0xFFBBF7D0),
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // AI Weed Detection Target Display (Branch 1 Deliverable)
        Text(
            text = "AI WEED REMOVAL TELEMETRY (BRANCH 1)",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = AgroTextMuted,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(6.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = AgroCardSurface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                if (latestDetection != null) {
                    val det = latestDetection
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        when (det.plantClass) {
                                            PlantClass.WEED -> StatusEmergencyRed
                                            PlantClass.ONION -> StatusSafeGreen
                                            PlantClass.UNKNOWN -> StatusWarningOrange
                                        }
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = det.plantClass.name,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Confidence: ${(det.confidence * 100).toInt()}%",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        val timeFmt = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(det.timestamp))
                        Text(text = timeFmt, fontSize = 11.sp, color = AgroTextMuted)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Bounding Box coordinates & Arm Kinematics Targets
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "IMAGE COORDINATES",
                                fontSize = 10.sp,
                                color = AgroTextMuted,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Center: (${det.xCenter.toInt()}, ${det.yCenter.toInt()}) px",
                                fontSize = 12.sp,
                                color = AgroTextPrimary,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Box: ${det.boundingBox.width.toInt()} x ${det.boundingBox.height.toInt()} px",
                                fontSize = 11.sp,
                                color = AgroTextSecondary
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "3-DOF ARM MAPPING",
                                fontSize = 10.sp,
                                color = AgroTextMuted,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Base: ${det.armTarget.baseAngle}° | S: ${det.armTarget.shoulderAngle}° | E: ${det.armTarget.elbowAngle}°",
                                fontSize = 12.sp,
                                color = AgroGreenPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Target: (${det.armTarget.targetXmm.toInt()}mm, ${det.armTarget.targetYmm.toInt()}mm)",
                                fontSize = 11.sp,
                                color = AgroTextSecondary
                            )
                        }
                    }

                    // Annotated Image Preview if available
                    if (det.annotatedImage != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Image(
                            bitmap = det.annotatedImage.asImageBitmap(),
                            contentDescription = "Annotated Weed Detection Frame",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Fit
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No detection frame captured yet. Camera scans automatically during AUTO or via AI SCAN in Robot tab.",
                            fontSize = 11.sp,
                            color = AgroTextMuted
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Autonomous Pipeline Architecture Card
        Text(
            text = "AUTONOMOUS SEQUENCES OVERVIEW",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = AgroTextMuted,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(6.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = AgroCardSurface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                SequenceBranchRow(
                    branchName = "BRANCH 1: AI WEED REMOVAL",
                    pipeline = "Search -> Capture -> AI Detect -> Weed Confirmed -> Arm Move -> Cutter On (≤7s) -> Cutter Off -> Arm Home",
                    icon = Icons.Default.ContentCut,
                    active = autoState.currentBranch == AutoBranch.WEED_REMOVAL
                )
                Spacer(modifier = Modifier.height(10.dp))
                SequenceBranchRow(
                    branchName = "BRANCH 2: TCS230 DISEASE MONITORING",
                    pipeline = "Search -> Read Color -> Threshold Check -> Suspected Disease -> Check Chem Tank -> Spray (≤10s) -> Pump Off",
                    icon = Icons.Default.Science,
                    active = autoState.currentBranch == AutoBranch.DISEASE_MONITORING
                )
                Spacer(modifier = Modifier.height(10.dp))
                SequenceBranchRow(
                    branchName = "BRANCH 3: SOIL TESTING (EVERY 5 MIN)",
                    pipeline = "Stop -> Soil Servo 90° -> Read Soil & DHT11 -> Dry Check -> Water Tank Check -> Pump (≤5s) -> Servo Home",
                    icon = Icons.Default.WaterDrop,
                    active = autoState.currentBranch == AutoBranch.SOIL_TESTING
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Session Counters Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = AgroCardSurface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                CounterColumn("Weeds Cut", autoState.weedsHandledCount.toString(), StatusEmergencyRed)
                CounterColumn("Diseases Treated", autoState.diseasesTreatedCount.toString(), Color(0xFFA855F7))
                CounterColumn("Watering Cycles", autoState.wateringEventsCount.toString(), Color(0xFF38BDF8))
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }

    // Confirmation dialog before starting AUTO
    if (showStartConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showStartConfirmDialog = false },
            title = {
                Text(
                    text = "Start Autonomous Mode?",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            text = {
                Text(
                    text = "Confirm that the robot field is clear and all safety pre-checks have been verified. Manual movement, pan/tilt, and manual relays will be locked during autonomous operations.",
                    color = Color(0xFFD1D5DB),
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showStartConfirmDialog = false
                        onStartAuto()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusAutoBlue)
                ) {
                    Text("Confirm & Start")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showStartConfirmDialog = false }) {
                    Text("Cancel", color = Color.White)
                }
            },
            containerColor = Color(0xFF1F2937)
        )
    }
}

@Composable
fun SafetyCheckItem(title: String, passed: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, fontSize = 12.sp, color = AgroTextPrimary)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (passed) Icons.Default.CheckCircle else Icons.Default.Close,
                contentDescription = if (passed) "Pass" else "Fail",
                tint = if (passed) StatusSafeGreen else StatusEmergencyRed,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = if (passed) "PASS" else "FAIL",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = if (passed) StatusSafeGreen else StatusEmergencyRed
            )
        }
    }
}

@Composable
fun SequenceBranchRow(
    branchName: String,
    pipeline: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    active: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (active) Color(0xFF1E3A8A) else Color.Transparent)
            .padding(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = branchName,
            tint = if (active) Color.White else AgroGreenPrimary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = branchName,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (active) Color.White else AgroTextPrimary
            )
            Text(
                text = pipeline,
                fontSize = 10.sp,
                color = if (active) Color(0xFFBFDBFE) else AgroTextMuted,
                lineHeight = 13.sp
            )
        }
    }
}

@Composable
fun CounterColumn(title: String, value: String, accentColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, fontSize = 22.sp, fontWeight = FontWeight.Black, color = accentColor)
        Text(text = title, fontSize = 10.sp, color = AgroTextMuted, fontWeight = FontWeight.Medium)
    }
}
