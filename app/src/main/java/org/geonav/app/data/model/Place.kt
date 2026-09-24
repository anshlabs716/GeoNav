package org.geonav.app.data.model

import kotlinx.serialization.Serializable

@Serializable
enum class PlaceCategory {
    HOME,
    WORK,
    RESTAURANT,
    CAFE,
    FUEL,
    EV_CHARGER,
    PARKING,
    SUPERMARKET,
    HOSPITAL,
    PHARMACY,
    TRANSIT_STATION,
    ATTRACTION,
    PARK,
    HOTEL,
    ATM,
    GENERAL;

    companion object {
        val GENERIC: PlaceCategory get() = GENERAL
    }
}

@Serializable
data class PlaceEntrance(
    val type: String, // "main", "pedestrian", "wheelchair", "parking", "transit"
    val location: GeoPoint,
    val description: String? = null
)

@Serializable
data class Place(
    val id: String,
    val name: String,
    val category: PlaceCategory,
    val address: String,
    val location: GeoPoint,
    val brand: String? = null,
    val openingHours: String? = null,
    val isOpenNow: Boolean? = true,
    val phone: String? = null,
    val website: String? = null,
    val wheelchairAccessible: Boolean = true,
    val parkingAvailable: Boolean = false,
    val entrances: List<PlaceEntrance> = emptyList(),
    val sourceProvenance: String = "OpenStreetMap contributors"
)
