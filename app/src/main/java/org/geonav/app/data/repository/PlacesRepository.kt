package org.geonav.app.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.geonav.app.data.local.SavedPlaceEntity
import org.geonav.app.data.local.SavedPlacesDao
import org.geonav.app.data.model.GeoPoint
import org.geonav.app.data.model.Place
import org.geonav.app.data.model.PlaceCategory

class PlacesRepository(private val savedPlacesDao: SavedPlacesDao) {

    val allSavedPlaces: Flow<List<SavedPlaceEntity>> = savedPlacesDao.getAllSavedPlaces()
    val favoritePlaces: Flow<List<SavedPlaceEntity>> = savedPlacesDao.getFavorites()

    val savedPlaces: Flow<List<Place>> = allSavedPlaces.map { list ->
        list.map { entityToPlace(it) }
    }

    fun getPlacesByList(listName: String): Flow<List<SavedPlaceEntity>> =
        savedPlacesDao.getPlacesByList(listName)

    suspend fun savePlace(place: Place, customLabel: String? = null, listName: String = "Favorites") {
        val entity = SavedPlaceEntity(
            id = place.id,
            name = place.name,
            customLabel = customLabel,
            category = place.category.name,
            latitude = place.location.latitude,
            longitude = place.location.longitude,
            address = place.address,
            isFavorite = listName == "Favorites",
            listName = listName
        )
        savedPlacesDao.insertPlace(entity)
    }

    suspend fun removePlace(id: String) {
        savedPlacesDao.deletePlaceById(id)
    }

    suspend fun clearAll() {
        savedPlacesDao.clearAll()
    }

    companion object {
        fun entityToPlace(entity: SavedPlaceEntity): Place {
            val cat = try {
                PlaceCategory.valueOf(entity.category)
            } catch (e: Exception) {
                PlaceCategory.GENERAL
            }
            return Place(
                id = entity.id,
                name = entity.name,
                category = cat,
                address = entity.address,
                location = GeoPoint(entity.latitude, entity.longitude)
            )
        }
    }
}
