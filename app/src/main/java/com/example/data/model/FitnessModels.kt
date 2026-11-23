package com.example.data.model

enum class WorkoutType(
    val displayName: String,
    val metValue: Double,
    val tracksDistance: Boolean,
    val avgSpeedKmh: Double
) {
    WALKING("Walking", metValue = 3.5, tracksDistance = true, avgSpeedKmh = 5.0),
    RUNNING("Running", metValue = 8.8, tracksDistance = true, avgSpeedKmh = 9.5),
    CYCLING("Cycling", metValue = 7.0, tracksDistance = true, avgSpeedKmh = 18.0),
    YOGA("Yoga", metValue = 2.8, tracksDistance = false, avgSpeedKmh = 0.0),
    GYM("Gym", metValue = 5.5, tracksDistance = false, avgSpeedKmh = 0.0);

    companion object {
        fun fromName(name: String): WorkoutType =
            entries.firstOrNull { it.name == name } ?: WALKING
    }
}

enum class DistanceUnit(val label: String, val shortLabel: String) {
    KM("Kilometers (km)", "km"),
    MILES("Miles (mi)", "mi")
}

enum class WeightUnit(val label: String, val shortLabel: String) {
    KG("Kilograms (kg)", "kg"),
    LBS("Pounds (lbs)", "lbs")
}

enum class ThemeMode(val label: String) {
    SYSTEM("System"),
    LIGHT("Light"),
    DARK("Dark")
}

data class UserProfile(
    val name: String = "",
    val age: Int = 28,
    val heightCm: Double = 172.0,
    val weightKg: Double = 70.0,
    val dailyStepGoal: Int = 6000
)

data class UserSettings(
    val distanceUnit: DistanceUnit = DistanceUnit.KM,
    val weightUnit: WeightUnit = WeightUnit.KG,
    val remindersEnabled: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM
)

enum class BmiCategory(val label: String) {
    UNDERWEIGHT("Underweight"),
    HEALTHY("Healthy Weight"),
    OVERWEIGHT("Overweight"),
    OBESE("Obese")
}

data class BmiResult(
    val value: Double,
    val category: BmiCategory
)

data class DayStepPoint(
    val date: String,
    val shortLabel: String,
    val fullDateLabel: String,
    val steps: Int,
    val goalSteps: Int
)

data class JournalWorkoutItem(
    val id: Long,
    val type: WorkoutType,
    val durationSeconds: Long,
    val caloriesBurned: Int,
    val distanceKm: Double,
    val timestampMillis: Long,
    val date: String,
    val timeLabel: String
)

data class JournalDayGroup(
    val date: String,
    val displayDate: String,
    val totalSteps: Int,
    val goalSteps: Int,
    val totalDistanceKm: Double,
    val totalCalories: Int,
    val totalActiveMinutes: Int,
    val workouts: List<JournalWorkoutItem>
)

data class ProgressSummary(
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val personalBestSteps: Int = 0,
    val personalBestDate: String? = null,
    val weeklyPoints: List<DayStepPoint> = emptyList(),
    val monthlyPoints: List<DayStepPoint> = emptyList(),
    val weeklyAverageSteps: Int = 0,
    val monthlyAverageSteps: Int = 0,
    val weeklyGoalsMetCount: Int = 0,
    val monthlyGoalsMetCount: Int = 0
)
