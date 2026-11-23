package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_activities")
data class DailyActivityEntity(
    @PrimaryKey val date: String, // yyyy-MM-dd
    val steps: Int = 0,
    val goalSteps: Int = 6000
)

@Entity(tableName = "workouts")
data class WorkoutEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val durationSeconds: Long,
    val caloriesBurned: Int,
    val distanceKm: Double,
    val timestampMillis: Long,
    val date: String // yyyy-MM-dd
)
