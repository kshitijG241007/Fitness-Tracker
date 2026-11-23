package com.example.ui.viewmodel

import com.example.data.model.JournalDayGroup
import com.example.data.model.ProgressSummary
import com.example.data.model.UserProfile
import com.example.data.model.UserSettings
import com.example.data.model.WorkoutType

data class TodayStatsUi(
    val steps: Int = 0,
    val stepGoal: Int = 6000,
    val distanceKm: Double = 0.0,
    val caloriesBurned: Int = 0,
    val activeMinutes: Int = 0,
    val workoutsCompletedToday: Int = 0
)

data class WorkoutTimerUi(
    val selectedType: WorkoutType = WorkoutType.WALKING,
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    val elapsedSeconds: Long = 0L,
    val estimatedCalories: Int = 0,
    val estimatedDistanceKm: Double = 0.0
)

data class SensorStatusUi(
    val isSensorHardwareAvailable: Boolean = false,
    val isPermissionGranted: Boolean = false,
    val isPermissionDenied: Boolean = false,
    val isListening: Boolean = false
)

data class FitTrackUiState(
    val userProfile: UserProfile = UserProfile(),
    val userSettings: UserSettings = UserSettings(),
    val todayStats: TodayStatsUi = TodayStatsUi(),
    val workoutTimer: WorkoutTimerUi = WorkoutTimerUi(),
    val sensorStatus: SensorStatusUi = SensorStatusUi(),
    val journalGroups: List<JournalDayGroup> = emptyList(),
    val progressSummary: ProgressSummary = ProgressSummary(),
    val snackbarMessage: String? = null
)
