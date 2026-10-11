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
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DeviceThermostat
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.PrecisionManufacturing
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Water
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DiseaseStatus
import com.example.data.model.RobotSettings
import com.example.data.model.RobotStatus
import com.example.data.model.UltrasonicStatus
import com.example.ui.components.LevelGaugeBar
import com.example.ui.components.TelemetryItemCard
import com.example.ui.theme.AgroCardSurface
import com.example.ui.theme.AgroGreenPrimary
import com.example.ui.theme.AgroOutline
import com.example.ui.theme.AgroSurfaceVariant
import com.example.ui.theme.AgroTextMuted
import com.example.ui.theme.AgroTextPrimary
import com.example.ui.theme.AgroTextSecondary
import com.example.ui.theme.StatusEmergencyRed
import com.example.ui.theme.StatusSafeGreen
import com.example.ui.theme.StatusWarningOrange

@Composable
fun SensorsScreen(
    robotStatus: RobotStatus,
    settings: RobotSettings,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        Text(
            text = "SENSOR TELEMETRY & DIAGNOSTICS",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
            color = Color.White
        )
        Text(
            text = "Real-time readings from ESP32 #1 and secondary ESP32 hardware sensors",
            fontSize = 12.sp,
            color = AgroTextMuted
        )

        Spacer(modifier = Modifier.height(16.dp))

        // TCS230 / TCS3200 Color Sensor Deep-Dive Card
        Text(
            text = "TCS230 / TCS3200 ONION DISEASE SENSOR",
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
                val r = robotStatus.tcsRed
                val g = robotStatus.tcsGreen
                val b = robotStatus.tcsBlue
                val intensity = robotStatus.tcsIntensity

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // RGB Color Swatch preview from actual sensor readings
                        val swatchColor = if (r != null && g != null && b != null) {
                            try {
                                val normR = (r.coerceIn(0, 255))
                                val normG = (g.coerceIn(0, 255))
                                val normB = (b.coerceIn(0, 255))
                                Color(normR, normG, normB)
                            } catch (_: Exception) {
                                Color.DarkGray
                            }
                        } else {
                            Color.DarkGray
                        }

                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(swatchColor)
                                .border(1.dp, AgroOutline, RoundedCornerShape(8.dp))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "CALIBRATED DISEASE STATUS",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = AgroTextMuted
                            )
                            val statusText = robotStatus.diseaseStatus.name.replace("_", " ")
                            val statusColor = when (robotStatus.diseaseStatus) {
                                DiseaseStatus.HEALTHY -> StatusSafeGreen
                                DiseaseStatus.DISEASE_SUSPECTED -> StatusWarningOrange
                                DiseaseStatus.SENSOR_ERROR -> StatusEmergencyRed
                                DiseaseStatus.UNKNOWN -> AgroTextMuted
                            }
                            Text(
                                text = statusText,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = statusColor
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.Default.ColorLens,
                        contentDescription = "TCS230",
                        tint = AgroGreenPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Color component bars
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ColorValuePill("RED", r?.toString() ?: "--", Color(0xFFEF4444), Modifier.weight(1f))
                    ColorValuePill("GREEN", g?.toString() ?: "--", Color(0xFF22C55E), Modifier.weight(1f))
                    ColorValuePill("BLUE", b?.toString() ?: "--", Color(0xFF3B82F6), Modifier.weight(1f))
                    ColorValuePill("INTENSITY", intensity?.toString() ?: "--", Color(0xFFFBBF24), Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(AgroSurfaceVariant)
                        .padding(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Disease Note",
                        tint = AgroTextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Color thresholds monitor leaf pigment changes. Color measurements alone do not clinically distinguish Fusarium basal rot, purple blotch, downy mildew, or neck rot.",
                        fontSize = 10.sp,
                        color = AgroTextMuted,
                        lineHeight = 13.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Soil Moisture & Microclimate (DHT11)
        Text(
            text = "SOIL & ENVIRONMENT SENSORS",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = AgroTextMuted,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val soilVal = robotStatus.soilMoisturePercent
            TelemetryItemCard(
                title = "CAPACITIVE SOIL",
                value = soilVal?.let { "$it%" } ?: "--",
                icon = Icons.Default.Opacity,
                statusColor = if (soilVal != null && soilVal < settings.soilDryThresholdPercent) StatusWarningOrange else StatusSafeGreen,
                subtitle = "State: ${robotStatus.soilState.name}",
                modifier = Modifier.weight(1f)
            )

            val tempVal = robotStatus.temperatureC
            val humVal = robotStatus.humidityPercent
            TelemetryItemCard(
                title = "DHT11 CLIMATE",
                value = tempVal?.let { "%.1f°C".format(it) } ?: "--",
                icon = Icons.Default.DeviceThermostat,
                statusColor = AgroGreenPrimary,
                subtitle = humVal?.let { "Humidity: ${it.toInt()}%" } ?: "--",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Reservoir HC-SR04 Level Sensors
        Text(
            text = "TANK LEVELS (ULTRASONIC LEVEL SENSORS)",
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
                LevelGaugeBar(
                    label = "Fresh Irrigation Water Tank",
                    currentPercent = robotStatus.waterTankLevelPercent,
                    minThresholdPercent = settings.waterTankMinPercent,
                    accentColor = Color(0xFF38BDF8)
                )
                Spacer(modifier = Modifier.height(12.dp))
                LevelGaugeBar(
                    label = "Agricultural Chemical Treatment Tank",
                    currentPercent = robotStatus.chemicalTankLevelPercent,
                    minThresholdPercent = settings.chemicalTankMinPercent,
                    accentColor = Color(0xFFA855F7)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Front Obstacle Ultrasonic & Power Telemetry
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val frontDist = robotStatus.frontUltrasonicDistanceCm
            val isObstacle = robotStatus.ultrasonicStatus == UltrasonicStatus.OBSTACLE_DETECTED
            TelemetryItemCard(
                title = "OBSTACLE (HC-SR04)",
                value = frontDist?.let { "%.1f cm".format(it) } ?: "--",
                icon = Icons.Default.Radar,
                statusColor = if (isObstacle) StatusEmergencyRed else StatusSafeGreen,
                subtitle = robotStatus.ultrasonicStatus.name.replace("_", " "),
                modifier = Modifier.weight(1f)
            )

            val batt = robotStatus.batteryVoltage
            TelemetryItemCard(
                title = "SYSTEM VOLTAGE",
                value = batt?.let { "%.2f V".format(it) } ?: "--",
                icon = Icons.Default.BatteryChargingFull,
                statusColor = if (batt != null && batt > 11.4) StatusSafeGreen else StatusWarningOrange,
                subtitle = if (batt != null) "DC BUS MONITORED" else "SENSOR NOT INSTALLED",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 3-DOF Arm Servo Feedback & Kinematics
        Text(
            text = "3-DOF ROBOTIC ARM TELEMETRY",
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Current State: ${robotStatus.armStatus.name}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Icon(
                        imageVector = Icons.Default.PrecisionManufacturing,
                        contentDescription = "3-DOF Arm",
                        tint = AgroGreenPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ArmAnglePill("BASE (J1)", "${robotStatus.armBaseAngle}°", Modifier.weight(1f))
                    ArmAnglePill("SHOULDER (J2)", "${robotStatus.armShoulderAngle}°", Modifier.weight(1f))
                    ArmAnglePill("ELBOW (J3)", "${robotStatus.armElbowAngle}°", Modifier.weight(1f))
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
fun ColorValuePill(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(AgroSurfaceVariant)
            .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = label, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = color)
            Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

@Composable
fun ArmAnglePill(label: String, angle: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(AgroSurfaceVariant)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = label, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = AgroTextMuted)
            Text(text = angle, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}
