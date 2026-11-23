package com.example.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.roundToInt

class StepCounterManager(context: Context) {

    private val sensorManager =
        context.applicationContext.getSystemService(Context.SENSOR_SERVICE) as? SensorManager

    private val stepCounterSensor: Sensor? =
        sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

    val isSensorAvailable: Boolean
        get() = stepCounterSensor != null

    private var activeListener: SensorEventListener? = null

    fun startListening(onTotalStepsUpdate: (Int) -> Unit) {
        val sensor = stepCounterSensor ?: return
        stopListening()

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                val rawValue = event?.values?.firstOrNull() ?: return
                onTotalStepsUpdate(rawValue.roundToInt())
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                // No action required
            }
        }
        activeListener = listener
        sensorManager?.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
    }

    fun stopListening() {
        activeListener?.let { listener ->
            sensorManager?.unregisterListener(listener)
        }
        activeListener = null
    }
}
