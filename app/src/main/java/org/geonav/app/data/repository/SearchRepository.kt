package org.geonav.app.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.geonav.app.data.local.SavedPlacesDao
import org.geonav.app.data.local.SearchHistoryDao
import org.geonav.app.data.local.SearchHistoryEntity
import org.geonav.app.data.model.GeoPoint
import org.geonav.app.data.model.Place
import org.geonav.app.data.model.PlaceCategory
import org.geonav.app.data.model.SearchResult
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale

class SearchRepository(
    private val searchHistoryDao: SearchHistoryDao,
    private val savedPlacesDao: SavedPlacesDao
) {

    val recentSearches: Flow<List<SearchHistoryEntity>> = searchHistoryDao.getRecentSearches()

    suspend fun search(query: String, userLocation: GeoPoint?): List<SearchResult> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return@withContext emptyList()

        val results = mutableListOf<SearchResult>()

        // 1. Coordinate search (Decimal & DMS)
        val coordPoint = parseCoordinates(trimmed)
        if (coordPoint != null) {
            val dist = userLocation?.distanceTo(coordPoint)
            val coordPlace = Place(
                id = "coord_${coordPoint.latitude}_${coordPoint.longitude}",
                name = String.format(Locale.US, "Location (%.5f, %.5f)", coordPoint.latitude, coordPoint.longitude),
                category = PlaceCategory.GENERAL,
                address = "Coordinates: ${coordPoint.latitude}, ${coordPoint.longitude}",
                location = coordPoint
            )
            return@withContext listOf(
                SearchResult(
                    place = coordPlace,
                    distanceMeters = dist,
                    matchType = "Coordinates"
                )
            )
        }

        // 2. Saved Places local matching (Instant offline response)
        try {
            val saved = savedPlacesDao.searchSavedPlaces(trimmed)
            for (entity in saved) {
                val place = PlacesRepository.entityToPlace(entity)
                val dist = userLocation?.distanceTo(place.location)
                results.add(
                    SearchResult(
                        place = place,
                        distanceMeters = dist,
                        matchType = "Saved Place"
                    )
                )
            }
        } catch (e: Exception) {
            // Local db fallback
        }

        // 3. Online OpenStreetMap Nominatim Geocoding
        val onlineResults = fetchNominatimResults(trimmed, userLocation)
        results.addAll(onlineResults)

        // 4. Sort: Prioritize saved places, then distance if available
        results.distinctBy { it.place.id }
            .sortedBy { it.distanceMeters ?: Double.MAX_VALUE }
            .take(12)
    }

    private fun parseCoordinates(query: String): GeoPoint? {
        val parts = query.split(",", " ", ";").filter { it.isNotBlank() }
        if (parts.size == 2) {
            val lat = parts[0].toDoubleOrNull()
            val lon = parts[1].toDoubleOrNull()
            if (lat != null && lon != null && lat in -90.0..90.0 && lon in -180.0..180.0) {
                return GeoPoint(lat, lon)
            }
        }
        return null
    }

    private fun fetchNominatimResults(query: String, userLocation: GeoPoint?): List<SearchResult> {
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val viewboxParam = if (userLocation != null) {
            val lat = userLocation.latitude
            val lon = userLocation.longitude
            // ~50km bounding box around user
            "&viewbox=${lon - 0.5},${lat + 0.5},${lon + 0.5},${lat - 0.5}&bounded=0"
        } else ""

        val urlString = "https://nominatim.openstreetmap.org/search?q=$encodedQuery&format=json&addressdetails=1&limit=10$viewboxParam"

        return try {
            val url = URL(urlString)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 4000
                readTimeout = 4000
                setRequestProperty("User-Agent", "GeoNav-Android-App/1.0 (contact@geonav.app)")
            }

            if (connection.responseCode == 200) {
                val jsonText = connection.inputStream.bufferedReader().use { it.readText() }
                parseNominatimJson(jsonText, userLocation)
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun parseNominatimJson(jsonText: String, userLocation: GeoPoint?): List<SearchResult> {
        val list = mutableListOf<SearchResult>()
        val array = JSONArray(jsonText)
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val lat = obj.optDouble("lat", 0.0)
            val lon = obj.optDouble("lon", 0.0)
            val displayName = obj.optString("display_name", "Unknown Location")
            val placeId = obj.optString("place_id", System.currentTimeMillis().toString() + i)
            val categoryType = obj.optString("type", "")
            val osmCategory = obj.optString("class", "")

            val name = displayName.split(",").firstOrNull()?.trim() ?: displayName
            val address = displayName.substringAfter(",").trim()

            val geoPoint = GeoPoint(lat, lon)
            val dist = userLocation?.distanceTo(geoPoint)

            val category = parseOsmTypeToCategory(osmCategory, categoryType)

            val place = Place(
                id = "osm_$placeId",
                name = name,
                category = category,
                address = address,
                location = geoPoint,
                sourceProvenance = "OpenStreetMap contributors"
            )

            list.add(
                SearchResult(
                    place = place,
                    distanceMeters = dist,
                    matchType = "Nominatim"
                )
            )
        }
        return list
    }

    private fun parseOsmTypeToCategory(osmClass: String, osmType: String): PlaceCategory {
        return when {
            osmClass == "amenity" && osmType in listOf("restaurant", "fast_food", "food_court") -> PlaceCategory.RESTAURANT
            osmClass == "amenity" && osmType in listOf("cafe", "coffee_shop") -> PlaceCategory.CAFE
            osmClass == "amenity" && osmType in listOf("fuel", "charging_station") -> PlaceCategory.FUEL
            osmClass == "amenity" && osmType in listOf("parking") -> PlaceCategory.PARKING
            osmClass == "shop" && osmType in listOf("supermarket", "grocery", "convenience") -> PlaceCategory.SUPERMARKET
            osmClass == "amenity" && osmType in listOf("hospital", "clinic") -> PlaceCategory.HOSPITAL
            osmClass == "amenity" && osmType in listOf("pharmacy") -> PlaceCategory.PHARMACY
            osmClass in listOf("railway", "aeroway", "highway") && osmType in listOf("station", "subway_entrance", "bus_stop") -> PlaceCategory.TRANSIT_STATION
            osmClass == "tourism" && osmType in listOf("attraction", "museum", "artwork") -> PlaceCategory.ATTRACTION
            osmClass == "leisure" && osmType in listOf("park", "garden") -> PlaceCategory.PARK
            osmClass == "tourism" && osmType in listOf("hotel", "motel", "hostel") -> PlaceCategory.HOTEL
            osmClass == "amenity" && osmType in listOf("atm", "bank") -> PlaceCategory.ATM
            else -> PlaceCategory.GENERAL
        }
    }

    suspend fun saveSearchQuery(query: String) = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.isNotBlank()) {
            searchHistoryDao.insertSearch(SearchHistoryEntity(query = trimmed))
        }
    }

    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        searchHistoryDao.clearHistory()
    }
}
