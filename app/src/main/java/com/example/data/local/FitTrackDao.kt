package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FitTrackDao {

    @Query("SELECT * FROM daily_activities WHERE date = :date LIMIT 1")
    fun observeDailyActivity(date: String): Flow<DailyActivityEntity?>

    @Query("SELECT * FROM daily_activities WHERE date = :date LIMIT 1")
    suspend fun getDailyActivity(date: String): DailyActivityEntity?

    @Query("SELECT * FROM daily_activities ORDER BY date DESC")
    fun observeAllDailyActivities(): Flow<List<DailyActivityEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDailyActivity(activity: DailyActivityEntity)

    @Query("SELECT * FROM workouts ORDER BY timestampMillis DESC")
    fun observeAllWorkouts(): Flow<List<WorkoutEntity>>

    @Query("SELECT * FROM workouts WHERE date = :date ORDER BY timestampMillis DESC")
    fun observeWorkoutsForDate(date: String): Flow<List<WorkoutEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkout(workout: WorkoutEntity): Long

    @Query("DELETE FROM workouts WHERE id = :id")
    suspend fun deleteWorkoutById(id: Long)
}
