package org.geonav.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.geonav.app.data.model.GeoPoint
import org.geonav.app.data.model.Place
import org.geonav.app.data.model.PlaceCategory

@Entity(tableName = "saved_places")
data class SavedPlaceEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val customLabel: String? = null,
    val category: String,
    val latitude: Double,
    val longitude: Double,
    val address: String,
    val notes: String? = null,
    val isFavorite: Boolean = false,
    val listName: String = "Favorites", // "Home", "Work", "Favorites", or custom
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toPlace(): Place {
        val cat = try {
            PlaceCategory.valueOf(category)
        } catch (e: Exception) {
            PlaceCategory.GENERAL
        }
        return Place(
            id = id,
            name = customLabel ?: name,
            category = cat,
            address = address,
            location = GeoPoint(latitude, longitude)
        )
    }
}
