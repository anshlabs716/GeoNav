package org.geonav.app.domain.engine

import org.geonav.app.data.model.GeoPoint
import org.geonav.app.data.model.ManeuverType
import org.geonav.app.data.model.Route
import org.geonav.app.data.model.RouteStep

data class GuidanceState(
    val currentStep: RouteStep,
    val nextStep: RouteStep?,
    val distanceToNextManeuverMeters: Double,
    val remainingDistanceMeters: Double,
    val remainingDurationSeconds: Long,
    val isArrived: Boolean = false,
    val isOffRoute: Boolean = false
)

class TurnByTurnEngine {

    private var activeRoute: Route? = null
    private var currentStepIndex: Int = 0

    fun startRoute(route: Route) {
        activeRoute = route
        currentStepIndex = 0
    }

    fun stopRoute() {
        activeRoute = null
        currentStepIndex = 0
    }

    fun updateProgress(currentLocation: GeoPoint): GuidanceState? {
        val route = activeRoute ?: return null
        if (route.steps.isEmpty()) return null

        val currentStep = route.steps.getOrNull(currentStepIndex) ?: route.steps.last()
        val nextStep = route.steps.getOrNull(currentStepIndex + 1)

        val distanceToStep = currentLocation.distanceTo(currentStep.location)

        // If user is within 25 meters of current maneuver point, advance to next step
        if (distanceToStep < 25.0 && currentStepIndex < route.steps.size - 1) {
            currentStepIndex++
        }

        // Check arrival
        val lastStep = route.steps.last()
        val distanceToDestination = currentLocation.distanceTo(lastStep.location)
        val isArrived = distanceToDestination < 20.0

        // Calculate total remaining distance
        var remainingDist = distanceToStep
        for (i in (currentStepIndex + 1) until route.steps.size) {
            remainingDist += route.steps[i].distanceMeters
        }

        val estimatedRemainingSeconds = (remainingDist / 13.8).toLong() // approx 50 km/h average

        return GuidanceState(
            currentStep = currentStep,
            nextStep = nextStep,
            distanceToNextManeuverMeters = distanceToStep,
            remainingDistanceMeters = remainingDist,
            remainingDurationSeconds = estimatedRemainingSeconds,
            isArrived = isArrived,
            isOffRoute = distanceToStep > 250.0 // Reroute if strayed far from current step
        )
    }
}
