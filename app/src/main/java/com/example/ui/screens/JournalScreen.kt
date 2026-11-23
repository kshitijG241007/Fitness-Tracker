package com.example.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.DistanceUnit
import com.example.data.model.JournalDayGroup
import com.example.data.model.JournalWorkoutItem
import com.example.data.repository.FitnessCalculator
import com.example.ui.theme.ActiveAmber
import com.example.ui.theme.CalorieCoral
import com.example.ui.theme.ProgressBlue
import com.example.ui.viewmodel.FitTrackUiState
import java.text.NumberFormat

@Composable
fun JournalScreen(
    uiState: FitTrackUiState,
    onDeleteWorkout: (Long) -> Unit,
    onNavigateToActivity: () -> Unit,
    modifier: Modifier = Modifier
) {
    var workoutToDelete by remember { mutableStateOf<JournalWorkoutItem?>(null) }
    val groups = uiState.journalGroups
    val distanceUnit = uiState.userSettings.distanceUnit

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Activity Journal",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Daily summaries and logged workouts",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(14.dp))

            if (groups.isEmpty()) {
                JournalEmptyState(
                    onStartWorkout = onNavigateToActivity,
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    groups.forEach { dayGroup ->
                        item(key = "header_${dayGroup.date}") {
                            DailySummaryHeaderCard(
                                dayGroup = dayGroup,
                                distanceUnit = distanceUnit
                            )
                        }

                        if (dayGroup.workouts.isEmpty()) {
                            item(key = "no_workouts_${dayGroup.date}") {
                                Text(
                                    text = "Step activity only — no separate workouts logged this day.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        } else {
                            items(
                                items = dayGroup.workouts,
                                key = { "workout_${it.id}" }
                            ) { workout ->
                                WorkoutEntryCard(
                                    workout = workout,
                                    distanceUnit = distanceUnit,
                                    onDeleteClick = { workoutToDelete = workout }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    workoutToDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { workoutToDelete = null },
            title = { Text("Delete Workout") },
            text = {
                Text(
                    "Remove this ${item.type.displayName} session (${FitnessCalculator.formatDurationReadable(item.durationSeconds)}) from your journal?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteWorkout(item.id)
                        workoutToDelete = null
                    },
                    modifier = Modifier.testTag("confirm_delete_workout_button")
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { workoutToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun DailySummaryHeaderCard(
    dayGroup: JournalDayGroup,
    distanceUnit: DistanceUnit
) {
    val formatter = remember { NumberFormat.getIntegerInstance() }

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dayGroup.displayDate,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (dayGroup.totalSteps >= dayGroup.goalSteps && dayGroup.goalSteps > 0) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surface
                    }
                ) {
                    Text(
                        text = if (dayGroup.totalSteps >= dayGroup.goalSteps && dayGroup.goalSteps > 0) {
                            "Goal Reached"
                        } else {
                            "${formatter.format(dayGroup.totalSteps)} steps"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (dayGroup.totalSteps >= dayGroup.goalSteps && dayGroup.goalSteps > 0) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SummaryBadge(
                    icon = Icons.Rounded.DirectionsWalk,
                    text = "${formatter.format(dayGroup.totalSteps)} steps",
                    tint = MaterialTheme.colorScheme.primary
                )
                SummaryBadge(
                    icon = Icons.Rounded.LocalFireDepartment,
                    text = "${dayGroup.totalCalories} kcal",
                    tint = CalorieCoral
                )
                SummaryBadge(
                    icon = Icons.Rounded.Timer,
                    text = "${dayGroup.totalActiveMinutes} min",
                    tint = ActiveAmber
                )
                Text(
                    text = FitnessCalculator.formatDistance(dayGroup.totalDistanceKm, distanceUnit),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = ProgressBlue
                )
            }
        }
    }
}

@Composable
private fun SummaryBadge(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    tint: androidx.compose.ui.graphics.Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(15.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun WorkoutEntryCard(
    workout: JournalWorkoutItem,
    distanceUnit: DistanceUnit,
    onDeleteClick: () -> Unit
) {
    ElevatedCard(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("journal_workout_card_${workout.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, top = 12.dp, bottom = 12.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = workoutIcon(workout.type),
                    contentDescription = workout.type.displayName,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = workout.type.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = workout.timeLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                val details = buildString {
                    append(FitnessCalculator.formatDurationReadable(workout.durationSeconds))
                    append(" • ")
                    append("${workout.caloriesBurned} kcal")
                    if (workout.type.tracksDistance && workout.distanceKm > 0.01) {
                        append(" • ")
                        append(FitnessCalculator.formatDistance(workout.distanceKm, distanceUnit))
                    }
                }
                Text(
                    text = details,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(
                onClick = onDeleteClick,
                modifier = Modifier.testTag("delete_workout_${workout.id}")
            ) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = "Delete ${workout.type.displayName} workout",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun JournalEmptyState(
    onStartWorkout: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_empty_workout),
                contentDescription = "Start your first workout illustration",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth(0.78f)
                    .height(175.dp)
                    .clip(MaterialTheme.shapes.large)
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "No workouts yet",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "No workouts yet. Tap Start to log your first one!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(18.dp))
            Button(
                onClick = onStartWorkout,
                modifier = Modifier.testTag("empty_journal_start_button")
            ) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Start a Workout")
            }
        }
    }
}
