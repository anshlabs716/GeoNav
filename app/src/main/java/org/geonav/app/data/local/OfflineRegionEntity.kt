package org.geonav.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.geonav.app.data.model.AutoUpdatePolicy
import org.geonav.app.data.model.OfflineDownloadProfile
import org.geonav.app.data.model.OfflineRegion
import org.geonav.app.data.model.OfflineRegionStatus
import org.geonav.app.data.model.RegionType

@Entity(tableName = "offline_regions")
data class OfflineRegionEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val regionType: String = "CITY",
    val country: String,
    val minLat: Double,
    val minLon: Double,
    val maxLat: Double,
    val maxLon: Double,
    val sizeBytes: Long,
    val lastUpdatedDate: String,
    val status: String,
    val downloadProgressPercent: Int = 0,
    val updateAvailable: Boolean = false,
    val autoUpdatePolicy: String = "WIFI_ONLY",
    val profile: String = "DETAILED"
) {
    fun toDomain(): OfflineRegion {
        val st = try {
            OfflineRegionStatus.valueOf(status)
        } catch (e: Exception) {
            OfflineRegionStatus.NOT_DOWNLOADED
        }
        val pr = try {
            OfflineDownloadProfile.valueOf(profile)
        } catch (e: Exception) {
            OfflineDownloadProfile.DETAILED
        }
        val rt = try {
            RegionType.valueOf(regionType)
        } catch (e: Exception) {
            RegionType.CITY
        }
        val aup = try {
            AutoUpdatePolicy.valueOf(autoUpdatePolicy)
        } catch (e: Exception) {
            AutoUpdatePolicy.WIFI_ONLY
        }
        return OfflineRegion(
            id = id,
            name = name,
            regionType = rt,
            country = country,
            minLat = minLat,
            minLon = minLon,
            maxLat = maxLat,
            maxLon = maxLon,
            sizeBytes = sizeBytes,
            lastUpdatedDate = lastUpdatedDate,
            status = st,
            downloadProgressPercent = downloadProgressPercent,
            updateAvailable = updateAvailable,
            autoUpdatePolicy = aup,
            profile = pr
        )
    }

    companion object {
        fun fromDomain(region: OfflineRegion): OfflineRegionEntity {
            return OfflineRegionEntity(
                id = region.id,
                name = region.name,
                regionType = region.regionType.name,
                country = region.country,
                minLat = region.minLat,
                minLon = region.minLon,
                maxLat = region.maxLat,
                maxLon = region.maxLon,
                sizeBytes = region.sizeBytes,
                lastUpdatedDate = region.lastUpdatedDate,
                status = region.status.name,
                downloadProgressPercent = region.downloadProgressPercent,
                updateAvailable = region.updateAvailable,
                autoUpdatePolicy = region.autoUpdatePolicy.name,
                profile = region.profile.name
            )
        }
    }
}
