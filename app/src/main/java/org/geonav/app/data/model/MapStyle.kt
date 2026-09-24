package org.geonav.app.data.model

enum class MapStyleType(
    val title: String,
    val description: String,
    val isSatelliteRelated: Boolean = false
) {
    STANDARD(
        title = "Standard",
        description = "Clean vector map powered by OpenStreetMap",
        isSatelliteRelated = false
    ),
    LIGHT(
        title = "Light",
        description = "Minimalist high-contrast light style",
        isSatelliteRelated = false
    ),
    DARK(
        title = "Dark",
        description = "Optimized dark mode for night navigation and OLED",
        isSatelliteRelated = false
    ),
    TERRAIN(
        title = "Terrain",
        description = "Topographical contours and elevation shading",
        isSatelliteRelated = false
    ),
    CYCLING(
        title = "Cycling",
        description = "Dedicated cycle paths, lanes, and bikeways",
        isSatelliteRelated = false
    ),
    HIKING(
        title = "Hiking",
        description = "Marked trail routes and footpath networks",
        isSatelliteRelated = false
    ),
    TRANSIT(
        title = "Transit",
        description = "Subway lines, rail corridors, and bus stops",
        isSatelliteRelated = false
    ),
    SATELLITE(
        title = "Satellite",
        description = "Aerial imagery (Requires configured provider)",
        isSatelliteRelated = true
    ),
    HYBRID(
        title = "Hybrid",
        description = "Satellite imagery with vector road and label overlay",
        isSatelliteRelated = true
    );

    fun getStyleUri(customSatelliteProviderUrl: String?): String {
        return when (this) {
            STANDARD -> "https://tiles.openfreemap.org/styles/liberty"
            LIGHT -> "https://tiles.openfreemap.org/styles/positron"
            DARK -> "https://tiles.openfreemap.org/styles/dark"
            TERRAIN -> "https://demotiles.maplibre.org/style.json"
            CYCLING -> "https://tiles.openfreemap.org/styles/liberty"
            HIKING -> "https://demotiles.maplibre.org/style.json"
            TRANSIT -> "https://tiles.openfreemap.org/styles/liberty"
            SATELLITE, HYBRID -> {
                if (!customSatelliteProviderUrl.isNullOrBlank()) {
                    customSatelliteProviderUrl
                } else {
                    // Legitimate fallback: standard vector with note that imagery is not configured
                    "https://tiles.openfreemap.org/styles/liberty"
                }
            }
        }
    }
}
