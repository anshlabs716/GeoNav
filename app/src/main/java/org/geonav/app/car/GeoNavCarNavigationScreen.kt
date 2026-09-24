package org.geonav.app.car

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.Distance
import androidx.car.app.model.Pane
import androidx.car.app.model.PaneTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.car.app.navigation.NavigationManager
import androidx.car.app.navigation.NavigationManagerCallback
import androidx.car.app.navigation.model.Destination
import androidx.car.app.navigation.model.Maneuver
import androidx.car.app.navigation.model.NavigationTemplate
import androidx.car.app.navigation.model.RoutingInfo
import androidx.car.app.navigation.model.Step
import androidx.car.app.navigation.model.TravelEstimate
import androidx.car.app.navigation.model.Trip
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.geonav.app.GeoNavApplication
import org.geonav.app.data.model.GeoPoint
import org.geonav.app.data.model.ManeuverType
import org.geonav.app.data.model.Place
import org.geonav.app.data.model.PlaceCategory
import org.geonav.app.data.model.Route
import org.geonav.app.data.model.TransportMode
import java.util.Locale
import java.util.TimeZone

class GeoNavCarNavigationScreen(carContext: CarContext) : Screen(carContext), DefaultLifecycleObserver {

    private val positioningRepo = GeoNavApplication.instance.positioningRepository
    private val routingRepo = GeoNavApplication.instance.routingRepository
    private val placesRepo = GeoNavApplication.instance.placesRepository
    private val vehicleDataManager = VehicleDataManager(carContext)

    private var activeRoute: Route? = null
    private var currentStepIndex: Int = 0
    private var isNavigating: Boolean = false
    private var navigationManager: NavigationManager? = null

    private var homePlace: Place? = null
    private var workPlace: Place? = null
    private var savedPlacesList: List<Place> = emptyList()

    init {
        lifecycle.addObserver(this)
    }

    override fun onCreate(owner: LifecycleOwner) {
        try {
            navigationManager = carContext.getCarService(NavigationManager::class.java).apply {
                setNavigationManagerCallback(object : NavigationManagerCallback {
                    override fun onStopNavigation() {
                        stopNavigation()
                    }
                })
            }
        } catch (e: Exception) {
            // NavigationManager not supported by vehicle
        }

        vehicleDataManager.startListening(carContext.mainExecutor)

        // Observe saved places
        lifecycleScope.launch {
            placesRepo.savedPlaces.collectLatest { places: List<Place> ->
                savedPlacesList = places
                homePlace = places.find { it.category == PlaceCategory.HOME || it.name.equals("Home", true) }
                workPlace = places.find { it.category == PlaceCategory.WORK || it.name.equals("Work", true) }
                invalidate()
            }
        }

        // Observe location updates for turn progression
        lifecycleScope.launch {
            positioningRepo.fusionState.collectLatest { fusion ->
                val loc = fusion.location
                if (isNavigating && activeRoute != null && loc != null) {
                    val steps = activeRoute!!.steps
                    if (currentStepIndex < steps.size) {
                        val currentStep = steps[currentStepIndex]
                        val distToStepEnd = loc.distanceTo(currentStep.location)
                        if (distToStepEnd < 25.0 && currentStepIndex < steps.size - 1) {
                            currentStepIndex++
                            updateClusterTurnGuidance()
                            invalidate()
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        vehicleDataManager.stopListening()
        stopNavigation()
    }

    override fun onGetTemplate(): Template {
        return if (isNavigating && activeRoute != null) {
            buildActiveNavigationTemplate()
        } else {
            buildExploreMenuTemplate()
        }
    }

    private fun buildActiveNavigationTemplate(): Template {
        val route = activeRoute ?: return buildExploreMenuTemplate()
        val steps = route.steps
        val step = if (currentStepIndex < steps.size) steps[currentStepIndex] else steps.last()
        val nextStep = if (currentStepIndex + 1 < steps.size) steps[currentStepIndex + 1] else null

        val currentLoc = positioningRepo.fusionState.value.location
        val distToStep = if (currentLoc != null) currentLoc.distanceTo(step.location) else step.distanceMeters

        val carManeuver = mapManeuverTypeToCarManeuver(step.maneuverType)

        val carStepBuilder = Step.Builder(step.instruction)
            .setManeuver(carManeuver)
            .setRoad(step.roadName)

        val carStep = carStepBuilder.build()
        val carDistance = Distance.create(distToStep.coerceAtLeast(10.0), Distance.UNIT_METERS)

        val routingInfoBuilder = RoutingInfo.Builder()
            .setCurrentStep(carStep, carDistance)

        if (nextStep != null) {
            val nextCarStep = Step.Builder(nextStep.instruction)
                .setManeuver(mapManeuverTypeToCarManeuver(nextStep.maneuverType))
                .setRoad(nextStep.roadName)
                .build()
            routingInfoBuilder.setNextStep(nextCarStep)
        }

        val actionStrip = ActionStrip.Builder()
            .addAction(
                Action.Builder()
                    .setTitle("End Trip")
                    .setOnClickListener { stopNavigation() }
                    .build()
            )
            .build()

        return NavigationTemplate.Builder()
            .setNavigationInfo(routingInfoBuilder.build())
            .setActionStrip(actionStrip)
            .build()
    }

    private fun buildExploreMenuTemplate(): Template {
        val paneBuilder = Pane.Builder()

        val fusion = positioningRepo.fusionState.value
        val speedText = if (fusion.hasFix) {
            String.format(Locale.US, "GPS Speed: %d km/h", fusion.speedKmh.toInt())
        } else {
            "GPS: Acquiring fix..."
        }

        // Quick Destinations Row
        paneBuilder.addRow(
            Row.Builder()
                .setTitle("Quick Navigation")
                .addText(speedText)
                .build()
        )

        // Home
        val homeSub = homePlace?.address ?: "Tap to navigate to Home"
        paneBuilder.addRow(
            Row.Builder()
                .setTitle("Home")
                .addText(homeSub)
                .setOnClickListener {
                    if (homePlace != null) {
                        startNavigationTo(homePlace!!.location, homePlace!!.name)
                    }
                }
                .build()
        )

        // Work
        val workSub = workPlace?.address ?: "Tap to navigate to Work"
        paneBuilder.addRow(
            Row.Builder()
                .setTitle("Work")
                .addText(workSub)
                .setOnClickListener {
                    if (workPlace != null) {
                        startNavigationTo(workPlace!!.location, workPlace!!.name)
                    }
                }
                .build()
        )

        // Quick Category: Fuel / Charging
        paneBuilder.addRow(
            Row.Builder()
                .setTitle("Nearby Fuel & Charging")
                .addText("Find closest service station or EV charger")
                .setOnClickListener {
                    searchNearbyAndRoute(PlaceCategory.FUEL)
                }
                .build()
        )

        // Quick Category: Parking
        paneBuilder.addRow(
            Row.Builder()
                .setTitle("Nearby Parking")
                .addText("Find parking garages and lots")
                .setOnClickListener {
                    searchNearbyAndRoute(PlaceCategory.PARKING)
                }
                .build()
        )

        val actionStrip = ActionStrip.Builder()
            .addAction(
                Action.Builder()
                    .setTitle("Recenter")
                    .setOnClickListener {
                        positioningRepo.startPositioning()
                        invalidate()
                    }
                    .build()
            )
            .build()

        return PaneTemplate.Builder(paneBuilder.build())
            .setTitle("GeoNav")
            .setActionStrip(actionStrip)
            .build()
    }

    private fun searchNearbyAndRoute(category: PlaceCategory) {
        val currentLoc = positioningRepo.fusionState.value.location ?: return
        lifecycleScope.launch {
            val query = when (category) {
                PlaceCategory.FUEL -> "fuel"
                PlaceCategory.EV_CHARGER -> "ev charger"
                PlaceCategory.PARKING -> "parking"
                else -> "service"
            }
            val results = GeoNavApplication.instance.searchRepository.search(query, currentLoc)
            if (results.isNotEmpty()) {
                val best = results.first().place
                startNavigationTo(best.location, best.name)
            }
        }
    }

    private fun startNavigationTo(dest: GeoPoint, destName: String) {
        val currentLoc = positioningRepo.fusionState.value.location ?: return
        lifecycleScope.launch {
            val routes = routingRepo.calculateRoutes(currentLoc, dest, TransportMode.DRIVE)
            if (routes.isNotEmpty()) {
                activeRoute = routes.first()
                currentStepIndex = 0
                isNavigating = true

                try {
                    navigationManager?.navigationStarted()
                    updateClusterTurnGuidance()
                } catch (e: Exception) {
                    // Ignored if cluster not supported
                }
                invalidate()
            }
        }
    }

    private fun stopNavigation() {
        if (isNavigating) {
            isNavigating = false
            activeRoute = null
            currentStepIndex = 0
            try {
                navigationManager?.navigationEnded()
            } catch (e: Exception) {
                // Ignore
            }
            invalidate()
        }
    }

    /**
     * Updates minimal turn guidance on instrument cluster / driver display.
     */
    private fun updateClusterTurnGuidance() {
        val route = activeRoute ?: return
        val navMgr = navigationManager ?: return
        val steps = route.steps
        if (currentStepIndex >= steps.size) return

        val step = steps[currentStepIndex]
        val currentLoc = positioningRepo.fusionState.value.location
        val dist = if (currentLoc != null) currentLoc.distanceTo(step.location) else step.distanceMeters

        try {
            val carStep = Step.Builder(step.instruction)
                .setManeuver(mapManeuverTypeToCarManeuver(step.maneuverType))
                .setRoad(step.roadName)
                .build()

            val trip = Trip.Builder()
                .addDestination(
                    Destination.Builder()
                        .setName(route.summary)
                        .setAddress("Destination")
                        .build(),
                    TravelEstimate.Builder(
                        Distance.create(dist.coerceAtLeast(10.0), Distance.UNIT_METERS),
                        androidx.car.app.model.DateTimeWithZone.create(
                            System.currentTimeMillis() + (route.durationSeconds * 1000),
                            TimeZone.getDefault()
                        )
                    ).build()
                )
                .addStep(
                    carStep,
                    TravelEstimate.Builder(
                        Distance.create(dist.coerceAtLeast(10.0), Distance.UNIT_METERS),
                        androidx.car.app.model.DateTimeWithZone.create(
                            System.currentTimeMillis() + (step.durationSeconds * 1000),
                            TimeZone.getDefault()
                        )
                    ).build()
                )
                .build()

            navMgr.updateTrip(trip)
        } catch (e: Exception) {
            // Vehicle cluster data not supported
        }
    }

    private fun mapManeuverTypeToCarManeuver(type: ManeuverType): Maneuver {
        val carType = when (type) {
            ManeuverType.DEPART -> Maneuver.TYPE_DEPART
            ManeuverType.ARRIVE -> Maneuver.TYPE_DESTINATION
            ManeuverType.TURN_LEFT -> Maneuver.TYPE_TURN_NORMAL_LEFT
            ManeuverType.TURN_SLIGHT_LEFT -> Maneuver.TYPE_TURN_SLIGHT_LEFT
            ManeuverType.TURN_SHARP_LEFT -> Maneuver.TYPE_TURN_SHARP_LEFT
            ManeuverType.TURN_RIGHT -> Maneuver.TYPE_TURN_NORMAL_RIGHT
            ManeuverType.TURN_SLIGHT_RIGHT -> Maneuver.TYPE_TURN_SLIGHT_RIGHT
            ManeuverType.TURN_SHARP_RIGHT -> Maneuver.TYPE_TURN_SHARP_RIGHT
            ManeuverType.U_TURN -> Maneuver.TYPE_U_TURN_LEFT
            ManeuverType.ROUNDABOUT_ENTER, ManeuverType.ROUNDABOUT_EXIT -> Maneuver.TYPE_ROUNDABOUT_ENTER_AND_EXIT_CW
            ManeuverType.MERGE -> Maneuver.TYPE_MERGE_RIGHT
            ManeuverType.RAMP_LEFT -> Maneuver.TYPE_ON_RAMP_NORMAL_LEFT
            ManeuverType.RAMP_RIGHT -> Maneuver.TYPE_ON_RAMP_NORMAL_RIGHT
            ManeuverType.FORK_LEFT -> Maneuver.TYPE_FORK_LEFT
            ManeuverType.FORK_RIGHT -> Maneuver.TYPE_FORK_RIGHT
            ManeuverType.CONTINUE_STRAIGHT -> Maneuver.TYPE_STRAIGHT
        }
        return Maneuver.Builder(carType).build()
    }
}
