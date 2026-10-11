package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MovementCommand
import com.example.data.model.RobotSettings
import com.example.data.model.RobotSpeed
import com.example.data.model.RobotStatus
import com.example.data.model.UltrasonicStatus
import com.example.ui.components.EmergencyStopBar
import com.example.ui.components.MjpegStreamPlayer
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
fun RobotScreen(
    robotStatus: RobotStatus,
    settings: RobotSettings,
    onMove: (MovementCommand) -> Unit,
    onStopMove: () -> Unit,
    onSetSpeed: (RobotSpeed) -> Unit,
    onPanTilt: (panDelta: Int, tiltDelta: Int) -> Unit,
    onCenterCamera: () -> Unit,
    onToggleRelay1: (Boolean) -> Unit,
    onCaptureFrame: () -> Unit,
    onTriggerEstop: () -> Unit,
    onResetEstop: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

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

        // Real ESP32-CAM MJPEG Video Stream
        Text(
            text = "LIVE ESP32-CAM FEED",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = AgroTextMuted,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(6.dp))

        Box(modifier = Modifier.fillMaxWidth()) {
            MjpegStreamPlayer(
                streamUrl = settings.camStreamUrl,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Top Right Stream Status Overlay
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xCC111827))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (robotStatus.cameraConnected) StatusSafeGreen else StatusEmergencyRed)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Pan: ${robotStatus.camPanAngle}° Tilt: ${robotStatus.camTiltAngle}°",
                        fontSize = 10.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Snap photo button on stream overlay
                Button(
                    onClick = onCaptureFrame,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xCC16A34A)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .testTag("stream_capture_button")
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = "Capture AI Frame", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("AI SCAN", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Real Front Ultrasonic Safety Banner
        val isObstacle = robotStatus.ultrasonicStatus == UltrasonicStatus.OBSTACLE_DETECTED
        val isError = robotStatus.ultrasonicStatus == UltrasonicStatus.SENSOR_ERROR
        val distVal = robotStatus.frontUltrasonicDistanceCm

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = when {
                    isObstacle -> Color(0xFF450A0A)
                    isError -> Color(0xFF431407)
                    else -> AgroCardSurface
                }
            ),
            shape = RoundedCornerShape(10.dp),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = androidx.compose.ui.graphics.SolidColor(
                    when {
                        isObstacle -> StatusEmergencyRed
                        isError -> StatusWarningOrange
                        else -> AgroOutline
                    }
                )
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isObstacle) Icons.Default.Warning else Icons.Default.Radar,
                        contentDescription = "Ultrasonic Status",
                        tint = when {
                            isObstacle -> StatusEmergencyRed
                            isError -> StatusWarningOrange
                            else -> StatusSafeGreen
                        },
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "FRONT ULTRASONIC SAFETY",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AgroTextMuted
                        )
                        Text(
                            text = when (robotStatus.ultrasonicStatus) {
                                UltrasonicStatus.PATH_CLEAR -> "PATH CLEAR (${distVal?.let { "%.1f cm".format(it) } ?: "--"})"
                                UltrasonicStatus.OBSTACLE_DETECTED -> "OBSTACLE DETECTED (${distVal?.let { "%.1f cm".format(it) } ?: "< threshold"})"
                                UltrasonicStatus.SENSOR_ERROR -> "SENSOR ERROR"
                            },
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                if (isObstacle) {
                    Text(
                        text = "OVERRIDE ACTIVE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = StatusEmergencyRed
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Speed Selector
        Text(
            text = "DRIVE SPEED",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = AgroTextMuted,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            RobotSpeed.values().forEach { speedOption ->
                val isSelected = robotStatus.speed == speedOption
                Button(
                    onClick = { onSetSpeed(speedOption) },
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("speed_${speedOption.name.lowercase()}"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSelected) AgroGreenPrimary else AgroSurfaceVariant,
                        contentColor = if (isSelected) Color.White else AgroTextSecondary
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = speedOption.label,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Movement Controls: EXACTLY 4 BUTTONS (FORWARD, LEFT, RIGHT, REVERSE)
        // No joystick. Large outdoor touch targets (min 56dp x 56dp).
        Text(
            text = "ROBOT MOVEMENT (MANUAL)",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = AgroTextMuted,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = AgroCardSurface),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // FORWARD
                TactileMovementButton(
                    label = "FORWARD",
                    icon = Icons.Default.KeyboardArrowUp,
                    command = MovementCommand.FORWARD,
                    isActive = robotStatus.currentMovement == MovementCommand.FORWARD,
                    onMove = onMove,
                    onStop = onStopMove,
                    testTag = "move_forward"
                )

                Spacer(modifier = Modifier.height(10.dp))

                // LEFT - STOP INDICATOR - RIGHT
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TactileMovementButton(
                        label = "LEFT",
                        icon = Icons.Default.KeyboardArrowLeft,
                        command = MovementCommand.LEFT,
                        isActive = robotStatus.currentMovement == MovementCommand.LEFT,
                        onMove = onMove,
                        onStop = onStopMove,
                        testTag = "move_left"
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    // Center Stop Status Indicator / Tap to Stop
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(
                                if (robotStatus.currentMovement != MovementCommand.STOP) Color(0xFF1E3A24)
                                else Color(0xFF263329)
                            )
                            .border(
                                2.dp,
                                if (robotStatus.currentMovement != MovementCommand.STOP) StatusSafeGreen
                                else AgroOutline,
                                CircleShape
                            )
                            .pointerInput(Unit) {
                                detectTapGestures(onTap = { onStopMove() })
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (robotStatus.currentMovement != MovementCommand.STOP) "DRIVING" else "STOP",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (robotStatus.currentMovement != MovementCommand.STOP) StatusSafeGreen else AgroTextMuted
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    TactileMovementButton(
                        label = "RIGHT",
                        icon = Icons.Default.KeyboardArrowRight,
                        command = MovementCommand.RIGHT,
                        isActive = robotStatus.currentMovement == MovementCommand.RIGHT,
                        onMove = onMove,
                        onStop = onStopMove,
                        testTag = "move_right"
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // REVERSE
                TactileMovementButton(
                    label = "REVERSE",
                    icon = Icons.Default.KeyboardArrowDown,
                    command = MovementCommand.BACKWARD,
                    isActive = robotStatus.currentMovement == MovementCommand.BACKWARD,
                    onMove = onMove,
                    onStop = onStopMove,
                    testTag = "move_reverse"
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Camera Pan/Tilt Servos
        Text(
            text = "CAMERA PAN / TILT SERVOS",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = AgroTextMuted,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = AgroCardSurface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // TILT UP
                OutlinedButton(
                    onClick = { onPanTilt(0, 10) },
                    modifier = Modifier.size(width = 120.dp, height = 44.dp).testTag("cam_tilt_up")
                ) {
                    Text("TILT UP", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { onPanTilt(-10, 0) },
                        modifier = Modifier.size(width = 100.dp, height = 44.dp).testTag("cam_pan_left")
                    ) {
                        Text("PAN LEFT", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Button(
                        onClick = onCenterCamera,
                        colors = ButtonDefaults.buttonColors(containerColor = AgroSurfaceVariant),
                        modifier = Modifier.size(width = 80.dp, height = 44.dp).testTag("cam_center")
                    ) {
                        Icon(Icons.Default.CenterFocusStrong, contentDescription = "Center Camera", modifier = Modifier.size(16.dp))
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    OutlinedButton(
                        onClick = { onPanTilt(10, 0) },
                        modifier = Modifier.size(width = 100.dp, height = 44.dp).testTag("cam_pan_right")
                    ) {
                        Text("PAN RIGHT", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // TILT DOWN
                OutlinedButton(
                    onClick = { onPanTilt(0, -10) },
                    modifier = Modifier.size(width = 120.dp, height = 44.dp).testTag("cam_tilt_down")
                ) {
                    Text("TILT DOWN", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Manual Relay: Exactly one manual relay (RELAY 1)
        Text(
            text = "MANUAL RELAY OUTPUT",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = AgroTextMuted,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

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
                    Icon(
                        imageVector = Icons.Default.ElectricBolt,
                        contentDescription = "Relay 1",
                        tint = if (robotStatus.relay1Active) StatusEmergencyRed else AgroTextMuted,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "RELAY 1 (MANUAL ASSIGNMENT)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Auto-shutdown safety: 30s max duration",
                            fontSize = 11.sp,
                            color = AgroTextMuted
                        )
                    }
                }

                Switch(
                    checked = robotStatus.relay1Active,
                    onCheckedChange = { onToggleRelay1(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = AgroGreenPrimary,
                        uncheckedTrackColor = Color(0xFF263329)
                    ),
                    modifier = Modifier.testTag("relay_1_switch")
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
fun TactileMovementButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    command: MovementCommand,
    isActive: Boolean,
    onMove: (MovementCommand) -> Unit,
    onStop: () -> Unit,
    testTag: String
) {
    Box(
        modifier = Modifier
            .size(width = 110.dp, height = 58.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isActive) AgroGreenPrimary else AgroSurfaceVariant)
            .border(
                2.dp,
                if (isActive) Color.White else AgroOutline,
                RoundedCornerShape(12.dp)
            )
            .pointerInput(command) {
                detectTapGestures(
                    onPress = {
                        onMove(command)
                        val released = tryAwaitRelease()
                        if (released) {
                            onStop()
                        }
                    }
                )
            }
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isActive) Color.White else AgroTextPrimary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (isActive) Color.White else AgroTextPrimary
            )
        }
    }
}
