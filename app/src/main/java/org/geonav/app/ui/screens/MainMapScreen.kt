package org.geonav.app.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.geonav.app.GeoNavApplication
import org.geonav.app.data.model.GeoPoint
import org.geonav.app.data.model.MapStyleType
import org.geonav.app.data.model.Place
import org.geonav.app.data.model.Route
import org.geonav.app.data.model.SearchResult
import org.geonav.app.data.model.TransportMode
import org.geonav.app.ui.components.FloatingSearchBar
import org.geonav.app.ui.components.ManeuverCard
import org.geonav.app.ui.components.MapControlsBar
import org.geonav.app.ui.components.NavigationBottomBar
import org.geonav.app.ui.components.PlaceDetailSheet
import org.geonav.app.ui.components.PositioningStatusBadge
import org.geonav.app.ui.components.RoutePreviewSheet
import org.geonav.app.ui.map.MapLibreMapView

enum class NavigationUiState {
    IDLE,
    PLACE_SELECTED,
    ROUTE_PREVIEW,
    NAVIGATING
}

@Composable
fun MainMapScreen(
    onOpenDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = GeoNavApplication.instance
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // State flows from repositories
    val fusionState by app.positioningRepository.fusionState.collectAsState()
    val currentMapStyle by app.settingsRepository.currentStyle.collectAsState()
    val satelliteProviderUrl by app.settingsRepository.satelliteProviderUrl.collectAsState()
    val speedUnit by app.settingsRepository.speedUnit.collectAsState()
    val savedPlaces by app.placesRepository.allSavedPlaces.collectAsState(initial = emptyList())

    // Camera and map state
    var cameraTilt by remember { mutableDoubleStateOf(0.0) }
    var cameraBearing by remember { mutableFloatStateOf(0.0f) }
    var isFollowingLocation by remember { mutableStateOf(true) }
    var recenterCounter by remember { mutableLongStateOf(0L) }

    // Navigation and UI workflow states
    var uiState by remember { mutableStateOf(NavigationUiState.IDLE) }
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<SearchResult>>(emptyList()) }
    var selectedPlace by remember { mutableStateOf<Place?>(null) }
    var availableRoutes by remember { mutableStateOf<List<Route>>(emptyList()) }
    var selectedRoute by remember { mutableStateOf<Route?>(null) }
    var selectedMode by remember { mutableStateOf(TransportMode.DRIVE) }
    var currentStepIndex by remember { mutableStateOf(0) }

    // Asynchronous search
    fun performSearch(query: String) {
        searchQuery = query
        if (query.isNotBlank()) {
            coroutineScope.launch {
                searchResults = app.searchRepository.search(query, fusionState.location)
            }
        } else {
            searchResults = emptyList()
        }
    }

    // Direct toggle layers in sequence
    fun cycleMapLayer() {
        val nextStyle = when (currentMapStyle) {
            MapStyleType.STANDARD -> MapStyleType.DARK
            MapStyleType.DARK -> MapStyleType.TERRAIN
            MapStyleType.TERRAIN -> MapStyleType.CYCLING
            MapStyleType.CYCLING -> MapStyleType.SATELLITE
            MapStyleType.SATELLITE -> MapStyleType.STANDARD
            else -> MapStyleType.STANDARD
        }
        app.settingsRepository.setMapStyle(nextStyle)

        if (nextStyle.isSatelliteRelated && satelliteProviderUrl.isBlank()) {
            Toast.makeText(context, "Satellite: Provider not configured (Using standard fallback)", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Map Layer: ${nextStyle.title}", Toast.LENGTH_SHORT).show()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("main_map_screen")
    ) {
        // MapLibre View
        MapLibreMapView(
            mapStyle = currentMapStyle,
            customSatelliteUrl = satelliteProviderUrl,
            userLocation = fusionState.location,
            isFollowingLocation = isFollowingLocation,
            recenterTrigger = recenterCounter,
            tilt = cameraTilt,
            activeRoute = selectedRoute,
            destinationPoint = selectedPlace?.location,
            onMapClicked = {
                if (uiState == NavigationUiState.PLACE_SELECTED) {
                    uiState = NavigationUiState.IDLE
                    selectedPlace = null
                }
            },
            onUserGesture = {
                // When user pans or pinches map, stop yanking camera back
                isFollowingLocation = false
            },
            onCameraHeadingChanged = { heading ->
                cameraBearing = heading
            }
        )

        // Top Search Bar (visible when not actively navigating)
        if (uiState != NavigationUiState.NAVIGATING) {
            FloatingSearchBar(
                query = searchQuery,
                onQueryChange = { performSearch(it) },
                searchResults = searchResults,
                onPlaceSelected = { place ->
                    selectedPlace = place
                    searchQuery = ""
                    searchResults = emptyList()
                    uiState = NavigationUiState.PLACE_SELECTED
                },
                onMenuClicked = onOpenDrawer,
                onCategoryFilter = { cat ->
                    performSearch(cat)
                },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
            )
        }

        // Active Navigation Turn Maneuver Top Card
        if (uiState == NavigationUiState.NAVIGATING && selectedRoute != null) {
            val steps = selectedRoute!!.steps
            val step = if (currentStepIndex < steps.size) steps[currentStepIndex] else steps.lastOrNull()
            if (step != null) {
                val dist = fusionState.location?.distanceTo(step.location) ?: step.distanceMeters
                ManeuverCard(
                    currentStep = step,
                    distanceMeters = dist,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                )
            }
        }

        // Right-Side Map Controls Bar
        MapControlsBar(
            compassHeading = cameraBearing,
            is3D = cameraTilt > 10.0,
            isFollowingLocation = isFollowingLocation,
            onCompassClick = {
                recenterCounter++
            },
            on3DToggle = {
                cameraTilt = if (cameraTilt > 10.0) 0.0 else 55.0
            },
            onLayersClick = {
                cycleMapLayer()
            },
            onRecenterClick = {
                if (fusionState.location == null) {
                    app.positioningRepository.startPositioning()
                    Toast.makeText(context, "Acquiring GPS fix...", Toast.LENGTH_SHORT).show()
                } else {
                    recenterCounter++
                    isFollowingLocation = true
                }
            },
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp)
        )

        // Bottom-Left Positioning Status Badge
        if (uiState != NavigationUiState.NAVIGATING) {
            PositioningStatusBadge(
                state = fusionState,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 16.dp, bottom = 24.dp)
            )
        }

        // Place Detail Sheet
        AnimatedVisibility(
            visible = uiState == NavigationUiState.PLACE_SELECTED && selectedPlace != null,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            selectedPlace?.let { place ->
                val isSaved = savedPlaces.any { it.id == place.id }
                PlaceDetailSheet(
                    place = place,
                    isSaved = isSaved,
                    onGetDirections = { targetPlace ->
                        val origin = fusionState.location ?: GeoPoint(37.7749, -122.4194)
                        coroutineScope.launch {
                            val routes = app.routingRepository.calculateRoutes(
                                origin = origin,
                                destination = targetPlace.location,
                                mode = selectedMode
                            )
                            availableRoutes = routes
                            selectedRoute = routes.firstOrNull()
                            uiState = NavigationUiState.ROUTE_PREVIEW
                        }
                    },
                    onToggleSave = { targetPlace ->
                        coroutineScope.launch {
                            if (isSaved) {
                                app.placesRepository.removePlace(targetPlace.id)
                            } else {
                                app.placesRepository.savePlace(targetPlace)
                            }
                        }
                    },
                    onSelectEntrance = { entrance ->
                        val origin = fusionState.location ?: GeoPoint(37.7749, -122.4194)
                        coroutineScope.launch {
                            val routes = app.routingRepository.calculateRoutes(
                                origin = origin,
                                destination = entrance.location,
                                mode = selectedMode
                            )
                            availableRoutes = routes
                            selectedRoute = routes.firstOrNull()
                            uiState = NavigationUiState.ROUTE_PREVIEW
                        }
                    },
                    onDismiss = {
                        uiState = NavigationUiState.IDLE
                        selectedPlace = null
                    }
                )
            }
        }

        // Route Preview Sheet
        AnimatedVisibility(
            visible = uiState == NavigationUiState.ROUTE_PREVIEW && selectedRoute != null,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            RoutePreviewSheet(
                routes = availableRoutes,
                selectedRoute = selectedRoute,
                selectedMode = selectedMode,
                onModeSelected = { mode ->
                    selectedMode = mode
                    selectedPlace?.let { place ->
                        val origin = fusionState.location ?: GeoPoint(37.7749, -122.4194)
                        coroutineScope.launch {
                            val routes = app.routingRepository.calculateRoutes(
                                origin = origin,
                                destination = place.location,
                                mode = mode
                            )
                            availableRoutes = routes
                            selectedRoute = routes.firstOrNull()
                        }
                    }
                },
                onRouteSelected = { route ->
                    selectedRoute = route
                },
                onStartNavigation = { route ->
                    currentStepIndex = 0
                    cameraTilt = 55.0 // Switch to 3D perspective for driving
                    isFollowingLocation = true
                    uiState = NavigationUiState.NAVIGATING
                },
                onDismiss = {
                    uiState = NavigationUiState.IDLE
                    selectedRoute = null
                }
            )
        }

        // Active Navigation Bottom Bar
        AnimatedVisibility(
            visible = uiState == NavigationUiState.NAVIGATING && selectedRoute != null,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            val route = selectedRoute!!
            NavigationBottomBar(
                remainingSeconds = route.durationSeconds,
                remainingDistanceMeters = route.distanceMeters,
                currentSpeedKmh = fusionState.speedKmh,
                speedUnit = speedUnit,
                speedLimitKmh = fusionState.speedLimitKmh,
                onStopNavigation = {
                    cameraTilt = 0.0
                    uiState = NavigationUiState.IDLE
                    selectedRoute = null
                }
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}
