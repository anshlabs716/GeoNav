package org.geonav.app.ui.map

import android.graphics.Color
import android.os.Bundle
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import org.geonav.app.data.model.GeoPoint
import org.geonav.app.data.model.MapStyleType
import org.geonav.app.data.model.Route
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory.circleColor
import org.maplibre.android.style.layers.PropertyFactory.circleRadius
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeColor
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeWidth
import org.maplibre.android.style.layers.PropertyFactory.lineCap
import org.maplibre.android.style.layers.PropertyFactory.lineColor
import org.maplibre.android.style.layers.PropertyFactory.lineJoin
import org.maplibre.android.style.layers.PropertyFactory.lineWidth
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point

private const val ROUTE_SOURCE_ID = "geonav_route_source"
private const val ROUTE_LAYER_ID = "geonav_route_layer"
private const val PUCK_SOURCE_ID = "geonav_puck_source"
private const val PUCK_LAYER_ID = "geonav_puck_layer"
private const val PUCK_ACCURACY_LAYER_ID = "geonav_puck_accuracy_layer"

@Composable
fun MapLibreMapView(
    modifier: Modifier = Modifier,
    mapStyle: MapStyleType,
    customSatelliteUrl: String? = null,
    userLocation: GeoPoint? = null,
    isFollowingLocation: Boolean = true,
    recenterTrigger: Long = 0L,
    tilt: Double = 0.0,
    activeRoute: Route? = null,
    destinationPoint: GeoPoint? = null,
    onMapClicked: (GeoPoint) -> Unit = {},
    onUserGesture: () -> Unit = {},
    onCameraHeadingChanged: (Float) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mapView = remember { MapView(context) }
    var mapInstance by remember { mutableStateOf<MapLibreMap?>(null) }
    var currentStyleObject by remember { mutableStateOf<Style?>(null) }

    // Map lifecycle
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_CREATE -> mapView.onCreate(Bundle())
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Load or switch map style
    val styleUri = remember(mapStyle, customSatelliteUrl) {
        mapStyle.getStyleUri(customSatelliteUrl)
    }

    LaunchedEffect(styleUri) {
        mapInstance?.setStyle(Style.Builder().fromUri(styleUri)) { style ->
            currentStyleObject = style
            updateRouteLayer(style, activeRoute)
            updateLocationPuck(style, userLocation)
        }
    }

    // Camera follow user location
    LaunchedEffect(userLocation, isFollowingLocation) {
        if (isFollowingLocation && userLocation != null) {
            mapInstance?.let { map ->
                val currentZoom = map.cameraPosition.zoom.coerceAtLeast(14.0)
                val target = LatLng(userLocation.latitude, userLocation.longitude)
                val cameraPosition = CameraPosition.Builder()
                    .target(target)
                    .zoom(currentZoom)
                    .tilt(tilt)
                    .build()
                map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition), 750)
            }
        }
    }

    // Explicit Recenter button trigger
    LaunchedEffect(recenterTrigger) {
        if (recenterTrigger > 0L && userLocation != null) {
            mapInstance?.let { map ->
                val target = LatLng(userLocation.latitude, userLocation.longitude)
                val cameraPosition = CameraPosition.Builder()
                    .target(target)
                    .zoom(16.0)
                    .tilt(tilt)
                    .bearing(0.0) // Reset to North
                    .build()
                map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition), 600)
            }
        }
    }

    // Perspective / 3D tilt update
    LaunchedEffect(tilt) {
        mapInstance?.let { map ->
            val pos = CameraPosition.Builder(map.cameraPosition)
                .tilt(tilt)
                .build()
            map.animateCamera(CameraUpdateFactory.newCameraPosition(pos), 400)
        }
    }

    // Update location puck on the map
    LaunchedEffect(userLocation, currentStyleObject) {
        currentStyleObject?.let { style ->
            updateLocationPuck(style, userLocation)
        }
    }

    // Update route polyline on the map
    LaunchedEffect(activeRoute, currentStyleObject) {
        currentStyleObject?.let { style ->
            updateRouteLayer(style, activeRoute)
            // Fit route in camera overview if activeRoute just arrived
            if (activeRoute != null && activeRoute.points.size >= 2) {
                mapInstance?.let { map ->
                    val minLat = activeRoute.points.minOf { it.latitude }
                    val maxLat = activeRoute.points.maxOf { it.latitude }
                    val minLon = activeRoute.points.minOf { it.longitude }
                    val maxLon = activeRoute.points.maxOf { it.longitude }
                    val centerLat = (minLat + maxLat) / 2.0
                    val centerLon = (minLon + maxLon) / 2.0
                    val pos = CameraPosition.Builder()
                        .target(LatLng(centerLat, centerLon))
                        .zoom(13.0)
                        .tilt(tilt)
                        .build()
                    map.animateCamera(CameraUpdateFactory.newCameraPosition(pos), 900)
                }
            }
        }
    }

    AndroidView(
        factory = {
            mapView.apply {
                getMapAsync { map ->
                    mapInstance = map

                    // CRITICAL: Ensure full gestures without restriction
                    map.uiSettings.apply {
                        isZoomGesturesEnabled = true
                        isScrollGesturesEnabled = true
                        isRotateGesturesEnabled = true
                        isTiltGesturesEnabled = true
                        isDoubleTapGesturesEnabled = true
                        isQuickZoomGesturesEnabled = true
                        isCompassEnabled = false // Custom M3 compass FAB used
                        isAttributionEnabled = true
                        isLogoEnabled = false
                    }

                    // Camera listeners
                    map.addOnCameraMoveStartedListener { reason ->
                        if (reason == MapLibreMap.OnCameraMoveStartedListener.REASON_API_GESTURE) {
                            onUserGesture()
                        }
                    }

                    map.addOnCameraMoveListener {
                        onCameraHeadingChanged(map.cameraPosition.bearing.toFloat())
                    }

                    map.addOnMapClickListener { latLng ->
                        onMapClicked(GeoPoint(latLng.latitude, latLng.longitude))
                        true
                    }

                    // Set initial style
                    map.setStyle(Style.Builder().fromUri(styleUri)) { style ->
                        currentStyleObject = style
                        updateRouteLayer(style, activeRoute)
                        updateLocationPuck(style, userLocation)
                    }

                    // Initial camera center
                    val initialCenter = userLocation ?: GeoPoint(37.7749, -122.4194)
                    map.cameraPosition = CameraPosition.Builder()
                        .target(LatLng(initialCenter.latitude, initialCenter.longitude))
                        .zoom(if (userLocation != null) 15.0 else 12.0)
                        .tilt(tilt)
                        .bearing(0.0)
                        .build()
                }
            }
        },
        modifier = modifier.fillMaxSize()
    )
}

private fun updateLocationPuck(style: Style, userLocation: GeoPoint?) {
    if (!style.isFullyLoaded) return

    val point = userLocation?.let { Point.fromLngLat(it.longitude, it.latitude) }
    val feature = point?.let { Feature.fromGeometry(it) }
    val collection = feature?.let { FeatureCollection.fromFeature(it) } ?: FeatureCollection.fromFeatures(emptyList())

    val existingSource = style.getSourceAs<GeoJsonSource>(PUCK_SOURCE_ID)
    if (existingSource != null) {
        existingSource.setGeoJson(collection)
    } else {
        val newSource = GeoJsonSource(PUCK_SOURCE_ID, collection)
        style.addSource(newSource)

        // Outer accuracy pulse circle
        val accuracyLayer = CircleLayer(PUCK_ACCURACY_LAYER_ID, PUCK_SOURCE_ID).apply {
            setProperties(
                circleRadius(16f),
                circleColor(Color.argb(50, 0, 150, 136)),
                circleStrokeColor(Color.argb(120, 0, 150, 136)),
                circleStrokeWidth(1.5f)
            )
        }
        style.addLayer(accuracyLayer)

        // Inner puck solid dot
        val puckLayer = CircleLayer(PUCK_LAYER_ID, PUCK_SOURCE_ID).apply {
            setProperties(
                circleRadius(8.5f),
                circleColor(Color.rgb(0, 137, 123)),
                circleStrokeColor(Color.WHITE),
                circleStrokeWidth(2.5f)
            )
        }
        style.addLayer(puckLayer)
    }
}

private fun updateRouteLayer(style: Style, activeRoute: Route?) {
    if (!style.isFullyLoaded) return

    val coordinates = activeRoute?.points?.map { Point.fromLngLat(it.longitude, it.latitude) } ?: emptyList()
    val lineString = if (coordinates.size >= 2) LineString.fromLngLats(coordinates) else null
    val feature = lineString?.let { Feature.fromGeometry(it) }
    val collection = feature?.let { FeatureCollection.fromFeature(it) } ?: FeatureCollection.fromFeatures(emptyList())

    val existingSource = style.getSourceAs<GeoJsonSource>(ROUTE_SOURCE_ID)
    if (existingSource != null) {
        existingSource.setGeoJson(collection)
    } else {
        val newSource = GeoJsonSource(ROUTE_SOURCE_ID, collection)
        style.addSource(newSource)

        val lineLayer = LineLayer(ROUTE_LAYER_ID, ROUTE_SOURCE_ID).apply {
            setProperties(
                lineWidth(6.5f),
                lineColor(Color.rgb(0, 150, 136)),
                lineCap("round"),
                lineJoin("round")
            )
        }
        style.addLayer(lineLayer)
    }
}
