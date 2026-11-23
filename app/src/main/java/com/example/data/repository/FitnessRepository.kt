package com.example.data.repository

import com.example.data.local.DailyActivityEntity
import com.example.data.local.FitTrackDao
import com.example.data.local.UserPreferencesRepository
import com.example.data.local.WorkoutEntity
import com.example.data.model.DayStepPoint
import com.example.data.model.DistanceUnit
import com.example.data.model.JournalDayGroup
import com.example.data.model.JournalWorkoutItem
import com.example.data.model.ProgressSummary
import com.example.data.model.ThemeMode
import com.example.data.model.UserProfile
import com.example.data.model.UserSettings
import com.example.data.model.WeightUnit
import com.example.data.model.WorkoutType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.roundToInt

class FitnessRepository(
    private val dao: FitTrackDao,
    private val prefs: UserPreferencesRepository
) {
    private val stepMutex = Mutex()

    val userProfileFlow: Flow<UserProfile> = prefs.userProfileFlow
    val userSettingsFlow: Flow<UserSettings> = prefs.userSettingsFlow
    val permissionDismissedFlow: Flow<Boolean> = prefs.permissionDismissedFlow

    val allDailyActivitiesFlow: Flow<List<DailyActivityEntity>> = dao.observeAllDailyActivities()
    val allWorkoutsFlow: Flow<List<WorkoutEntity>> = dao.observeAllWorkouts()

    fun observeTodaySteps(): Flow<Int> {
        val today = DateHelper.todayKey()
        return dao.observeDailyActivity(today).combine(userProfileFlow) { entity, _ ->
            entity?.steps ?: 0
        }
    }

    val journalGroupsFlow: Flow<List<JournalDayGroup>> = combine(
        allDailyActivitiesFlow,
        allWorkoutsFlow,
        userProfileFlow
    ) { activities, workouts, profile ->
        val activityMap = activities.associateBy { it.date }
        val workoutsByDate = workouts.groupBy { it.date }
        val allDates = (activityMap.keys + workoutsByDate.keys)
            .distinct()
            .sortedDescending()

        allDates.mapNotNull { date ->
            val dayActivity = activityMap[date]
            val dayWorkouts = workoutsByDate[date].orEmpty()
            val steps = dayActivity?.steps ?: 0
            // Skip completely empty days with 0 steps and 0 workouts
            if (steps == 0 && dayWorkouts.isEmpty()) return@mapNotNull null

            val goalSteps = dayActivity?.goalSteps ?: profile.dailyStepGoal
            val stepDistance = FitnessCalculator.stepsToDistanceKm(steps, profile)
            val workoutDistance = dayWorkouts.sumOf { it.distanceKm }
            val stepCalories = FitnessCalculator.stepsToCalories(steps, profile)
            val workoutCalories = dayWorkouts.sumOf { it.caloriesBurned }
            val stepActiveMin = FitnessCalculator.stepsToActiveMinutes(steps)
            val workoutActiveMin = dayWorkouts.sumOf { (it.durationSeconds / 60.0).roundToInt() }

            JournalDayGroup(
                date = date,
                displayDate = DateHelper.formatJournalHeader(date),
                totalSteps = steps,
                goalSteps = goalSteps,
                totalDistanceKm = stepDistance + workoutDistance,
                totalCalories = stepCalories + workoutCalories,
                totalActiveMinutes = stepActiveMin + workoutActiveMin,
                workouts = dayWorkouts.map { entity ->
                    JournalWorkoutItem(
                        id = entity.id,
                        type = WorkoutType.fromName(entity.type),
                        durationSeconds = entity.durationSeconds,
                        caloriesBurned = entity.caloriesBurned,
                        distanceKm = entity.distanceKm,
                        timestampMillis = entity.timestampMillis,
                        date = entity.date,
                        timeLabel = DateHelper.formatTime(entity.timestampMillis)
                    )
                }
            )
        }
    }

    val progressSummaryFlow: Flow<ProgressSummary> = combine(
        allDailyActivitiesFlow,
        userProfileFlow
    ) { activities, profile ->
        val activityMap = activities.associateBy { it.date }
        val last7 = DateHelper.lastNDates(7)
        val last30 = DateHelper.lastNDates(30)

        val weeklyPoints = last7.map { date ->
            val act = activityMap[date]
            DayStepPoint(
                date = date,
                shortLabel = DateHelper.formatShortDay(date),
                fullDateLabel = DateHelper.formatMonthDay(date),
                steps = act?.steps ?: 0,
                goalSteps = act?.goalSteps ?: profile.dailyStepGoal
            )
        }

        val monthlyPoints = last30.map { date ->
            val act = activityMap[date]
            DayStepPoint(
                date = date,
                shortLabel = DateHelper.formatDayNumber(date),
                fullDateLabel = DateHelper.formatMonthDay(date),
                steps = act?.steps ?: 0,
                goalSteps = act?.goalSteps ?: profile.dailyStepGoal
            )
        }

        val weeklyAvg = if (weeklyPoints.isNotEmpty()) {
            weeklyPoints.sumOf { it.steps } / weeklyPoints.size
        } else 0

        val monthlyAvg = if (monthlyPoints.isNotEmpty()) {
            monthlyPoints.sumOf { it.steps } / monthlyPoints.size
        } else 0

        val weeklyMet = weeklyPoints.count { it.steps >= it.goalSteps && it.goalSteps > 0 }
        val monthlyMet = monthlyPoints.count { it.steps >= it.goalSteps && it.goalSteps > 0 }

        // Compute streaks across recorded activities sorted ascending
        val sortedAsc = activities.sortedBy { it.date }
        var bestStreak = 0
        var running = 0
        var prevDate: String? = null

        for (act in sortedAsc) {
            val met = act.steps >= act.goalSteps && act.goalSteps > 0
            if (met) {
                if (prevDate != null && isConsecutiveDay(prevDate, act.date)) {
                    running += 1
                } else {
                    running = 1
                }
                if (running > bestStreak) bestStreak = running
                prevDate = act.date
            } else {
                running = 0
                prevDate = null
            }
        }

        // Current streak: count backwards from today (or yesterday if today hasn't met goal yet)
        val today = DateHelper.todayKey()
        val yesterday = DateHelper.dateKeyDaysAgo(1)
        var currentStreak = 0
        val todayAct = activityMap[today]
        val startOffset = if (todayAct != null && todayAct.steps >= todayAct.goalSteps && todayAct.goalSteps > 0) {
            0
        } else {
            val yestAct = activityMap[yesterday]
            if (yestAct != null && yestAct.steps >= yestAct.goalSteps && yestAct.goalSteps > 0) 1 else -1
        }

        if (startOffset >= 0) {
            var offset = startOffset
            while (true) {
                val d = DateHelper.dateKeyDaysAgo(offset)
                val a = activityMap[d]
                if (a != null && a.steps >= a.goalSteps && a.goalSteps > 0) {
                    currentStreak += 1
                    offset += 1
                } else {
                    break
                }
            }
        }

        val bestDay = activities.maxByOrNull { it.steps }
        ProgressSummary(
            currentStreak = currentStreak,
            bestStreak = bestStreak,
            personalBestSteps = bestDay?.steps ?: 0,
            personalBestDate = bestDay?.let { DateHelper.formatMonthDay(it.date) },
            weeklyPoints = weeklyPoints,
            monthlyPoints = monthlyPoints,
            weeklyAverageSteps = weeklyAvg,
            monthlyAverageSteps = monthlyAvg,
            weeklyGoalsMetCount = weeklyMet,
            monthlyGoalsMetCount = monthlyMet
        )
    }

    private fun isConsecutiveDay(earlierKey: String, laterKey: String): Boolean {
        val d1 = DateHelper.parseKey(earlierKey) ?: return false
        val d2 = DateHelper.parseKey(laterKey) ?: return false
        val diffDays = ((d2.time - d1.time) / (1000L * 60L * 60L * 24L))
        return diffDays == 1L
    }

    suspend fun onSensorStepReading(cumulativeSensorSteps: Int) {
        if (cumulativeSensorSteps < 0) return
        stepMutex.withLock {
            val today = DateHelper.todayKey()
            val profile = prefs.userProfileFlow.first()
            val (savedTotal, savedDate) = prefs.getSensorBaseline()

            if (savedTotal < 0) {
                // First ever reading: store baseline without inflating today's steps with pre-install boot steps
                prefs.saveSensorBaseline(cumulativeSensorSteps, today)
                val existing = dao.getDailyActivity(today)
                if (existing == null) {
                    dao.upsertDailyActivity(
                        DailyActivityEntity(date = today, steps = 0, goalSteps = profile.dailyStepGoal)
                    )
                }
                return@withLock
            }

            val delta = when {
                cumulativeSensorSteps < savedTotal -> {
                    // Device rebooted since last reading
                    cumulativeSensorSteps
                }
                savedDate != today -> {
                    // New day started: only count steps accrued since last reading if reasonable, or reset baseline cleanly
                    (cumulativeSensorSteps - savedTotal).coerceAtLeast(0)
                }
                else -> {
                    (cumulativeSensorSteps - savedTotal).coerceAtLeast(0)
                }
            }

            prefs.saveSensorBaseline(cumulativeSensorSteps, today)
            if (delta > 0) {
                val existing = dao.getDailyActivity(today)
                val newSteps = (existing?.steps ?: 0) + delta
                dao.upsertDailyActivity(
                    DailyActivityEntity(
                        date = today,
                        steps = newSteps,
                        goalSteps = existing?.goalSteps ?: profile.dailyStepGoal
                    )
                )
            }
        }
    }

    suspend fun addManualSteps(additionalSteps: Int) {
        if (additionalSteps <= 0) return
        stepMutex.withLock {
            val today = DateHelper.todayKey()
            val profile = prefs.userProfileFlow.first()
            val existing = dao.getDailyActivity(today)
            val updated = (existing?.steps ?: 0) + additionalSteps
            dao.upsertDailyActivity(
                DailyActivityEntity(
                    date = today,
                    steps = updated.coerceAtMost(200_000),
                    goalSteps = profile.dailyStepGoal
                )
            )
        }
    }

    suspend fun setTodaySteps(totalSteps: Int) {
        val safeSteps = totalSteps.coerceIn(0, 200_000)
        stepMutex.withLock {
            val today = DateHelper.todayKey()
            val profile = prefs.userProfileFlow.first()
            dao.upsertDailyActivity(
                DailyActivityEntity(
                    date = today,
                    steps = safeSteps,
                    goalSteps = profile.dailyStepGoal
                )
            )
        }
    }

    suspend fun saveWorkout(
        type: WorkoutType,
        durationSeconds: Long
    ) {
        val profile = prefs.userProfileFlow.first()
        val now = System.currentTimeMillis()
        val today = DateHelper.todayKey()
        val calories = FitnessCalculator.calculateWorkoutCalories(type, durationSeconds, profile.weightKg)
        val distanceKm = FitnessCalculator.calculateWorkoutDistanceKm(type, durationSeconds)

        dao.insertWorkout(
            WorkoutEntity(
                type = type.name,
                durationSeconds = durationSeconds,
                caloriesBurned = calories,
                distanceKm = distanceKm,
                timestampMillis = now,
                date = today
            )
        )

        // Ensure today has a daily activity row so its goal snapshot exists
        val existing = dao.getDailyActivity(today)
        if (existing == null) {
            dao.upsertDailyActivity(
                DailyActivityEntity(date = today, steps = 0, goalSteps = profile.dailyStepGoal)
            )
        }
    }

    suspend fun deleteWorkout(workoutId: Long) {
        dao.deleteWorkoutById(workoutId)
    }

    suspend fun saveUserProfile(profile: UserProfile) {
        prefs.saveUserProfile(profile)
        // Update today's goal snapshot as well
        val today = DateHelper.todayKey()
        val existing = dao.getDailyActivity(today)
        dao.upsertDailyActivity(
            DailyActivityEntity(
                date = today,
                steps = existing?.steps ?: 0,
                goalSteps = profile.dailyStepGoal
            )
        )
    }

    suspend fun updateDistanceUnit(unit: DistanceUnit) = prefs.updateDistanceUnit(unit)
    suspend fun updateWeightUnit(unit: WeightUnit) = prefs.updateWeightUnit(unit)
    suspend fun updateRemindersEnabled(enabled: Boolean) = prefs.updateRemindersEnabled(enabled)
    suspend fun updateThemeMode(mode: ThemeMode) = prefs.updateThemeMode(mode)
    suspend fun setPermissionRationaleDismissed(dismissed: Boolean) =
        prefs.setPermissionRationaleDismissed(dismissed)
}
