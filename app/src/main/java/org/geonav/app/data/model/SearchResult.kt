package org.geonav.app.data.model

data class SearchResult(
    val place: Place,
    val matchType: String = "Exact",
    val distanceMeters: Double? = null,
    val highlightSpan: String? = null
)

data class StructuredGeoQuery(
    val rawQuery: String,
    val normalizedQuery: String,
    val categoryFilter: PlaceCategory? = null,
    val nearLocation: GeoPoint? = null,
    val alongRoute: Boolean = false,
    val onlyOpenNow: Boolean = false,
    val requiresWheelchair: Boolean = false,
    val requiresParking: Boolean = false
)
