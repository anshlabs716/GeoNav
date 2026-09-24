package org.geonav.app.data.model

import kotlinx.serialization.Serializable

@Serializable
enum class TransportMode(val label: String) {
    DRIVE("Drive"),
    WALK("Walk"),
    CYCLE("Cycle"),
    HIKE("Hike"),
    TRANSIT("Transit")
}

@Serializable
enum class ManeuverType {
    DEPART,
    TURN_SLIGHT_LEFT,
    TURN_LEFT,
    TURN_SHARP_LEFT,
    TURN_SLIGHT_RIGHT,
    TURN_RIGHT,
    TURN_SHARP_RIGHT,
    CONTINUE_STRAIGHT,
    U_TURN,
    ROUNDABOUT_ENTER,
    ROUNDABOUT_EXIT,
    MERGE,
    RAMP_LEFT,
    RAMP_RIGHT,
    FORK_LEFT,
    FORK_RIGHT,
    ARRIVE
}

@Serializable
data class RouteStep(
    val instruction: String,
    val maneuverType: ManeuverType,
    val distanceMeters: Double,
    val durationSeconds: Long,
    val roadName: String,
    val location: GeoPoint,
    val laneGuidance: String? = null
)

@Serializable
data class Route(
    val id: String,
    val summary: String,
    val distanceMeters: Double,
    val durationSeconds: Long,
    val points: List<GeoPoint>,
    val steps: List<RouteStep>,
    val mode: TransportMode,
    val hasTolls: Boolean = false,
    val hasHighways: Boolean = false,
    val isFastest: Boolean = true,
    val isOfflineFallback: Boolean = false,
    val trafficCondition: String? = null,
    val elevationGainMeters: Double = 0.0,
    val elevationLossMeters: Double = 0.0
)
