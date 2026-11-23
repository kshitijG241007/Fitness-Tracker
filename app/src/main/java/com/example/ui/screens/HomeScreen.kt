package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsRun
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Route
import androidx.compose.material.icons.rounded.Sensors
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.repository.FitnessCalculator
import com.example.ui.components.StepProgressRing
import com.example.ui.theme.ActiveAmber
import com.example.ui.theme.CalorieCoral
import com.example.ui.theme.ProgressBlue
import com.example.ui.viewmodel.FitTrackUiState

@Composable
fun HomeScreen(
    uiState: FitTrackUiState,
    onActivityPermissionResult: (Boolean) -> Unit,
    onAddManualSteps: (Int) -> Unit,
    onSetTodaySteps: (Int) -> Unit,
    onNavigateToActivity: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = onActivityPermissionResult
    )

    var showManualDialog by remember { mutableStateOf(false) }
    var showPermissionExplanation by remember { mutableStateOf(false) }

    val greeting = if (uiState.userProfile.name.isNotBlank()) {
        "Hi, ${uiState.userProfile.name.trim()}"
    } else {
        "FitTrack"
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 600.dp)
        ) {
            // Top Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = greeting,
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Today's activity summary",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = onNavigateToProfile,
                        modifier = Modifier.testTag("home_profile_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Person,
                            contentDescription = "Profile and Goals"
                        )
                    }
                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier.testTag("home_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = "Settings"
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Hero Progress Ring Card
            ElevatedCard(
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f),
                                    MaterialTheme.colorScheme.surface
                                )
                            )
                        )
                        .padding(vertical = 24.dp, horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        StepProgressRing(
                            steps = uiState.todayStats.steps,
                            goalSteps = uiState.todayStats.stepGoal
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Quick adjust / manual log button always accessible
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (uiState.sensorStatus.isListening) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Sensors,
                                            contentDescription = "Step sensor active",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "Sensor active",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                            }

                            FilledTonalButton(
                                onClick = { showManualDialog = true },
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                                modifier = Modifier.testTag("home_log_steps_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Edit,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Log Steps")
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Permission or Manual Fallback Card
            val sensorStatus = uiState.sensorStatus
            AnimatedVisibility(
                visible = !sensorStatus.isPermissionGranted || !sensorStatus.isSensorHardwareAvailable
            ) {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = if (sensorStatus.isPermissionDenied || !sensorStatus.isSensorHardwareAvailable) {
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
                        } else {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        if (!sensorStatus.isPermissionGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.DirectionsWalk,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = if (sensorStatus.isPermissionDenied) {
                                        "Manual step entry mode"
                                    } else {
                                        "Automatic step counting"
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (sensorStatus.isPermissionDenied) {
                                    "Activity recognition permission was declined. You can add or update today's steps manually below, or enable the sensor anytime."
                                } else {
                                    "Allow physical activity access so FitTrack can count your steps automatically using your phone's built-in step sensor. All data stays on your device."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = { showPermissionExplanation = true },
                                    modifier = Modifier.testTag("enable_step_sensor_button")
                                ) {
                                    Text(if (sensorStatus.isPermissionDenied) "Retry Permission" else "Enable Sensor")
                                }
                                OutlinedButton(
                                    onClick = { showManualDialog = true },
                                    modifier = Modifier.testTag("manual_steps_fallback_button")
                                ) {
                                    Text("Enter Manually")
                                }
                            }
                        } else if (!sensorStatus.isSensorHardwareAvailable) {
                            Text(
                                text = "Step sensor unavailable on this device",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Use quick buttons below to add your steps manually.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilledTonalButton(
                                    onClick = { onAddManualSteps(500) },
                                    modifier = Modifier.testTag("quick_add_500_steps")
                                ) {
                                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("500")
                                }
                                FilledTonalButton(
                                    onClick = { onAddManualSteps(1000) },
                                    modifier = Modifier.testTag("quick_add_1000_steps")
                                ) {
                                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("1,000")
                                }
                                OutlinedButton(
                                    onClick = { showManualDialog = true },
                                    modifier = Modifier.testTag("custom_steps_button")
                                ) {
                                    Text("Custom")
                                }
                            }
                        }
                    }
                }
            }

            // 3 Metric Cards (Distance, Calories Burned, Active Minutes)
            val distanceText = FitnessCalculator.formatDistanceValueOnly(
                uiState.todayStats.distanceKm,
                uiState.userSettings.distanceUnit
            )
            val distanceUnitLabel = uiState.userSettings.distanceUnit.shortLabel

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricStatCard(
                    title = "Distance",
                    value = distanceText,
                    unit = distanceUnitLabel,
                    icon = Icons.Rounded.Route,
                    accentColor = ProgressBlue,
                    modifier = Modifier.weight(1f)
                )
                MetricStatCard(
                    title = "Calories",
                    value = "${uiState.todayStats.caloriesBurned}",
                    unit = "kcal",
                    icon = Icons.Rounded.LocalFireDepartment,
                    accentColor = CalorieCoral,
                    modifier = Modifier.weight(1f)
                )
                MetricStatCard(
                    title = "Active",
                    value = "${uiState.todayStats.activeMinutes}",
                    unit = "min",
                    icon = Icons.Rounded.Timer,
                    accentColor = ActiveAmber,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Quick Workout Launcher Card
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.DirectionsRun,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Column {
                            Text(
                                text = if (uiState.todayStats.workoutsCompletedToday > 0) {
                                    "${uiState.todayStats.workoutsCompletedToday} workout(s) logged today"
                                } else {
                                    "Ready for a workout?"
                                },
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Track walking, running, cycling, yoga & gym",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    FilledTonalButton(
                        onClick = onNavigateToActivity,
                        modifier = Modifier.testTag("home_go_activity_button")
                    ) {
                        Text("Track")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showPermissionExplanation) {
        AlertDialog(
            onDismissRequest = { showPermissionExplanation = false },
            title = { Text("Allow Step Counting") },
            text = {
                Text(
                    "FitTrack uses Android's Physical Activity recognition permission solely to read your device's built-in step counter sensor. No location or cloud data is collected."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPermissionExplanation = false
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            permissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
                        } else {
                            onActivityPermissionResult(true)
                        }
                    }
                ) {
                    Text("Continue")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showPermissionExplanation = false
                        onActivityPermissionResult(false)
                    }
                ) {
                    Text("Enter Manually")
                }
            }
        )
    }

    if (showManualDialog) {
        ManualStepsDialog(
            currentSteps = uiState.todayStats.steps,
            onDismiss = { showManualDialog = false },
            onAddSteps = { add ->
                onAddManualSteps(add)
                showManualDialog = false
            },
            onSetTotalSteps = { total ->
                onSetTodaySteps(total)
                showManualDialog = false
            }
        )
    }
}

@Composable
private fun MetricStatCard(
    title: String,
    value: String,
    unit: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(accentColor.copy(alpha = 0.14f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "$unit • $title",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ManualStepsDialog(
    currentSteps: Int,
    onDismiss: () -> Unit,
    onAddSteps: (Int) -> Unit,
    onSetTotalSteps: (Int) -> Unit
) {
    var inputText by remember { mutableStateOf("") }
    var isAddMode by remember { mutableStateOf(true) }
    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log Today's Steps") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Current today: $currentSteps steps",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (isAddMode) {
                        Button(onClick = { isAddMode = true }, modifier = Modifier.weight(1f)) {
                            Text("Add Steps")
                        }
                        OutlinedButton(onClick = { isAddMode = false }, modifier = Modifier.weight(1f)) {
                            Text("Set Total")
                        }
                    } else {
                        OutlinedButton(onClick = { isAddMode = true }, modifier = Modifier.weight(1f)) {
                            Text("Add Steps")
                        }
                        Button(onClick = { isAddMode = false }, modifier = Modifier.weight(1f)) {
                            Text("Set Total")
                        }
                    }
                }
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { value ->
                        if (value.all { it.isDigit() } && value.length <= 6) {
                            inputText = value
                            errorText = null
                        }
                    },
                    label = {
                        Text(if (isAddMode) "Steps to add (e.g. 1500)" else "Total steps today")
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    isError = errorText != null,
                    supportingText = errorText?.let { { Text(it) } },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("manual_steps_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsed = inputText.toIntOrNull()
                    if (parsed == null) {
                        errorText = "Enter a valid number of steps"
                    } else if (isAddMode && parsed <= 0) {
                        errorText = "Enter at least 1 step to add"
                    } else if (parsed > 150_000) {
                        errorText = "Please enter a realistic daily count (max 150,000)"
                    } else {
                        if (isAddMode) onAddSteps(parsed) else onSetTotalSteps(parsed)
                    }
                },
                modifier = Modifier.testTag("manual_steps_confirm_button")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
