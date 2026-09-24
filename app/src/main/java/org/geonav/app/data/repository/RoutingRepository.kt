package org.geonav.app.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.geonav.app.data.model.GeoPoint
import org.geonav.app.data.model.ManeuverType
import org.geonav.app.data.model.Route
import org.geonav.app.data.model.RouteStep
import org.geonav.app.data.model.TransportMode
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlin.math.roundToLong

class RoutingRepository {

    suspend fun calculateRoutes(
        origin: GeoPoint,
        destination: GeoPoint,
        mode: TransportMode,
        avoidTolls: Boolean = false,
        avoidHighways: Boolean = false
    ): List<Route> = withContext(Dispatchers.IO) {
        val osrmProfile = when (mode) {
            TransportMode.DRIVE -> "driving"
            TransportMode.WALK -> "walking"
            TransportMode.CYCLE -> "cycling"
            TransportMode.HIKE -> "walking"
            TransportMode.TRANSIT -> "driving"
        }

        try {
            val urlString = String.format(
                Locale.US,
                "https://router.project-osrm.org/route/v1/%s/%.6f,%.6f;%%20%.6f,%.6f?overview=full&geometries=geojson&steps=true&alternatives=true",
                osrmProfile,
                origin.longitude, origin.latitude,
                destination.longitude, destination.latitude
            )

            val url = URL(urlString.replace("%20", ""))
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 5000
                readTimeout = 5000
                setRequestProperty("User-Agent", "GeoNav-Android-App/1.0")
            }

            if (connection.responseCode == 200) {
                val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(responseText)
                if (json.optString("code") == "Ok") {
                    val routesArray = json.optJSONArray("routes")
                    if (routesArray != null && routesArray.length() > 0) {
                        val parsedRoutes = mutableListOf<Route>()
                        for (i in 0 until routesArray.length()) {
                            val rObj = routesArray.getJSONObject(i)
                            val distance = rObj.optDouble("distance", 0.0)
                            val duration = rObj.optDouble("duration", 0.0).roundToLong()

                            // Points from GeoJSON LineString
                            val geom = rObj.optJSONObject("geometry")
                            val coords = geom?.optJSONArray("coordinates")
                            val points = mutableListOf<GeoPoint>()
                            if (coords != null) {
                                for (c in 0 until coords.length()) {
                                    val pair = coords.getJSONArray(c)
                                    val lon = pair.getDouble(0)
                                    val lat = pair.getDouble(1)
                                    points.add(GeoPoint(lat, lon))
                                }
                            }

                            // Steps from first leg
                            val legs = rObj.optJSONArray("legs")
                            val steps = mutableListOf<RouteStep>()
                            var summaryName = ""
                            if (legs != null && legs.length() > 0) {
                                val leg = legs.getJSONObject(0)
                                summaryName = leg.optString("summary", "")
                                val stepsArray = leg.optJSONArray("steps")
                                if (stepsArray != null) {
                                    for (s in 0 until stepsArray.length()) {
                                        val sObj = stepsArray.getJSONObject(s)
                                        val sDist = sObj.optDouble("distance", 0.0)
                                        val sDur = sObj.optDouble("duration", 0.0).roundToLong()
                                        val sName = sObj.optString("name", "")
                                        val maneuver = sObj.optJSONObject("maneuver")

                                        val mTypeStr = maneuver?.optString("type", "turn") ?: "turn"
                                        val mModStr = maneuver?.optString("modifier", "") ?: ""
                                        val mLocArray = maneuver?.optJSONArray("location")
                                        val mLoc = if (mLocArray != null && mLocArray.length() >= 2) {
                                            GeoPoint(mLocArray.getDouble(1), mLocArray.getDouble(0))
                                        } else {
                                            origin
                                        }

                                        val mType = mapManeuver(mTypeStr, mModStr)
                                        val instruction = formatInstruction(mType, sName)

                                        steps.add(
                                            RouteStep(
                                                instruction = instruction,
                                                maneuverType = mType,
                                                distanceMeters = sDist,
                                                durationSeconds = sDur,
                                                roadName = if (sName.isNotBlank()) sName else "Unnamed Road",
                                                location = mLoc
                                            )
                                        )
                                    }
                                }
                            }

                            if (summaryName.isBlank()) {
                                summaryName = if (i == 0) "Fastest Route" else "Alternative ${i + 1}"
                            }

                            parsedRoutes.add(
                                Route(
                                    id = "osrm_route_$i",
                                    summary = summaryName,
                                    distanceMeters = distance,
                                    durationSeconds = duration,
                                    points = points,
                                    steps = steps,
                                    mode = mode,
                                    hasTolls = false,
                                    hasHighways = false,
                                    isFastest = (i == 0),
                                    isOfflineFallback = false
                                )
                            )
                        }
                        if (parsedRoutes.isNotEmpty()) {
                            return@withContext parsedRoutes
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Server offline or network error - use clean offline direct calculation
        }

        // Offline / Direct fallback with clear indicator
        listOf(calculateOfflineFallback(origin, destination, mode))
    }

    private fun calculateOfflineFallback(origin: GeoPoint, destination: GeoPoint, mode: TransportMode): Route {
        val directDistance = origin.distanceTo(destination)
        val speedMs = when (mode) {
            TransportMode.DRIVE -> 13.8 // 50 km/h
            TransportMode.WALK -> 1.4   // 5 km/h
            TransportMode.CYCLE -> 4.5  // 16 km/h
            TransportMode.HIKE -> 1.1   // 4 km/h
            TransportMode.TRANSIT -> 8.3// 30 km/h
        }
        val duration = (directDistance / speedMs).roundToLong()

        val steps = listOf(
            RouteStep(
                instruction = "Depart toward destination",
                maneuverType = ManeuverType.DEPART,
                distanceMeters = directDistance * 0.9,
                durationSeconds = duration,
                roadName = "Direct Route",
                location = origin
            ),
            RouteStep(
                instruction = "Arrive at destination",
                maneuverType = ManeuverType.ARRIVE,
                distanceMeters = 10.0,
                durationSeconds = 10,
                roadName = "Destination",
                location = destination
            )
        )

        return Route(
            id = "offline_direct_route",
            summary = "Direct Route (Offline / Direct Line)",
            distanceMeters = directDistance,
            durationSeconds = duration,
            points = listOf(origin, destination),
            steps = steps,
            mode = mode,
            hasTolls = false,
            hasHighways = false,
            isFastest = true,
            isOfflineFallback = true
        )
    }

    private fun mapManeuver(type: String, modifier: String): ManeuverType {
        return when (type) {
            "depart" -> ManeuverType.DEPART
            "arrive" -> ManeuverType.ARRIVE
            "roundabout" -> ManeuverType.ROUNDABOUT_ENTER
            "exit roundabout" -> ManeuverType.ROUNDABOUT_EXIT
            "merge" -> ManeuverType.MERGE
            "on ramp" -> if (modifier.contains("left")) ManeuverType.RAMP_LEFT else ManeuverType.RAMP_RIGHT
            "fork" -> if (modifier.contains("left")) ManeuverType.FORK_LEFT else ManeuverType.FORK_RIGHT
            "turn" -> when (modifier) {
                "sharp left" -> ManeuverType.TURN_SHARP_LEFT
                "left" -> ManeuverType.TURN_LEFT
                "slight left" -> ManeuverType.TURN_SLIGHT_LEFT
                "sharp right" -> ManeuverType.TURN_SHARP_RIGHT
                "right" -> ManeuverType.TURN_RIGHT
                "slight right" -> ManeuverType.TURN_SLIGHT_RIGHT
                "uturn" -> ManeuverType.U_TURN
                else -> ManeuverType.CONTINUE_STRAIGHT
            }
            else -> ManeuverType.CONTINUE_STRAIGHT
        }
    }

    private fun formatInstruction(maneuver: ManeuverType, roadName: String): String {
        val road = if (roadName.isNotBlank()) " onto $roadName" else ""
        return when (maneuver) {
            ManeuverType.DEPART -> "Depart on ${roadName.ifBlank { "current road" }}"
            ManeuverType.ARRIVE -> "Arrive at destination"
            ManeuverType.TURN_LEFT -> "Turn left$road"
            ManeuverType.TURN_SLIGHT_LEFT -> "Keep slightly left$road"
            ManeuverType.TURN_SHARP_LEFT -> "Sharp left$road"
            ManeuverType.TURN_RIGHT -> "Turn right$road"
            ManeuverType.TURN_SLIGHT_RIGHT -> "Keep slightly right$road"
            ManeuverType.TURN_SHARP_RIGHT -> "Sharp right$road"
            ManeuverType.U_TURN -> "Make a U-turn"
            ManeuverType.ROUNDABOUT_ENTER -> "Enter roundabout"
            ManeuverType.ROUNDABOUT_EXIT -> "Take exit from roundabout"
            ManeuverType.MERGE -> "Merge$road"
            ManeuverType.RAMP_LEFT -> "Take ramp on the left$road"
            ManeuverType.RAMP_RIGHT -> "Take ramp on the right$road"
            ManeuverType.FORK_LEFT -> "Keep left at the fork$road"
            ManeuverType.FORK_RIGHT -> "Keep right at the fork$road"
            ManeuverType.CONTINUE_STRAIGHT -> "Continue straight$road"
        }
    }
}
