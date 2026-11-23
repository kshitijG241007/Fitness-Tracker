package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.DistanceUnit
import com.example.data.model.ThemeMode
import com.example.data.model.UserProfile
import com.example.data.model.UserSettings
import com.example.data.model.WeightUnit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "fittrack_prefs")

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val NAME = stringPreferencesKey("user_name")
        val AGE = intPreferencesKey("user_age")
        val HEIGHT_CM = doublePreferencesKey("user_height_cm")
        val WEIGHT_KG = doublePreferencesKey("user_weight_kg")
        val DAILY_STEP_GOAL = intPreferencesKey("daily_step_goal")

        val DISTANCE_UNIT = stringPreferencesKey("distance_unit")
        val WEIGHT_UNIT = stringPreferencesKey("weight_unit")
        val REMINDERS_ENABLED = booleanPreferencesKey("reminders_enabled")
        val THEME_MODE = stringPreferencesKey("theme_mode")

        val LAST_SENSOR_TOTAL = intPreferencesKey("last_sensor_total")
        val LAST_SENSOR_DATE = stringPreferencesKey("last_sensor_date")
        val PERMISSION_RATIONALE_DISMISSED = booleanPreferencesKey("permission_rationale_dismissed")
    }

    val userProfileFlow: Flow<UserProfile> = context.dataStore.data.map { prefs ->
        UserProfile(
            name = prefs[Keys.NAME] ?: "",
            age = prefs[Keys.AGE] ?: 28,
            heightCm = prefs[Keys.HEIGHT_CM] ?: 172.0,
            weightKg = prefs[Keys.WEIGHT_KG] ?: 70.0,
            dailyStepGoal = prefs[Keys.DAILY_STEP_GOAL] ?: 6000
        )
    }

    val userSettingsFlow: Flow<UserSettings> = context.dataStore.data.map { prefs ->
        UserSettings(
            distanceUnit = prefs[Keys.DISTANCE_UNIT]?.let {
                runCatching { DistanceUnit.valueOf(it) }.getOrNull()
            } ?: DistanceUnit.KM,
            weightUnit = prefs[Keys.WEIGHT_UNIT]?.let {
                runCatching { WeightUnit.valueOf(it) }.getOrNull()
            } ?: WeightUnit.KG,
            remindersEnabled = prefs[Keys.REMINDERS_ENABLED] ?: false,
            themeMode = prefs[Keys.THEME_MODE]?.let {
                runCatching { ThemeMode.valueOf(it) }.getOrNull()
            } ?: ThemeMode.SYSTEM
        )
    }

    val permissionDismissedFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.PERMISSION_RATIONALE_DISMISSED] ?: false
    }

    suspend fun saveUserProfile(profile: UserProfile) {
        context.dataStore.edit { prefs ->
            prefs[Keys.NAME] = profile.name
            prefs[Keys.AGE] = profile.age
            prefs[Keys.HEIGHT_CM] = profile.heightCm
            prefs[Keys.WEIGHT_KG] = profile.weightKg
            prefs[Keys.DAILY_STEP_GOAL] = profile.dailyStepGoal
        }
    }

    suspend fun updateDistanceUnit(unit: DistanceUnit) {
        context.dataStore.edit { it[Keys.DISTANCE_UNIT] = unit.name }
    }

    suspend fun updateWeightUnit(unit: WeightUnit) {
        context.dataStore.edit { it[Keys.WEIGHT_UNIT] = unit.name }
    }

    suspend fun updateRemindersEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.REMINDERS_ENABLED] = enabled }
    }

    suspend fun updateThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setPermissionRationaleDismissed(dismissed: Boolean) {
        context.dataStore.edit { it[Keys.PERMISSION_RATIONALE_DISMISSED] = dismissed }
    }

    suspend fun getSensorBaseline(): Pair<Int, String> {
        val prefs = context.dataStore.data.first()
        return (prefs[Keys.LAST_SENSOR_TOTAL] ?: -1) to (prefs[Keys.LAST_SENSOR_DATE] ?: "")
    }

    suspend fun saveSensorBaseline(totalSteps: Int, date: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LAST_SENSOR_TOTAL] = totalSteps
            prefs[Keys.LAST_SENSOR_DATE] = date
        }
    }
}
