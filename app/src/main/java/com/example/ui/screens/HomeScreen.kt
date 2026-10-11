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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.DeviceThermostat
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.InvertColors
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MovementCommand
import com.example.data.model.RobotMode
import com.example.data.model.RobotSettings
import com.example.data.model.RobotStatus
import com.example.data.model.UltrasonicStatus
import com.example.ui.components.EmergencyStopBar
import com.example.ui.components.HardwareLinkBar
import com.example.ui.components.LevelGaugeBar
import com.example.ui.components.TelemetryItemCard
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

@Composable
fun HomeScreen(
    robotStatus: RobotStatus,
    settings: RobotSettings,
    onTriggerEstop: () -> Unit,
    onResetEstop: () -> Unit,
    onNavigateToRobot: () -> Unit,
    onNavigateToAuto: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // App Header & Tagline
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "AI COMPANION",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "Smart Onion Garden Maintainer Robot",
                    fontSize = 12.sp,
                    color = AgroGreenPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "“See the Weed. Protect the Onion. Work Smart.”",
                    fontSize = 11.sp,
                    color = AgroTextMuted,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            // Mode Badge
            val modeBg = when (robotStatus.mode) {
                RobotMode.AUTO -> StatusAutoBlue
                RobotMode.EMERGENCY_STOP -> StatusEmergencyRed
                RobotMode.MANUAL -> Color(0xFF27442D)
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(modeBg)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = robotStatus.mode.name,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Prominent Emergency Stop Component
        EmergencyStopBar(
            isEstopActive = robotStatus.emergencyStopActive,
            onTriggerEstop = onTriggerEstop,
            onResetEstop = onResetEstop
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Hardware Network & UART Telemetry
        HardwareLinkBar(
            mainEspOnline = robotStatus.mainEspConnected,
            secondaryEspOnline = robotStatus.secondaryEspConnected,
            uartConnected = robotStatus.uartConnected,
            camConnected = robotStatus.cameraConnected
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Movement & Drive Status Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = AgroCardSurface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(AgroSurfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DirectionsRun,
                            contentDescription = "Movement",
                            tint = if (robotStatus.currentMovement != MovementCommand.STOP) StatusSafeGreen else AgroTextMuted
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "DRIVE STATUS",
                            fontSize = 11.sp,
                            color = AgroTextMuted,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = if (robotStatus.currentMovement == MovementCommand.STOP) "STOPPED" else robotStatus.currentMovement.name,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (robotStatus.currentMovement != MovementCommand.STOP) StatusSafeGreen else Color.White
                        )
                    }
                }

                // Speed Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF263329))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "SPEED: ${robotStatus.speed.name}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = AgroGreenPrimary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Primary Sensor Telemetry Grid (2 columns)
        Text(
            text = "LIVE TELEMETRY",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = AgroTextSecondary,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            // Front Ultrasonic
            val dist = robotStatus.frontUltrasonicDistanceCm
            val distText = if (dist != null) "%.1f".format(dist) else "--"
            val distColor = when (robotStatus.ultrasonicStatus) {
                UltrasonicStatus.OBSTACLE_DETECTED -> StatusEmergencyRed
                UltrasonicStatus.SENSOR_ERROR -> StatusWarningOrange
                UltrasonicStatus.PATH_CLEAR -> StatusSafeGreen
            }
            TelemetryItemCard(
                title = "FRONT ULTRASONIC",
                value = distText,
                unit = if (dist != null) "cm" else "",
                icon = Icons.Default.Radar,
                statusColor = distColor,
                subtitle = robotStatus.ultrasonicStatus.name.replace("_", " "),
                modifier = Modifier.weight(1f)
            )

            // Soil Moisture
            val soil = robotStatus.soilMoisturePercent
            val soilText = if (soil != null) "$soil" else "--"
            TelemetryItemCard(
                title = "SOIL MOISTURE",
                value = soilText,
                unit = if (soil != null) "%" else "",
                icon = Icons.Default.Opacity,
                statusColor = if (soil != null && soil < settings.soilDryThresholdPercent) StatusWarningOrange else StatusSafeGreen,
                subtitle = robotStatus.soilState.name,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            // Temperature & Humidity (DHT11)
            val temp = robotStatus.temperatureC
            val hum = robotStatus.humidityPercent
            val tempText = if (temp != null) "%.1f".format(temp) else "--"
            val humText = if (hum != null) "${hum.toInt()}% RH" else ""
            TelemetryItemCard(
                title = "DHT11 CLIMATE",
                value = tempText,
                unit = if (temp != null) "°C" else "",
                icon = Icons.Default.DeviceThermostat,
                statusColor = AgroGreenPrimary,
                subtitle = humText,
                modifier = Modifier.weight(1f)
            )

            // Battery Voltage
            val batt = robotStatus.batteryVoltage
            val battText = if (batt != null) "%.1f".format(batt) else "--"
            TelemetryItemCard(
                title = "BATTERY VOLTAGE",
                value = battText,
                unit = if (batt != null) "V" else "",
                icon = Icons.Default.BatteryChargingFull,
                statusColor = if (batt != null && batt > 11.4) StatusSafeGreen else StatusWarningOrange,
                subtitle = if (batt != null) if (batt > 11.8) "HEALTHY" else "LOW" else "UNINSTALLED",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Tank Levels Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = AgroCardSurface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "RESERVOIR LEVELS (HC-SR04 SENSORS)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = AgroTextMuted
                )
                Spacer(modifier = Modifier.height(10.dp))
                LevelGaugeBar(
                    label = "Water Reservoir",
                    currentPercent = robotStatus.waterTankLevelPercent,
                    minThresholdPercent = settings.waterTankMinPercent,
                    accentColor = Color(0xFF38BDF8)
                )
                Spacer(modifier = Modifier.height(12.dp))
                LevelGaugeBar(
                    label = "Chemical Treatment Tank",
                    currentPercent = robotStatus.chemicalTankLevelPercent,
                    minThresholdPercent = settings.chemicalTankMinPercent,
                    accentColor = Color(0xFFA855F7)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Actuators & Arm Status Summary
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = AgroCardSurface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "ACTUATOR & 3-DOF ARM TELEMETRY",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = AgroTextMuted
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    ActuatorStatusBadge(
                        label = "CUTTER",
                        isActive = robotStatus.cutterActive,
                        icon = Icons.Default.ContentCut
                    )
                    ActuatorStatusBadge(
                        label = "WATER PUMP",
                        isActive = robotStatus.waterPumpActive,
                        icon = Icons.Default.WaterDrop
                    )
                    ActuatorStatusBadge(
                        label = "CHEM PUMP",
                        isActive = robotStatus.chemicalPumpActive,
                        icon = Icons.Default.Science
                    )
                    ActuatorStatusBadge(
                        label = "RELAY 1",
                        isActive = robotStatus.relay1Active,
                        icon = Icons.Default.ElectricBolt
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Arm Status: ${robotStatus.armStatus.name} | Base: ${robotStatus.armBaseAngle}° | Shoulder: ${robotStatus.armShoulderAngle}° | Elbow: ${robotStatus.armElbowAngle}°",
                    fontSize = 11.sp,
                    color = AgroTextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Quick Navigation Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onNavigateToRobot,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("nav_manual_button"),
                colors = ButtonDefaults.buttonColors(containerColor = AgroSurfaceVariant),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Build, contentDescription = "Manual Robot", modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("MANUAL CONTROL")
            }

            Button(
                onClick = onNavigateToAuto,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("nav_auto_button"),
                colors = ButtonDefaults.buttonColors(containerColor = StatusAutoBlue),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Auto Mode", modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("AUTO MODE")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun ActuatorStatusBadge(
    label: String,
    isActive: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isActive) StatusEmergencyRed else AgroTextMuted,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
            color = AgroTextMuted
        )
        Text(
            text = if (isActive) "ACTIVE" else "OFF",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = if (isActive) StatusEmergencyRed else StatusSafeGreen
        )
    }
}
