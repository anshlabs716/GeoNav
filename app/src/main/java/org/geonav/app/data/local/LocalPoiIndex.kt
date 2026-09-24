package org.geonav.app.data.local

import org.geonav.app.data.model.GeoPoint
import org.geonav.app.data.model.Place
import org.geonav.app.data.model.PlaceCategory
import org.geonav.app.data.model.PlaceEntrance
import org.geonav.app.data.model.SearchResult
import org.geonav.app.data.model.StructuredGeoQuery
import kotlin.math.min

object LocalPoiIndex {

    private val localPlaces = listOf(
        Place(
            id = "poi_1",
            name = "Central Train Station",
            category = PlaceCategory.TRANSIT_STATION,
            address = "Eddy Ave & Elizabeth St",
            location = GeoPoint(-33.8832, 151.2062),
            openingHours = "24/7",
            isOpenNow = true,
            wheelchairAccessible = true,
            parkingAvailable = true,
            entrances = listOf(
                PlaceEntrance("main", GeoPoint(-33.8830, 151.2060), "Main concourse entrance"),
                PlaceEntrance("wheelchair", GeoPoint(-33.8833, 151.2065), "Accessible step-free elevator entrance"),
                PlaceEntrance("transit", GeoPoint(-33.8835, 151.2068), "Light rail interchange")
            )
        ),
        Place(
            id = "poi_2",
            name = "Woolworths Metro Supermarket",
            category = PlaceCategory.SUPERMARKET,
            address = "126 Campbell St",
            location = GeoPoint(-33.8805, 151.2110),
            brand = "Woolworths",
            openingHours = "06:00 - 23:00",
            isOpenNow = true,
            wheelchairAccessible = true,
            parkingAvailable = true,
            entrances = listOf(
                PlaceEntrance("main", GeoPoint(-33.8805, 151.2110), "Street entrance"),
                PlaceEntrance("parking", GeoPoint(-33.8808, 151.2112), "Underground car park entry")
            )
        ),
        Place(
            id = "poi_3",
            name = "Ampol Fuel & EV Charging Hub",
            category = PlaceCategory.FUEL,
            address = "450 Crown St",
            location = GeoPoint(-33.8860, 151.2140),
            brand = "Ampol",
            openingHours = "24/7",
            isOpenNow = true,
            wheelchairAccessible = true,
            parkingAvailable = true,
            entrances = listOf(
                PlaceEntrance("main", GeoPoint(-33.8860, 151.2140), "Driveway entrance")
            )
        ),
        Place(
            id = "poi_4",
            name = "St Vincent's Hospital Emergency",
            category = PlaceCategory.HOSPITAL,
            address = "390 Victoria St",
            location = GeoPoint(-33.8798, 151.2225),
            openingHours = "24/7 Emergency",
            isOpenNow = true,
            wheelchairAccessible = true,
            parkingAvailable = true,
            entrances = listOf(
                PlaceEntrance("emergency", GeoPoint(-33.8795, 151.2228), "Ambulance & Emergency walk-in"),
                PlaceEntrance("main", GeoPoint(-33.8798, 151.2225), "Hospital main foyer"),
                PlaceEntrance("parking", GeoPoint(-33.8802, 151.2220), "Visitor parking basement")
            )
        ),
        Place(
            id = "poi_5",
            name = "Single O Cafe & Roastery",
            category = PlaceCategory.CAFE,
            address = "60 Reservoir St, Surry Hills",
            location = GeoPoint(-33.8845, 151.2095),
            openingHours = "07:00 - 15:30",
            isOpenNow = true,
            wheelchairAccessible = true,
            parkingAvailable = false
        ),
        Place(
            id = "poi_6",
            name = "Tesla Supercharger & Tritium EV",
            category = PlaceCategory.EV_CHARGER,
            address = "Goulburn St Parking, Level 2",
            location = GeoPoint(-33.8780, 151.2085),
            openingHours = "24/7",
            isOpenNow = true,
            wheelchairAccessible = true,
            parkingAvailable = true,
            entrances = listOf(
                PlaceEntrance("parking", GeoPoint(-33.8780, 151.2085), "Car park boom gate entrance")
            )
        ),
        Place(
            id = "poi_7",
            name = "Hyde Park Nature & Walking Trail",
            category = PlaceCategory.PARK,
            address = "Elizabeth St & Park St",
            location = GeoPoint(-33.8732, 151.2113),
            openingHours = "Open 24 hours",
            isOpenNow = true,
            wheelchairAccessible = true,
            parkingAvailable = false
        ),
        Place(
            id = "poi_8",
            name = "Chemist Warehouse Pharmacy",
            category = PlaceCategory.PHARMACY,
            address = "210 Pitt St",
            location = GeoPoint(-33.8720, 151.2088),
            openingHours = "08:00 - 21:00",
            isOpenNow = true,
            wheelchairAccessible = true,
            parkingAvailable = false
        )
    )

    fun search(query: StructuredGeoQuery): List<SearchResult> {
        val tokens = query.normalizedQuery.split(" ").filter { it.isNotBlank() }

        return localPlaces.mapNotNull { place ->
            val score = calculateMatchScore(place, tokens, query)
            if (score > 0) {
                val distance = query.nearLocation?.distanceTo(place.location)
                SearchResult(
                    place = place,
                    matchType = if (score >= 90) "Exact" else "Fuzzy Match",
                    distanceMeters = distance
                )
            } else null
        }.sortedWith(
            compareByDescending<SearchResult> { it.matchType == "Exact" }
                .thenBy { it.distanceMeters ?: Double.MAX_VALUE }
        )
    }

    private fun calculateMatchScore(
        place: Place,
        tokens: List<String>,
        query: StructuredGeoQuery
    ): Int {
        // Check category filter
        if (query.categoryFilter != null && place.category != query.categoryFilter) {
            return 0
        }
        // Accessibility filter
        if (query.requiresWheelchair && !place.wheelchairAccessible) {
            return 0
        }
        // Parking filter
        if (query.requiresParking && !place.parkingAvailable) {
            return 0
        }
        if (tokens.isEmpty()) {
            return 80 // Return category or nearby matches
        }

        val nameLower = place.name.lowercase()
        val addressLower = place.address.lowercase()
        val categoryLower = place.category.name.lowercase().replace("_", " ")

        var matches = 0
        for (token in tokens) {
            if (nameLower.contains(token) || addressLower.contains(token) || categoryLower.contains(token)) {
                matches += 2
            } else if (isFuzzyMatch(token, nameLower) || isFuzzyMatch(token, categoryLower)) {
                matches += 1
            }
        }

        return if (matches > 0) min(100, matches * 25) else 0
    }

    private fun isFuzzyMatch(target: String, text: String): Boolean {
        if (target.length < 3) return false
        val words = text.split(" ", ",", "-", "&")
        for (word in words) {
            if (levenshteinDistance(target, word) <= 1) return true
        }
        return false
    }

    private fun levenshteinDistance(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }
        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j
        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = min(dp[i - 1][j] + 1, min(dp[i][j - 1] + 1, dp[i - 1][j - 1] + cost))
            }
        }
        return dp[s1.length][s2.length]
    }
}
