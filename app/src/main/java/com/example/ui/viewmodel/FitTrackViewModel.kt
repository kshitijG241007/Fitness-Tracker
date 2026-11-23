package com.example.ui.viewmodel

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import android.os.SystemClock
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.FitTrackDatabase
import com.example.data.local.UserPreferencesRepository
import com.example.data.model.DistanceUnit
import com.example.data.model.ThemeMode
import com.example.data.model.UserProfile
import com.example.data.model.WeightUnit
import com.example.data.model.WorkoutType
import com.example.data.repository.DateHelper
import com.example.data.repository.FitnessCalculator
import com.example.data.repository.FitnessRepository
import com.example.reminders.ReminderScheduler
import com.example.sensors.StepCounterManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class FitTrackViewModel(application: Application) : AndroidViewModel(application) {

    private val database = FitTrackDatabase.getInstance(application)
    private val preferences = UserPreferencesRepository(application)
    private val repository = FitnessRepository(database.fitTrackDao(), preferences)
    private val stepCounterManager = StepCounterManager(application)

    private val timerState = MutableStateFlow(WorkoutTimerUi())
    private val sensorState = MutableStateFlow(
        SensorStatusUi(
            isSensorHardwareAvailable = stepCounterManager.isSensorAvailable,
            isPermissionGranted = hasActivityRecognitionPermission()
        )
    )
    private val snackbarState = MutableStateFlow<String?>(null)

    private var timerJob: Job? = null
    private var accumulatedSecondsBeforeResume = 0L
    private var lastResumeElapsedRealtime = 0L

    private val todayStatsFlow = combine(
        repository.observeTodaySteps(),
        repository.allWorkoutsFlow,
        repository.userProfileFlow
    ) { steps, workouts, profile ->
        val today = DateHelper.todayKey()
        val todayWorkouts = workouts.filter { it.date == today }

        val stepDistanceKm = FitnessCalculator.stepsToDistanceKm(steps, profile)
        val workoutDistanceKm = todayWorkouts.sumOf { it.distanceKm }
        val stepCalories = FitnessCalculator.stepsToCalories(steps, profile)
        val workoutCalories = todayWorkouts.sumOf { it.caloriesBurned }
        val stepActiveMinutes = FitnessCalculator.stepsToActiveMinutes(steps)
        val workoutActiveMinutes = todayWorkouts.sumOf { (it.durationSeconds / 60.0).roundToInt() }

        TodayStatsUi(
            steps = steps,
            stepGoal = profile.dailyStepGoal,
            distanceKm = stepDistanceKm + workoutDistanceKm,
            caloriesBurned = stepCalories + workoutCalories,
            activeMinutes = stepActiveMinutes + workoutActiveMinutes,
            workoutsCompletedToday = todayWorkouts.size
        )
    }

    val uiState: StateFlow<FitTrackUiState> = combine(
        repository.userProfileFlow,
        repository.userSettingsFlow,
        todayStatsFlow,
        timerState,
        sensorState,
        repository.journalGroupsFlow,
        repository.progressSummaryFlow,
        snackbarState
    ) { values ->
        val profile = values[0] as UserProfile
        val settings = values[1] as com.example.data.model.UserSettings
        val todayStats = values[2] as TodayStatsUi
        val workoutTimer = values[3] as WorkoutTimerUi
        val sensor = values[4] as SensorStatusUi
        @Suppress("UNCHECKED_CAST")
        val journal = values[5] as List<com.example.data.model.JournalDayGroup>
        val progress = values[6] as com.example.data.model.ProgressSummary
        val snackbar = values[7] as String?

        FitTrackUiState(
            userProfile = profile,
            userSettings = settings,
            todayStats = todayStats,
            workoutTimer = workoutTimer,
            sensorStatus = sensor,
            journalGroups = journal,
            progressSummary = progress,
            snackbarMessage = snackbar
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = FitTrackUiState()
    )

    init {
        refreshPermissionAndStartSensorIfAllowed()
    }

    private fun hasActivityRecognitionPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                getApplication(),
                Manifest.permission.ACTIVITY_RECOGNITION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun refreshPermissionAndStartSensorIfAllowed() {
        val granted = hasActivityRecognitionPermission()
        val available = stepCounterManager.isSensorAvailable
        sensorState.update {
            it.copy(
                isSensorHardwareAvailable = available,
                isPermissionGranted = granted
            )
        }
        if (granted && available && !sensorState.value.isListening) {
            startStepSensor()
        }
    }

    fun onActivityPermissionResult(granted: Boolean) {
        sensorState.update {
            it.copy(
                isPermissionGranted = granted,
                isPermissionDenied = !granted
            )
        }
        if (granted) {
            if (stepCounterManager.isSensorAvailable) {
                startStepSensor()
                showMessage("Live step tracking enabled")
            } else {
                showMessage("No hardware step sensor on this device — manual entry is ready")
            }
        } else {
            showMessage("Permission declined. You can log steps manually anytime.")
        }
    }

    private fun startStepSensor() {
        if (!stepCounterManager.isSensorAvailable) return
        stepCounterManager.startListening { cumulativeSteps ->
            viewModelScope.launch {
                repository.onSensorStepReading(cumulativeSteps)
            }
        }
        sensorState.update { it.copy(isListening = true) }
    }

    fun addManualSteps(steps: Int) {
        viewModelScope.launch {
            repository.addManualSteps(steps)
            showMessage("Added $steps steps to today")
        }
    }

    fun setTodaySteps(totalSteps: Int) {
        viewModelScope.launch {
            repository.setTodaySteps(totalSteps)
            showMessage("Updated today's steps to $totalSteps")
        }
    }

    fun selectWorkoutType(type: WorkoutType) {
        if (timerState.value.isRunning || timerState.value.isPaused) return
        timerState.update {
            it.copy(selectedType = type)
        }
    }

    fun startWorkout() {
        val current = timerState.value
        if (current.isRunning) return

        if (!current.isPaused) {
            accumulatedSecondsBeforeResume = 0L
        }
        lastResumeElapsedRealtime = SystemClock.elapsedRealtime()
        timerState.update {
            it.copy(isRunning = true, isPaused = false)
        }
        startTickerLoop()
    }

    fun pauseWorkout() {
        val current = timerState.value
        if (!current.isRunning) return

        val deltaSeconds = (SystemClock.elapsedRealtime() - lastResumeElapsedRealtime) / 1000L
        accumulatedSecondsBeforeResume += deltaSeconds
        timerJob?.cancel()
        timerJob = null
        updateTimerMetrics(accumulatedSecondsBeforeResume, isRunning = false, isPaused = true)
    }

    fun stopAndSaveWorkout() {
        timerJob?.cancel()
        timerJob = null

        val current = timerState.value
        val totalSeconds = if (current.isRunning) {
            accumulatedSecondsBeforeResume +
                ((SystemClock.elapsedRealtime() - lastResumeElapsedRealtime) / 1000L)
        } else {
            current.elapsedSeconds
        }

        val finalSeconds = totalSeconds.coerceAtLeast(1L)
        val type = current.selectedType

        // Reset timer state immediately
        accumulatedSecondsBeforeResume = 0L
        timerState.value = WorkoutTimerUi(selectedType = type)

        viewModelScope.launch {
            repository.saveWorkout(type = type, durationSeconds = finalSeconds)
            showMessage("Saved ${type.displayName} workout (${FitnessCalculator.formatDurationReadable(finalSeconds)})")
        }
    }

    fun discardWorkout() {
        timerJob?.cancel()
        timerJob = null
        accumulatedSecondsBeforeResume = 0L
        val selected = timerState.value.selectedType
        timerState.value = WorkoutTimerUi(selectedType = selected)
        showMessage("Workout discarded")
    }

    private fun startTickerLoop() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive) {
                val runningDelta = (SystemClock.elapsedRealtime() - lastResumeElapsedRealtime) / 1000L
                val totalSeconds = accumulatedSecondsBeforeResume + runningDelta
                updateTimerMetrics(totalSeconds, isRunning = true, isPaused = false)
                delay(500L)
            }
        }
    }

    private fun updateTimerMetrics(totalSeconds: Long, isRunning: Boolean, isPaused: Boolean) {
        val type = timerState.value.selectedType
        val weightKg = uiState.value.userProfile.weightKg
        val calories = FitnessCalculator.calculateWorkoutCalories(type, totalSeconds, weightKg)
        val distance = FitnessCalculator.calculateWorkoutDistanceKm(type, totalSeconds)
        timerState.update {
            it.copy(
                isRunning = isRunning,
                isPaused = isPaused,
                elapsedSeconds = totalSeconds,
                estimatedCalories = calories,
                estimatedDistanceKm = distance
            )
        }
    }

    fun deleteWorkout(workoutId: Long) {
        viewModelScope.launch {
            repository.deleteWorkout(workoutId)
            showMessage("Workout entry removed")
        }
    }

    fun saveUserProfile(profile: UserProfile) {
        viewModelScope.launch {
            repository.saveUserProfile(profile)
            showMessage("Profile & goals saved")
        }
    }

    fun updateDistanceUnit(unit: DistanceUnit) {
        viewModelScope.launch {
            repository.updateDistanceUnit(unit)
        }
    }

    fun updateWeightUnit(unit: WeightUnit) {
        viewModelScope.launch {
            repository.updateWeightUnit(unit)
        }
    }

    fun setRemindersEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateRemindersEnabled(enabled)
            val app = getApplication<Application>()
            if (enabled) {
                ReminderScheduler.scheduleDailyReminder(app)
                showMessage("Daily fitness reminders enabled")
            } else {
                ReminderScheduler.cancelDailyReminder(app)
                showMessage("Daily fitness reminders turned off")
            }
        }
    }

    fun updateThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            repository.updateThemeMode(mode)
        }
    }

    fun showMessage(message: String) {
        snackbarState.value = message
    }

    fun clearSnackbarMessage() {
        snackbarState.value = null
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        stepCounterManager.stopListening()
    }
}
