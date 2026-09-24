package org.geonav.app.domain.engine

import org.geonav.app.data.model.GeoPoint
import org.geonav.app.data.model.PlaceCategory
import org.geonav.app.data.model.StructuredGeoQuery

object SearchQueryParser {

    private val categorySynonyms = mapOf(
        PlaceCategory.FUEL to listOf("fuel", "petrol", "gas", "diesel", "service station", "gas station", "oil"),
        PlaceCategory.EV_CHARGER to listOf("ev", "charger", "charging", "tesla", "supercharger", "plug"),
        PlaceCategory.CAFE to listOf("cafe", "coffee", "espresso", "bakery", "breakfast"),
        PlaceCategory.RESTAURANT to listOf("food", "restaurant", "dining", "dinner", "lunch", "eatery", "pizza", "burger"),
        PlaceCategory.PARKING to listOf("parking", "car park", "garage", "park"),
        PlaceCategory.SUPERMARKET to listOf("supermarket", "grocery", "groceries", "market", "woolworths", "coles", "aldi"),
        PlaceCategory.HOSPITAL to listOf("hospital", "emergency", "er", "doctor", "clinic", "health"),
        PlaceCategory.PHARMACY to listOf("pharmacy", "chemist", "drugstore", "medicine"),
        PlaceCategory.TRANSIT_STATION to listOf("station", "train", "metro", "subway", "bus", "tram", "ferry")
    )

    fun parse(rawQuery: String, userLocation: GeoPoint?): StructuredGeoQuery {
        val clean = rawQuery.trim().lowercase()

        // Check if query is latitude, longitude coordinates
        val coordMatch = Regex("""^([-+]?\d{1,2}(?:\.\d+)?)[,\s]+([-+]?\d{1,3}(?:\.\d+)?)$""").find(clean)
        if (coordMatch != null) {
            val lat = coordMatch.groupValues[1].toDoubleOrNull()
            val lon = coordMatch.groupValues[2].toDoubleOrNull()
            if (lat != null && lon != null) {
                return StructuredGeoQuery(
                    rawQuery = rawQuery,
                    normalizedQuery = clean,
                    nearLocation = GeoPoint(lat, lon)
                )
            }
        }

        // Identify category from keywords
        var detectedCategory: PlaceCategory? = null
        for ((cat, synonyms) in categorySynonyms) {
            if (synonyms.any { clean.contains(it) }) {
                detectedCategory = cat
                break
            }
        }

        val onlyOpenNow = clean.contains("open now") || clean.contains("open")
        val requiresWheelchair = clean.contains("wheelchair") || clean.contains("accessible") || clean.contains("step free")
        val requiresParking = clean.contains("with parking") || clean.contains("car park")

        // Clean tokens
        var normalized = clean
            .replace("open now", "")
            .replace("wheelchair accessible", "")
            .replace("with parking", "")
            .trim()

        return StructuredGeoQuery(
            rawQuery = rawQuery,
            normalizedQuery = normalized,
            categoryFilter = detectedCategory,
            nearLocation = userLocation,
            onlyOpenNow = onlyOpenNow,
            requiresWheelchair = requiresWheelchair,
            requiresParking = requiresParking
        )
    }
}
