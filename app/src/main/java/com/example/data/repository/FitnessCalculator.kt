package com.example.data.repository

import com.example.data.model.BmiCategory
import com.example.data.model.BmiResult
import com.example.data.model.DistanceUnit
import com.example.data.model.UserProfile
import com.example.data.model.WeightUnit
import com.example.data.model.WorkoutType
import kotlin.math.pow
import kotlin.math.roundToInt

object FitnessCalculator {

    private const val KM_TO_MILES = 0.621371
    private const val KG_TO_LBS = 2.20462

    fun calculateStrideMeters(heightCm: Double): Double {
        val safeHeight = heightCm.coerceIn(90.0, 250.0)
        return (safeHeight * 0.415) / 100.0
    }

    fun stepsToDistanceKm(steps: Int, profile: UserProfile): Double {
        if (steps <= 0) return 0.0
        return (steps * calculateStrideMeters(profile.heightCm)) / 1000.0
    }

    fun stepsToCalories(steps: Int, profile: UserProfile): Int {
        if (steps <= 0) return 0
        val weightFactor = (profile.weightKg.coerceIn(25.0, 300.0)) / 70.0
        return (steps * 0.04 * weightFactor).roundToInt()
    }

    fun stepsToActiveMinutes(steps: Int): Int {
        if (steps <= 0) return 0
        return (steps / 100.0).roundToInt()
    }

    fun calculateWorkoutCalories(
        type: WorkoutType,
        durationSeconds: Long,
        weightKg: Double
    ): Int {
        if (durationSeconds <= 0L) return 0
        val safeWeight = weightKg.coerceIn(25.0, 300.0)
        val hours = durationSeconds / 3600.0
        return (type.metValue * safeWeight * hours).roundToInt().coerceAtLeast(1)
    }

    fun calculateWorkoutDistanceKm(type: WorkoutType, durationSeconds: Long): Double {
        if (!type.tracksDistance || durationSeconds <= 0L) return 0.0
        val hours = durationSeconds / 3600.0
        return type.avgSpeedKmh * hours
    }

    fun calculateBmi(heightCm: Double, weightKg: Double): BmiResult? {
        if (heightCm < 90.0 || heightCm > 250.0 || weightKg < 25.0 || weightKg > 300.0) {
            return null
        }
        val heightMeters = heightCm / 100.0
        val bmi = weightKg / heightMeters.pow(2)
        val category = when {
            bmi < 18.5 -> BmiCategory.UNDERWEIGHT
            bmi < 25.0 -> BmiCategory.HEALTHY
            bmi < 30.0 -> BmiCategory.OVERWEIGHT
            else -> BmiCategory.OBESE
        }
        return BmiResult(value = bmi, category = category)
    }

    fun formatDistance(distanceKm: Double, unit: DistanceUnit): String {
        val converted = when (unit) {
            DistanceUnit.KM -> distanceKm
            DistanceUnit.MILES -> distanceKm * KM_TO_MILES
        }
        return String.format("%.2f %s", converted, unit.shortLabel)
    }

    fun formatDistanceValueOnly(distanceKm: Double, unit: DistanceUnit): String {
        val converted = when (unit) {
            DistanceUnit.KM -> distanceKm
            DistanceUnit.MILES -> distanceKm * KM_TO_MILES
        }
        return String.format("%.2f", converted)
    }

    fun kgToDisplayWeight(weightKg: Double, unit: WeightUnit): Double {
        return when (unit) {
            WeightUnit.KG -> weightKg
            WeightUnit.LBS -> weightKg * KG_TO_LBS
        }
    }

    fun displayWeightToKg(displayValue: Double, unit: WeightUnit): Double {
        return when (unit) {
            WeightUnit.KG -> displayValue
            WeightUnit.LBS -> displayValue / KG_TO_LBS
        }
    }

    fun formatDuration(seconds: Long): String {
        val hrs = seconds / 3600
        val mins = (seconds % 3600) / 60
        val secs = seconds % 60
        return if (hrs > 0) {
            String.format("%02d:%02d:%02d", hrs, mins, secs)
        } else {
            String.format("%02d:%02d", mins, secs)
        }
    }

    fun formatDurationReadable(seconds: Long): String {
        val hrs = seconds / 3600
        val mins = (seconds % 3600) / 60
        val secs = seconds % 60
        return when {
            hrs > 0 -> "${hrs}h ${mins}m"
            mins > 0 -> "${mins}m ${secs}s"
            else -> "${secs}s"
        }
    }
}
