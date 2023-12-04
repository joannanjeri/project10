package com.example.project10

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import android.Manifest
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.IOException

/**
 * sensor activity for displaying sensor data and location
 * handles sensor events and displays temperature and air pressure
 * navigates to gesture activity on a fling gesture
 */
class SensorActivity : ComponentActivity(), SensorEventListener {
    /**
     * sensor manager for accessing device sensors
     */
    private lateinit var sensorManager: SensorManager

    /**
     * sensor for ambient temperature
     */
    private var temperatureSensor: Sensor? = null

    /**
     * sensor for another sensor data like air pressure
     */
    private var otherSensor: Sensor? = null

    /**
     * state for storing the current temperature reading
     */
    private var temperature = mutableStateOf("Waiting for data...")

    /**
     * state for storing the current reading of the other sensor
     */
    private var otherSensorValue = mutableStateOf("Waiting for data...")

    /**
     * sets up the activity content using jetpack compose
     * registers sensor event listeners
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        temperatureSensor = sensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE)
        otherSensor = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)

        sensorManager.registerListener(this, temperatureSensor, SensorManager.SENSOR_DELAY_NORMAL)
        sensorManager.registerListener(this, otherSensor, SensorManager.SENSOR_DELAY_NORMAL)

        setContent {
            var city by remember { mutableStateOf("Fetching city...") }
            var state by remember { mutableStateOf("Fetching state...") }

            LaunchedEffect(Unit) {
                val locationResult = getLocation()
                city = locationResult.first
                state = locationResult.second
            }

            SensorActivityContent(
                temperature = temperature.value,
                otherSensorValue = otherSensorValue.value,
                city = city,
                state = state,
                onFling = { navigateToGestureActivity() }
            )
        }

    }

    /**
     * navigates to the gesture activity
     */
    private fun navigateToGestureActivity() {
        val intent = Intent(this, GestureActivity::class.java)
        startActivity(intent)
    }

    /**
     * fetches the current location of the device
     * uses geocoder to get the city and state from the location
     * @return a pair of strings representing city and state
     */
    private suspend fun getLocation(): Pair<String, String> = withContext(Dispatchers.IO) {
        var city = "Unknown"
        var state = "Unknown"

        if (ContextCompat.checkSelfPermission(this@SensorActivity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this@SensorActivity, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this@SensorActivity, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), 100)
            return@withContext Pair(city, state)
        }

        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

        if (!isGPSEnabled && !isNetworkEnabled) {
            return@withContext Pair(city, state)
        }

        var location: Location? = null
        if (isNetworkEnabled) {
            location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        }

        if (isGPSEnabled && location == null) {
            location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        }


        location?.let {
            try {
                val geocoder = Geocoder(this@SensorActivity, Locale.getDefault())
                val addresses = geocoder.getFromLocation(it.latitude, it.longitude, 1)
                if (addresses != null && addresses.isNotEmpty()) {
                    city = addresses[0].locality ?: city
                    state = addresses[0].adminArea ?: state
                }
            } catch (e: IOException) {
            }
        }

        return@withContext Pair(city, state)
    }

    /**
     * handles sensor event changes
     * updates temperature and other sensor values
     * @param event sensor event containing new sensor readings
     */
    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            when (event.sensor.type) {
                Sensor.TYPE_AMBIENT_TEMPERATURE -> temperature.value = "${event.values[0]} °C"
                Sensor.TYPE_PRESSURE -> otherSensorValue.value = "${event.values[0]} hPa"
            }
        }
    }

    /**
     * handles changes in sensor accuracy
     * displays a toast message with the current sensor accuracy
     * @param sensor the sensor whose accuracy has changed
     * @param accuracy the new accuracy of the sensor
     */
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        when (accuracy) {
            SensorManager.SENSOR_STATUS_ACCURACY_LOW -> showToast("Sensor accuracy is low")
            SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> showToast("Sensor accuracy is medium")
            SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> showToast("Sensor accuracy is high")
            SensorManager.SENSOR_STATUS_NO_CONTACT -> showToast("Sensor has no contact")
            SensorManager.SENSOR_STATUS_UNRELIABLE -> showToast("Sensor data is unreliable")
        }
    }

    /**
     * displays a toast message
     * @param message the message to be displayed in the toast
     */
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    /**
     * composable function for displaying sensor activity content
     * displays temperature, other sensor value, and location data
     * includes a button for navigating to the gesture activity
     * @param temperature current temperature reading
     * @param otherSensorValue current reading of the other sensor
     * @param city current city as determined by location
     * @param state current state as determined by location
     * @param onFling function to be called when a fling gesture is detected on the button
     */
    @Composable
    fun SensorActivityContent(
        temperature: String,
        otherSensorValue: String,
        city: String,
        state: String,
        onFling: () -> Unit
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = "Joanna Njeri", fontSize = 24.sp, color = Color.Black)
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Location:", fontSize = 18.sp, color = Color.Black)
            Text(text = "City: $city", fontSize = 18.sp, color = Color.Black)
            Text(text = "State: $state", fontSize = 18.sp, color = Color.Black)
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Temperature: $temperature", fontSize = 18.sp, color = Color.Black)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Air Pressure: $otherSensorValue", fontSize = 18.sp, color = Color.Black)
            Spacer(modifier = Modifier.height(32.dp))
            GesturePlaygroundButton(onFling = onFling)
        }
    }

    /**
     * composable function for the gesture playground button
     * detects fling gestures and calls the provided onFling function
     * @param onFling function to be called when a fling gesture is detected
     */
    @Composable
    fun GesturePlaygroundButton(onFling: () -> Unit) {
        val flingDetector = remember { FlingGestureDetector() }

        Button(
            onClick = { },
            colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDrag = { change, dragAmount -> flingDetector.onDrag(dragAmount) },
                        onDragEnd = {
                            if (flingDetector.isFling()) {
                                onFling()
                            }
                        }
                    )
                }
        ) {
           Text(text = "GESTURE PLAYGROUND", fontSize = 18.sp, color = Color.White)
        }
    }

    /**
     * class for detecting fling gestures
     * accumulates total drag and determines if it constitutes a fling
     */
    class FlingGestureDetector {
        private var totalDrag = Offset.Zero

        fun onDrag(dragAmount: Offset) {
            totalDrag += dragAmount
        }
        fun isFling(): Boolean {
            val flingThreshold = 1000f
            return totalDrag.getDistance() > flingThreshold
        }
    }

    /**
     * preview for sensor activity content
     */

    @Preview(showBackground = true)
    @Composable
    fun PreviewSensorActivityContent() {
        SensorActivityContent(
            temperature = "22°C",
            otherSensorValue = "1013.25 hPa",
            city = "Bloomington",
            state = "Indiana",
            onFling = {}
        )
    }

    /**
     * unregisters sensor listeners when the activity is paused
     */
    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    /**
     * registers sensor listeners when the activity resumes
     */
    override fun onResume() {
        super.onResume()
        sensorManager.registerListener(this, temperatureSensor, SensorManager.SENSOR_DELAY_NORMAL)
        sensorManager.registerListener(this, otherSensor, SensorManager.SENSOR_DELAY_NORMAL)
    }

}