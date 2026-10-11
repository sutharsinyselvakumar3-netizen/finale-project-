package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.screens.AutoScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.RobotScreen
import com.example.ui.screens.SensorsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.AgroCharcoalDark
import com.example.ui.theme.AgroGreenPrimary
import com.example.ui.theme.AgroSurfaceDark
import com.example.ui.theme.AgroTextMuted
import com.example.ui.theme.OnionRobotTheme
import com.example.viewmodel.RobotViewModel

enum class NavigationSection(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    HOME("HOME", Icons.Default.Home),
    ROBOT("ROBOT", Icons.Default.Build),
    AUTO("AUTO", Icons.Default.PlayArrow),
    SENSORS("SENSORS", Icons.Default.Sensors),
    SETTINGS("SETTINGS", Icons.Default.Settings)
}

class MainActivity : ComponentActivity() {

    private val viewModel: RobotViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            OnionRobotTheme {
                val robotStatus by viewModel.robotStatus.collectAsStateWithLifecycle()
                val settings by viewModel.settings.collectAsStateWithLifecycle()
                val autoState by viewModel.autoState.collectAsStateWithLifecycle()
                val latestDetection by viewModel.latestDetection.collectAsStateWithLifecycle()
                val eventLogs by viewModel.eventLogs.collectAsStateWithLifecycle()
                val userMessage by viewModel.userMessage.collectAsStateWithLifecycle()

                var currentSection by remember { mutableStateOf(NavigationSection.HOME) }
                val snackbarHostState = remember { SnackbarHostState() }

                // Display user feedback snackbars
                LaunchedEffect(userMessage) {
                    if (userMessage != null) {
                        snackbarHostState.showSnackbar(userMessage!!)
                        viewModel.clearUserMessage()
                    }
                }

                // Handle system back navigation
                if (currentSection != NavigationSection.HOME) {
                    BackHandler {
                        currentSection = NavigationSection.HOME
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = AgroCharcoalDark,
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    bottomBar = {
                        NavigationBar(
                            containerColor = AgroSurfaceDark,
                            tonalElevation = 8.dp
                        ) {
                            NavigationSection.values().forEach { section ->
                                val selected = currentSection == section
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { currentSection = section },
                                    icon = {
                                        Icon(
                                            imageVector = section.icon,
                                            contentDescription = section.title
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = section.title,
                                            fontSize = 10.sp,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = Color.White,
                                        selectedTextColor = AgroGreenPrimary,
                                        indicatorColor = AgroGreenPrimary,
                                        unselectedIconColor = AgroTextMuted,
                                        unselectedTextColor = AgroTextMuted
                                    ),
                                    modifier = Modifier.testTag("nav_item_${section.name.lowercase()}")
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (currentSection) {
                            NavigationSection.HOME -> {
                                HomeScreen(
                                    robotStatus = robotStatus,
                                    settings = settings,
                                    onTriggerEstop = { viewModel.triggerEmergencyStop() },
                                    onResetEstop = { viewModel.resetEmergencyStop() },
                                    onNavigateToRobot = { currentSection = NavigationSection.ROBOT },
                                    onNavigateToAuto = { currentSection = NavigationSection.AUTO }
                                )
                            }
                            NavigationSection.ROBOT -> {
                                RobotScreen(
                                    robotStatus = robotStatus,
                                    settings = settings,
                                    onMove = { dir -> viewModel.sendMovement(dir) },
                                    onStopMove = { viewModel.stopMovement() },
                                    onSetSpeed = { spd -> viewModel.setSpeed(spd) },
                                    onPanTilt = { pan, tilt -> viewModel.panTiltCamera(pan, tilt) },
                                    onCenterCamera = { viewModel.centerCamera() },
                                    onToggleRelay1 = { enabled -> viewModel.toggleRelay1(enabled) },
                                    onCaptureFrame = { viewModel.captureAndAnalyzeFrame() },
                                    onTriggerEstop = { viewModel.triggerEmergencyStop() },
                                    onResetEstop = { viewModel.resetEmergencyStop() }
                                )
                            }
                            NavigationSection.AUTO -> {
                                AutoScreen(
                                    robotStatus = robotStatus,
                                    autoState = autoState,
                                    latestDetection = latestDetection,
                                    settings = settings,
                                    onStartAuto = { viewModel.startAutoSequence() },
                                    onStopAuto = { viewModel.stopAutoSequence() },
                                    onTriggerEstop = { viewModel.triggerEmergencyStop() },
                                    onResetEstop = { viewModel.resetEmergencyStop() }
                                )
                            }
                            NavigationSection.SENSORS -> {
                                SensorsScreen(
                                    robotStatus = robotStatus,
                                    settings = settings
                                )
                            }
                            NavigationSection.SETTINGS -> {
                                SettingsScreen(
                                    robotStatus = robotStatus,
                                    settings = settings,
                                    eventLogs = eventLogs,
                                    onSaveSettings = { updated -> viewModel.updateSettings(updated) },
                                    onTestEsp = { s -> viewModel.testConnection(s) },
                                    onTestCam = { s -> viewModel.testCamera(s) },
                                    onClearLogs = { viewModel.clearLogs() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
