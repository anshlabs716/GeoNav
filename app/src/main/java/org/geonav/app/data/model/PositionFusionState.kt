package org.geonav.app.data.model

enum class PositioningSource(val displayName: String) {
    GNSS("GNSS / GPS"),
    CELLULAR_NETWORK("Network / Cellular"),
    WIFI_POSITIONING("Wi-Fi Positioning"),
    SENSOR_DEAD_RECKONING("Sensor Dead Reckoning"),
    BLE_BEACON("BLE Beacon / Tunnel"),
    VEHICLE_SENSOR("Vehicle Sensor (Android Auto)")
}

enum class FusionConfidence(val label: String) {
    NO_FIX("No GPS Fix"),
    SEARCHING("Acquiring Satellites..."),
    HIGH("High Accuracy"),
    MEDIUM("Medium Accuracy"),
    LOW("Low Accuracy"),
    DEAD_RECKONING("Dead Reckoning (Sensors)"),
    LOST("Signal Lost")
}

data class PositionFusionState(
    val location: GeoPoint? = null,
    val hasFix: Boolean = false,
    val accuracyMeters: Float? = null,
    val speedKmh: Float = 0.0f,
    val speedLimitKmh: Int? = null,
    val headingDegrees: Float = 0.0f,
    val isTrueNorth: Boolean = true,
    val magneticDeclination: Float = 0.0f,
    val altitudeMeters: Double? = null,
    val source: PositioningSource? = null,
    val confidence: FusionConfidence = FusionConfidence.NO_FIX,
    val satellitesInUse: Int? = null,
    val totalSatellites: Int? = null,
    val isTunnelMode: Boolean = false,
    val availableSensors: Set<String> = emptySet(),
    val timestamp: Long = System.currentTimeMillis()
)
