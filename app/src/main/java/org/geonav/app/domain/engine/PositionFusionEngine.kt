package org.geonav.app.domain.engine

import android.hardware.GeomagneticField
import android.location.Location
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.geonav.app.data.model.FusionConfidence
import org.geonav.app.data.model.GeoPoint
import org.geonav.app.data.model.PositionFusionState
import org.geonav.app.data.model.PositioningSource
import kotlin.math.cos
import kotlin.math.sin

class PositionFusionEngine {

    private val _fusionState = MutableStateFlow(
        PositionFusionState(
            location = null,
            hasFix = false,
            accuracyMeters = null,
            speedKmh = 0.0f,
            speedLimitKmh = null,
            headingDegrees = 0.0f,
            isTrueNorth = true,
            magneticDeclination = 0.0f,
            altitudeMeters = null,
            source = null,
            confidence = FusionConfidence.NO_FIX,
            satellitesInUse = null,
            totalSatellites = null,
            isTunnelMode = false,
            availableSensors = emptySet()
        )
    )
    val fusionState: StateFlow<PositionFusionState> = _fusionState.asStateFlow()

    private var previousLocation: Location? = null
    private var lastValidFixTime: Long = 0L
    private var deadReckoningSpeed: Float = 0.0f
    private var deadReckoningHeading: Float = 0.0f
    private val availableSensorsSet = mutableSetOf<String>()

    fun setAvailableSensors(sensors: Set<String>) {
        availableSensorsSet.clear()
        availableSensorsSet.addAll(sensors)
        _fusionState.value = _fusionState.value.copy(availableSensors = availableSensorsSet.toSet())
    }

    fun onSearchingForFix() {
        if (!_fusionState.value.hasFix) {
            _fusionState.value = _fusionState.value.copy(
                confidence = FusionConfidence.SEARCHING
            )
        }
    }

    fun onGnssStatusChanged(inUseCount: Int, totalCount: Int) {
        val current = _fusionState.value
        _fusionState.value = current.copy(
            satellitesInUse = inUseCount,
            totalSatellites = totalCount
        )
    }

    /**
     * Updates position from Android Location API (GNSS / Network).
     * Calculates real speed, accuracy, and true north declination.
     */
    fun onRawLocationUpdate(location: Location) {
        val now = System.currentTimeMillis()
        val hasPrev = previousLocation != null

        // Calculate speed from location.speed or consecutive fixes
        val calculatedSpeedKmh = if (location.hasSpeed() && location.speed >= 0f) {
            location.speed * 3.6f
        } else if (hasPrev) {
            val dist = previousLocation!!.distanceTo(location)
            val dtSec = (location.time - previousLocation!!.time) / 1000.0f
            if (dtSec in 0.2f..10.0f) {
                (dist / dtSec) * 3.6f
            } else {
                0.0f
            }
        } else {
            0.0f
        }

        previousLocation = location
        lastValidFixTime = now
        deadReckoningSpeed = calculatedSpeedKmh
        if (location.hasBearing()) {
            deadReckoningHeading = location.bearing
        }

        // Magnetic declination for true north calculation
        val altitudeForField = if (location.hasAltitude()) location.altitude.toFloat() else 0.0f
        val geoField = GeomagneticField(
            location.latitude.toFloat(),
            location.longitude.toFloat(),
            altitudeForField,
            now
        )
        val declination = geoField.declination

        // Confidence based strictly on GPS horizontal accuracy
        val confidence = when {
            location.accuracy <= 10.0f -> FusionConfidence.HIGH
            location.accuracy <= 25.0f -> FusionConfidence.MEDIUM
            else -> FusionConfidence.LOW
        }

        val source = if (location.provider == "gps") {
            PositioningSource.GNSS
        } else {
            PositioningSource.CELLULAR_NETWORK
        }

        _fusionState.value = _fusionState.value.copy(
            location = GeoPoint(location.latitude, location.longitude, if (location.hasAltitude()) location.altitude else null),
            hasFix = true,
            accuracyMeters = location.accuracy,
            speedKmh = calculatedSpeedKmh,
            headingDegrees = if (location.hasBearing()) location.bearing else _fusionState.value.headingDegrees,
            isTrueNorth = true,
            magneticDeclination = declination,
            altitudeMeters = if (location.hasAltitude()) location.altitude else null,
            source = source,
            confidence = confidence,
            isTunnelMode = false,
            timestamp = now
        )
    }

    /**
     * Updates device orientation from hardware sensors.
     */
    fun onSensorOrientationUpdate(azimuthDegrees: Float, isTrueNorthRequested: Boolean) {
        val current = _fusionState.value
        val adjustedHeading = if (isTrueNorthRequested) {
            (azimuthDegrees + current.magneticDeclination + 360f) % 360f
        } else {
            azimuthDegrees
        }

        _fusionState.value = current.copy(
            headingDegrees = adjustedHeading,
            isTrueNorth = isTrueNorthRequested
        )
    }

    /**
     * Dead reckoning integration step when GNSS signal is lost (e.g., tunnel or deep urban canyon).
     */
    fun onDeadReckoningStep(deltaSeconds: Float, tunnelGeometryDetected: Boolean) {
        val current = _fusionState.value
        val currentLocation = current.location ?: return
        val now = System.currentTimeMillis()
        val timeSinceGnss = now - lastValidFixTime

        if (timeSinceGnss > 3000 && current.hasFix) {
            if (timeSinceGnss > 30000) {
                // Too long without fix, mark as signal lost
                _fusionState.value = current.copy(
                    confidence = FusionConfidence.LOST,
                    source = PositioningSource.SENSOR_DEAD_RECKONING
                )
                return
            }

            // Project new coordinate along dead reckoning vector
            val distanceTraveledMeters = (deadReckoningSpeed / 3.6f) * deltaSeconds
            if (distanceTraveledMeters > 0.05f) {
                val earthRadius = 6371000.0
                val headingRad = Math.toRadians(deadReckoningHeading.toDouble())
                val latRad = Math.toRadians(currentLocation.latitude)
                val lonRad = Math.toRadians(currentLocation.longitude)

                val newLatRad = latRad + (distanceTraveledMeters / earthRadius) * cos(headingRad)
                val newLonRad = lonRad + (distanceTraveledMeters / (earthRadius * cos(latRad))) * sin(headingRad)

                val currentAccuracy = current.accuracyMeters ?: 10.0f
                _fusionState.value = current.copy(
                    location = GeoPoint(Math.toDegrees(newLatRad), Math.toDegrees(newLonRad), current.altitudeMeters),
                    accuracyMeters = currentAccuracy + (deltaSeconds * 0.8f),
                    source = if (tunnelGeometryDetected) PositioningSource.BLE_BEACON else PositioningSource.SENSOR_DEAD_RECKONING,
                    confidence = FusionConfidence.DEAD_RECKONING,
                    isTunnelMode = tunnelGeometryDetected,
                    timestamp = now
                )
            }
        }
    }
}
