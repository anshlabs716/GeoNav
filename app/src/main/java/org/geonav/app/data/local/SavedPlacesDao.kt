package org.geonav.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedPlacesDao {
    @Query("SELECT * FROM saved_places ORDER BY createdAt DESC")
    fun getAllSavedPlaces(): Flow<List<SavedPlaceEntity>>

    @Query("SELECT * FROM saved_places WHERE listName = :listName ORDER BY createdAt DESC")
    fun getPlacesByList(listName: String): Flow<List<SavedPlaceEntity>>

    @Query("SELECT * FROM saved_places WHERE isFavorite = 1")
    fun getFavorites(): Flow<List<SavedPlaceEntity>>

    @Query("SELECT * FROM saved_places WHERE name LIKE '%' || :query || '%' OR address LIKE '%' || :query || '%'")
    suspend fun searchSavedPlaces(query: String): List<SavedPlaceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlace(place: SavedPlaceEntity)

    @Update
    suspend fun updatePlace(place: SavedPlaceEntity)

    @Query("DELETE FROM saved_places WHERE id = :id")
    suspend fun deletePlaceById(id: String)

    @Query("DELETE FROM saved_places")
    suspend fun clearAll()
}
