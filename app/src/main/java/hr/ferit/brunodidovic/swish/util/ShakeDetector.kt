package hr.ferit.brunodidovic.swish.util

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlin.math.sqrt

private const val SHAKE_THRESHOLD = 2.7f      // g-force; resting is ~1.0
private const val SHAKE_COOLDOWN_MS = 1000L   // ignore repeats within 1s

@Composable
fun ShakeDetector(
    enabled: Boolean = true,
    onShake: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    // always call the latest onShake without re-registering the sensor
    val currentOnShake by rememberUpdatedState(onShake)

    DisposableEffect(lifecycleOwner, enabled) {
        if (!enabled) {
            onDispose { }
        } else {
            val sensorManager =
                context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
            val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

            var lastShake = 0L
            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    val x = event.values[0]
                    val y = event.values[1]
                    val z = event.values[2]
                    val gForce = sqrt(x * x + y * y + z * z) / SensorManager.GRAVITY_EARTH
                    if (gForce > SHAKE_THRESHOLD) {
                        val now = System.currentTimeMillis()
                        if (now - lastShake > SHAKE_COOLDOWN_MS) {
                            lastShake = now
                            currentOnShake()
                        }
                    }
                }
                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
            }

            // register while resumed, unregister while paused
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_RESUME ->
                        accelerometer?.let {
                            sensorManager.registerListener(
                                listener, it, SensorManager.SENSOR_DELAY_UI
                            )
                        }
                    Lifecycle.Event.ON_PAUSE ->
                        sensorManager.unregisterListener(listener)
                    else -> {}
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)

            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
                sensorManager.unregisterListener(listener)
            }
        }
    }
}