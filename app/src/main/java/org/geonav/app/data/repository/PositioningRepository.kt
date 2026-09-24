package org.geonav.app.data.repository

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.GnssStatus
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.geonav.app.data.model.PositionFusionState
import org.geonav.app.domain.engine.PositionFusionEngine

class PositioningRepository(private val context: Context) : LocationListener, SensorEventListener {

    private val fusionEngine = PositionFusionEngine()
    val fusionState: StateFlow<PositionFusionState> = fusionEngine.fusionState

    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager

    private val _isGpsEnabled = MutableStateFlow(false)
    val isGpsEnabled: StateFlow<Boolean> = _isGpsEnabled.asStateFlow()

    private val _hasPermission = MutableStateFlow(false)
    val hasPermission: StateFlow<Boolean> = _hasPermission.asStateFlow()

    private val accelerometerReading = FloatArray(3)
    private val magnetometerReading = FloatArray(3)
    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)

    private var isTracking = false
    private var gnssStatusCallback: GnssStatus.Callback? = null

    init {
        detectAvailableSensors()
    }

    private fun detectAvailableSensors() {
        val detected = mutableSetOf<String>()
        sensorManager?.let { sm ->
            if (sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) detected.add("Accelerometer")
            if (sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) != null) detected.add("Magnetometer")
            if (sm.getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null) detected.add("Gyroscope")
            if (sm.getDefaultSensor(Sensor.TYPE_PRESSURE) != null) detected.add("Barometer")
            if (sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) != null) detected.add("Rotation Vector")
        }
        if (context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            detected.add("BLE Beacon")
        }
        fusionEngine.setAvailableSensors(detected)
    }

    fun checkPermissions(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarse = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val granted = fine || coarse
        _hasPermission.value = granted
        return granted
    }

    @SuppressLint("MissingPermission")
    fun startPositioning() {
        if (!checkPermissions()) {
            return
        }

        if (isTracking) return
        isTracking = true
        fusionEngine.onSearchingForFix()

        try {
            locationManager?.let { lm ->
                val gpsEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
                val netEnabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
                _isGpsEnabled.value = gpsEnabled || netEnabled

                if (gpsEnabled) {
                    lm.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        1000L,
                        0.5f,
                        this
                    )
                }

                if (netEnabled) {
                    lm.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER,
                        2000L,
                        2.0f,
                        this
                    )
                }

                // Check latest real fix from providers
                val lastGps = if (gpsEnabled) lm.getLastKnownLocation(LocationManager.GPS_PROVIDER) else null
                val lastNet = if (netEnabled) lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER) else null
                val bestLast = when {
                    lastGps != null && lastNet != null -> if (lastGps.time > lastNet.time) lastGps else lastNet
                    lastGps != null -> lastGps
                    else -> lastNet
                }
                bestLast?.let { onLocationChanged(it) }

                // GNSS Satellite status monitoring
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    val callback = object : GnssStatus.Callback() {
                        override fun onSatelliteStatusChanged(status: GnssStatus) {
                            var usedCount = 0
                            val totalCount = status.satelliteCount
                            for (i in 0 until totalCount) {
                                if (status.usedInFix(i)) {
                                    usedCount++
                                }
                            }
                            fusionEngine.onGnssStatusChanged(usedCount, totalCount)
                        }
                    }
                    gnssStatusCallback = callback
                    lm.registerGnssStatusCallback(callback, null)
                }
            }
        } catch (e: SecurityException) {
            _hasPermission.value = false
        }

        // Register sensors
        sensorManager?.let { sm ->
            sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let {
                sm.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
            }
            sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)?.let {
                sm.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
            }
        }
    }

    fun stopPositioning() {
        if (!isTracking) return
        isTracking = false
        try {
            locationManager?.removeUpdates(this)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && gnssStatusCallback != null) {
                locationManager?.unregisterGnssStatusCallback(gnssStatusCallback!!)
                gnssStatusCallback = null
            }
            sensorManager?.unregisterListener(this)
        } catch (e: Exception) {
            // Ignore
        }
    }

    override fun onLocationChanged(location: Location) {
        fusionEngine.onRawLocationUpdate(location)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, accelerometerReading, 0, accelerometerReading.size)
        } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, magnetometerReading, 0, magnetometerReading.size)
        }

        if (SensorManager.getRotationMatrix(rotationMatrix, null, accelerometerReading, magnetometerReading)) {
            SensorManager.getOrientation(rotationMatrix, orientationAngles)
            val azimuthRad = orientationAngles[0]
            val azimuthDeg = ((Math.toDegrees(azimuthRad.toDouble()) + 360) % 360).toFloat()
            fusionEngine.onSensorOrientationUpdate(azimuthDeg, isTrueNorthRequested = true)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    override fun onProviderEnabled(provider: String) {
        _isGpsEnabled.value = true
    }
    override fun onProviderDisabled(provider: String) {
        // Handled
    }
    @Deprecated("Deprecated in Java")
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
}
