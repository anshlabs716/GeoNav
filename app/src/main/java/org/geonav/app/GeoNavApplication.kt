package org.geonav.app

import android.app.Application
import org.geonav.app.data.local.GeoNavDatabase
import org.geonav.app.data.repository.OfflineRepository
import org.geonav.app.data.repository.PlacesRepository
import org.geonav.app.data.repository.PositioningRepository
import org.geonav.app.data.repository.RoutingRepository
import org.geonav.app.data.repository.SearchRepository
import org.geonav.app.data.repository.SettingsRepository
import org.geonav.app.data.storage.GeoNavDataExchangeManager
import org.maplibre.android.MapLibre

class GeoNavApplication : Application() {

    lateinit var database: GeoNavDatabase
        private set

    lateinit var placesRepository: PlacesRepository
        private set

    lateinit var searchRepository: SearchRepository
        private set

    lateinit var routingRepository: RoutingRepository
        private set

    lateinit var positioningRepository: PositioningRepository
        private set

    lateinit var offlineRepository: OfflineRepository
        private set

    lateinit var settingsRepository: SettingsRepository
        private set

    lateinit var dataExchangeManager: GeoNavDataExchangeManager
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Initialize MapLibre Native SDK
        MapLibre.getInstance(this)

        // Initialize local Room database
        database = GeoNavDatabase.getDatabase(this)

        // Initialize repositories
        settingsRepository = SettingsRepository(this)
        placesRepository = PlacesRepository(database.savedPlacesDao())
        searchRepository = SearchRepository(database.searchHistoryDao(), database.savedPlacesDao())
        routingRepository = RoutingRepository()
        positioningRepository = PositioningRepository(this)
        offlineRepository = OfflineRepository(this, database.offlineRegionsDao(), settingsRepository)
        dataExchangeManager = GeoNavDataExchangeManager()
    }

    companion object {
        lateinit var instance: GeoNavApplication
            private set
    }
}
