package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.EventLogEntity
import com.example.data.model.RobotSettings
import com.example.data.model.RobotStatus
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
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(
    robotStatus: RobotStatus,
    settings: RobotSettings,
    eventLogs: List<EventLogEntity>,
    onSaveSettings: (RobotSettings) -> Unit,
    onTestEsp: suspend (RobotSettings) -> Result<Boolean>,
    onTestCam: suspend (RobotSettings) -> Result<Boolean>,
    onClearLogs: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    // Local state for settings form
    var mainEspIp by remember(settings) { mutableStateOf(settings.mainEspIp) }
    var mainEspPort by remember(settings) { mutableStateOf(settings.mainEspPort.toString()) }
    var camIp by remember(settings) { mutableStateOf(settings.camIp) }
    var camStreamPort by remember(settings) { mutableStateOf(settings.camStreamPort.toString()) }
    var camStreamUrl by remember(settings) { mutableStateOf(settings.camStreamUrl) }
    var camCaptureUrl by remember(settings) { mutableStateOf(settings.camCaptureUrl) }

    var obstacleThreshold by remember(settings) { mutableStateOf(settings.obstacleThresholdCm.toString()) }
    var weedConfidence by remember(settings) { mutableStateOf((settings.weedConfidenceThreshold * 100).toInt().toString()) }
    var soilDryThreshold by remember(settings) { mutableStateOf(settings.soilDryThresholdPercent.toString()) }
    var chemTankMin by remember(settings) { mutableStateOf(settings.chemicalTankMinPercent.toString()) }
    var waterTankMin by remember(settings) { mutableStateOf(settings.waterTankMinPercent.toString()) }

    var cutterTimeout by remember(settings) { mutableStateOf(settings.cutterTimeoutSec.toString()) }
    var sprayerTimeout by remember(settings) { mutableStateOf(settings.sprayerTimeoutSec.toString()) }
    var waterPumpTimeout by remember(settings) { mutableStateOf(settings.waterPumpTimeoutSec.toString()) }

    var calibScaleX by remember(settings) { mutableStateOf(settings.calibScaleX.toString()) }
    var calibScaleY by remember(settings) { mutableStateOf(settings.calibScaleY.toString()) }
    var calibOffsetX by remember(settings) { mutableStateOf(settings.calibOffsetX.toString()) }
    var calibOffsetY by remember(settings) { mutableStateOf(settings.calibOffsetY.toString()) }

    var testStatusText by remember { mutableStateOf<String?>(null) }
    var isTestingEsp by remember { mutableStateOf(false) }
    var isTestingCam by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "SYSTEM SETTINGS & LOGS",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
            color = Color.White
        )
        Text(
            text = "Configurable network endpoints, safety limits, calibration, and event logs",
            fontSize = 12.sp,
            color = AgroTextMuted
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Settings Tabs
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = AgroSurfaceVariant,
            contentColor = AgroGreenPrimary,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = AgroGreenPrimary
                )
            }
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Network & Hardware", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Thresholds & Calib", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("Event Logs (${eventLogs.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        when (selectedTab) {
            0 -> {
                // Tab 0: Network & Hardware Settings
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        HardwareLinkBar(
                            mainEspOnline = robotStatus.mainEspConnected,
                            secondaryEspOnline = robotStatus.secondaryEspConnected,
                            uartConnected = robotStatus.uartConnected,
                            camConnected = robotStatus.cameraConnected
                        )
                    }

                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = AgroCardSurface),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "ESP32 #1 (MAIN CONTROLLER) NETWORK",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AgroTextMuted
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    CustomTextField(
                                        label = "Main ESP32 IP",
                                        value = mainEspIp,
                                        onValueChange = { mainEspIp = it },
                                        modifier = Modifier.weight(2f)
                                    )
                                    CustomTextField(
                                        label = "HTTP Port",
                                        value = mainEspPort,
                                        onValueChange = { mainEspPort = it },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = AgroCardSurface),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "AI-THINKER ESP32-CAM NETWORK",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AgroTextMuted
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    CustomTextField(
                                        label = "Camera IP Address",
                                        value = camIp,
                                        onValueChange = {
                                            camIp = it
                                            camStreamUrl = "http://$it:$camStreamPort/stream"
                                            camCaptureUrl = "http://$it/capture"
                                        },
                                        modifier = Modifier.weight(2f)
                                    )
                                    CustomTextField(
                                        label = "Stream Port",
                                        value = camStreamPort,
                                        onValueChange = {
                                            camStreamPort = it
                                            camStreamUrl = "http://$camIp:$it/stream"
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                CustomTextField(
                                    label = "Camera Stream URL (MJPEG)",
                                    value = camStreamUrl,
                                    onValueChange = { camStreamUrl = it },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                CustomTextField(
                                    label = "Camera Capture URL (JPEG)",
                                    value = camCaptureUrl,
                                    onValueChange = { camCaptureUrl = it },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    item {
                        // Diagnostic test buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        isTestingEsp = true
                                        testStatusText = "Pinging Main ESP32..."
                                        val tempSettings = settings.copy(
                                            mainEspIp = mainEspIp,
                                            mainEspPort = mainEspPort.toIntOrNull() ?: 80
                                        )
                                        val res = onTestEsp(tempSettings)
                                        isTestingEsp = false
                                        testStatusText = if (res.isSuccess) "✓ Main ESP32 Connected Successfully"
                                        else "✗ ESP32 Ping Failed: ${res.exceptionOrNull()?.localizedMessage}"
                                    }
                                },
                                modifier = Modifier.weight(1f).height(46.dp).testTag("test_esp_button")
                            ) {
                                Icon(Icons.Default.NetworkCheck, contentDescription = "Test ESP", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Test ESP32 #1", fontSize = 11.sp)
                            }

                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        isTestingCam = true
                                        testStatusText = "Pinging ESP32-CAM..."
                                        val tempSettings = settings.copy(
                                            camIp = camIp,
                                            camCaptureUrl = camCaptureUrl
                                        )
                                        val res = onTestCam(tempSettings)
                                        isTestingCam = false
                                        testStatusText = if (res.isSuccess) "✓ ESP32-CAM Reached Successfully"
                                        else "✗ Camera Test Failed: ${res.exceptionOrNull()?.localizedMessage}"
                                    }
                                },
                                modifier = Modifier.weight(1f).height(46.dp).testTag("test_cam_button")
                            ) {
                                Icon(Icons.Default.Videocam, contentDescription = "Test Cam", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Test Camera", fontSize = 11.sp)
                            }
                        }
                    }

                    if (testStatusText != null) {
                        item {
                            Text(
                                text = testStatusText!!,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (testStatusText!!.startsWith("✓")) StatusSafeGreen else StatusEmergencyRed
                            )
                        }
                    }

                    item {
                        // Firmware & UART Health Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = AgroCardSurface),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "FIRMWARE & UART ARCHITECTURE",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AgroTextMuted
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Main ESP32 Firmware: ${robotStatus.firmwareVersionMain}",
                                    fontSize = 12.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = "Hardware ESP32 Firmware: ${robotStatus.firmwareVersionSecondary}",
                                    fontSize = 12.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = "UART Link Status: ${if (robotStatus.uartConnected) "HEALTHY (ACK received)" else "ERROR / TIMEOUT"}",
                                    fontSize = 12.sp,
                                    color = if (robotStatus.uartConnected) StatusSafeGreen else StatusEmergencyRed,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    item {
                        Button(
                            onClick = {
                                val updated = settings.copy(
                                    mainEspIp = mainEspIp,
                                    mainEspPort = mainEspPort.toIntOrNull() ?: 80,
                                    camIp = camIp,
                                    camStreamPort = camStreamPort.toIntOrNull() ?: 81,
                                    camStreamUrl = camStreamUrl,
                                    camCaptureUrl = camCaptureUrl
                                )
                                onSaveSettings(updated)
                                testStatusText = "✓ Settings Saved Successfully"
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AgroGreenPrimary),
                            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("save_network_button")
                        ) {
                            Icon(Icons.Default.Save, contentDescription = "Save Settings", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("SAVE NETWORK SETTINGS", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            1 -> {
                // Tab 1: Safety Thresholds & Arm Calibration
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = AgroCardSurface),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "SAFETY & OPERATIONAL THRESHOLDS",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AgroTextMuted
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    CustomTextField(
                                        label = "Obstacle Dist (cm)",
                                        value = obstacleThreshold,
                                        onValueChange = { obstacleThreshold = it },
                                        modifier = Modifier.weight(1f)
                                    )
                                    CustomTextField(
                                        label = "Weed Conf (%)",
                                        value = weedConfidence,
                                        onValueChange = { weedConfidence = it },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    CustomTextField(
                                        label = "Soil Dry Thresh (%)",
                                        value = soilDryThreshold,
                                        onValueChange = { soilDryThreshold = it },
                                        modifier = Modifier.weight(1f)
                                    )
                                    CustomTextField(
                                        label = "Min Water Tank (%)",
                                        value = waterTankMin,
                                        onValueChange = { waterTankMin = it },
                                        modifier = Modifier.weight(1f)
                                    )
                                    CustomTextField(
                                        label = "Min Chem Tank (%)",
                                        value = chemTankMin,
                                        onValueChange = { chemTankMin = it },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = AgroCardSurface),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "ACTUATOR HARDWARE TIMEOUTS (FIRMWARE ENFORCED)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AgroTextMuted
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    CustomTextField(
                                        label = "Cutter (max 7s)",
                                        value = cutterTimeout,
                                        onValueChange = { cutterTimeout = it },
                                        modifier = Modifier.weight(1f)
                                    )
                                    CustomTextField(
                                        label = "Sprayer (max 10s)",
                                        value = sprayerTimeout,
                                        onValueChange = { sprayerTimeout = it },
                                        modifier = Modifier.weight(1f)
                                    )
                                    CustomTextField(
                                        label = "Water Pump (max 5s)",
                                        value = waterPumpTimeout,
                                        onValueChange = { waterPumpTimeout = it },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = AgroCardSurface),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "CAMERA-TO-ARM KINEMATICS CALIBRATION",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AgroTextMuted
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    CustomTextField("Scale X", calibScaleX, { calibScaleX = it }, Modifier.weight(1f))
                                    CustomTextField("Scale Y", calibScaleY, { calibScaleY = it }, Modifier.weight(1f))
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    CustomTextField("Offset X (mm)", calibOffsetX, { calibOffsetX = it }, Modifier.weight(1f))
                                    CustomTextField("Offset Y (mm)", calibOffsetY, { calibOffsetY = it }, Modifier.weight(1f))
                                }
                            }
                        }
                    }

                    item {
                        Button(
                            onClick = {
                                val updated = settings.copy(
                                    obstacleThresholdCm = obstacleThreshold.toDoubleOrNull() ?: 25.0,
                                    weedConfidenceThreshold = ((weedConfidence.toFloatOrNull() ?: 65f) / 100f).coerceIn(0.5f, 0.95f),
                                    soilDryThresholdPercent = (soilDryThreshold.toIntOrNull() ?: 30).coerceIn(10, 80),
                                    waterTankMinPercent = (waterTankMin.toIntOrNull() ?: 15).coerceIn(5, 50),
                                    chemicalTankMinPercent = (chemTankMin.toIntOrNull() ?: 15).coerceIn(5, 50),
                                    cutterTimeoutSec = (cutterTimeout.toIntOrNull() ?: 7).coerceIn(1, 7),
                                    sprayerTimeoutSec = (sprayerTimeout.toIntOrNull() ?: 10).coerceIn(1, 10),
                                    waterPumpTimeoutSec = (waterPumpTimeout.toIntOrNull() ?: 5).coerceIn(1, 5),
                                    calibScaleX = calibScaleX.toFloatOrNull() ?: 0.45f,
                                    calibScaleY = calibScaleY.toFloatOrNull() ?: 0.40f,
                                    calibOffsetX = calibOffsetX.toFloatOrNull() ?: 0.0f,
                                    calibOffsetY = calibOffsetY.toFloatOrNull() ?: 120.0f
                                )
                                onSaveSettings(updated)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AgroGreenPrimary),
                            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("save_thresholds_button")
                        ) {
                            Icon(Icons.Default.Save, contentDescription = "Save Thresholds", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("SAVE THRESHOLDS & CALIBRATION", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            2 -> {
                // Tab 2: Timestamped Event Logs
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "PERSISTENT OPERATION AUDIT LOG",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = AgroTextMuted
                        )
                        OutlinedButton(
                            onClick = onClearLogs,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusEmergencyRed),
                            modifier = Modifier.testTag("clear_logs_button")
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Clear", modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Clear Logs", fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (eventLogs.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No operational events logged yet.", color = AgroTextMuted, fontSize = 13.sp)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(eventLogs, key = { it.id }) { log ->
                                EventLogItemRow(log)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CustomTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 11.sp) },
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AgroGreenPrimary,
            unfocusedBorderColor = AgroOutline,
            focusedTextColor = Color.White,
            unfocusedTextColor = AgroTextPrimary,
            focusedLabelColor = AgroGreenPrimary,
            unfocusedLabelColor = AgroTextMuted
        ),
        modifier = modifier
    )
}

@Composable
fun EventLogItemRow(log: EventLogEntity) {
    val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(log.timestamp))
    val statusColor = when (log.status) {
        "TRIGGERED", "ERROR", "CHEMICAL_LOW", "WATER_LOW", "SUSPECTED" -> StatusEmergencyRed
        "SUCCESS", "RESET", "ACTIVE", "SPRAYING", "PUMP_ON", "AUTO" -> StatusSafeGreen
        else -> AgroTextPrimary
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AgroCardSurface),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = log.eventType, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = statusColor)
                Text(text = dateStr, fontSize = 10.sp, color = AgroTextMuted)
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = log.details, fontSize = 11.sp, color = AgroTextPrimary)
            if (log.measuredValues.isNotEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Values: ${log.measuredValues}",
                    fontSize = 10.sp,
                    color = AgroGreenPrimary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
