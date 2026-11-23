package com.example

import com.example.data.model.BmiCategory
import com.example.data.model.DistanceUnit
import com.example.data.model.UserProfile
import com.example.data.model.WeightUnit
import com.example.data.model.WorkoutType
import com.example.data.repository.FitnessCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun `steps to distance calories and active minutes are realistic`() {
        val profile = UserProfile(
            name = "Alex",
            age = 30,
            heightCm = 175.0,
            weightKg = 70.0,
            dailyStepGoal = 6000
        )

        val distanceKm = FitnessCalculator.stepsToDistanceKm(6000, profile)
        val calories = FitnessCalculator.stepsToCalories(6000, profile)
        val activeMinutes = FitnessCalculator.stepsToActiveMinutes(6000)

        assertTrue(distanceKm in 4.0..5.0)
        assertEquals(240, calories)
        assertEquals(60, activeMinutes)
    }

    @Test
    fun `bmi calculation and categories work accurately`() {
        val healthy = FitnessCalculator.calculateBmi(175.0, 70.0)
        assertNotNull(healthy)
        assertEquals(BmiCategory.HEALTHY, healthy?.category)
        assertEquals(22.86, healthy?.value ?: 0.0, 0.1)

        val invalid = FitnessCalculator.calculateBmi(50.0, 70.0)
        assertNull(invalid)
    }

    @Test
    fun `workout calorie calculation scales with MET and duration`() {
        val runningCalories = FitnessCalculator.calculateWorkoutCalories(
            type = WorkoutType.RUNNING,
            durationSeconds = 1800L, // 30 minutes
            weightKg = 70.0
        )
        assertTrue(runningCalories in 290..320)
    }

    @Test
    fun `unit conversions format cleanly`() {
        val kmFormatted = FitnessCalculator.formatDistance(5.0, DistanceUnit.KM)
        val milesFormatted = FitnessCalculator.formatDistance(5.0, DistanceUnit.MILES)
        assertTrue(kmFormatted.endsWith("km"))
        assertTrue(milesFormatted.endsWith("mi"))

        val lbs = FitnessCalculator.kgToDisplayWeight(70.0, WeightUnit.LBS)
        assertEquals(154.3, lbs, 0.2)
    }
}
