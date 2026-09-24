package org.geonav.app.data.repository

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.geonav.app.data.local.OfflineRegionEntity
import org.geonav.app.data.local.OfflineRegionsDao
import org.geonav.app.data.model.OfflineRegion
import org.geonav.app.data.model.OfflineRegionStatus
import java.io.File

sealed class DownloadResult {
    data object Success : DownloadResult()
    data class Error(val message: String) : DownloadResult()
}

class OfflineRepository(
    private val context: Context,
    private val dao: OfflineRegionsDao,
    private val settingsRepository: SettingsRepository
) {

    val regions: Flow<List<OfflineRegion>> = dao.getAllRegions().map { entities ->
        entities.map { it.toDomain() }
    }

    private val offlineTileDir: File
        get() = File(context.filesDir, "offline_maps").apply { if (!exists()) mkdirs() }

    fun getActualStorageUsageBytes(): Long {
        return calculateDirSize(offlineTileDir)
    }

    private fun calculateDirSize(dir: File): Long {
        var size = 0L
        if (dir.exists() && dir.isDirectory) {
            dir.listFiles()?.forEach { file ->
                size += if (file.isDirectory) calculateDirSize(file) else file.length()
            }
        }
        return size
    }

    suspend fun updateRegion(region: OfflineRegion): DownloadResult = withContext(Dispatchers.IO) {
        val serverUrl = settingsRepository.offlineServerUrl.value.trim()
        if (serverUrl.isBlank()) {
            val unconfigured = region.copy(
                status = OfflineRegionStatus.SERVER_NOT_CONFIGURED
            )
            dao.updateRegion(OfflineRegionEntity.fromDomain(unconfigured))
            return@withContext DownloadResult.Error(
                "Offline map package server endpoint is not configured. Please configure a custom tile server or PMTiles/MBTiles URL in Settings."
            )
        }

        // Real download attempt if server URL is provided
        try {
            dao.updateRegion(
                OfflineRegionEntity.fromDomain(
                    region.copy(status = OfflineRegionStatus.DOWNLOADING, downloadProgressPercent = 5)
                )
            )

            val regionFile = File(offlineTileDir, "${region.id}.mbtiles")
            val url = java.net.URL("$serverUrl/${region.id}.mbtiles")
            val conn = (url.openConnection() as java.net.HttpURLConnection).apply {
                connectTimeout = 6000
                readTimeout = 6000
                setRequestProperty("User-Agent", "GeoNav-Android-App/1.0")
            }

            if (conn.responseCode == 200) {
                val totalLength = conn.contentLengthLong
                var bytesRead = 0L
                conn.inputStream.use { input ->
                    regionFile.outputStream().use { output ->
                        val buffer = ByteArray(8192)
                        var read: Int
                        while (input.read(buffer).also { read = it } != -1) {
                            output.write(buffer, 0, read)
                            bytesRead += read
                            if (totalLength > 0) {
                                val progress = ((bytesRead * 100) / totalLength).toInt()
                                dao.updateRegion(
                                    OfflineRegionEntity.fromDomain(
                                        region.copy(
                                            status = OfflineRegionStatus.DOWNLOADING,
                                            downloadProgressPercent = progress
                                        )
                                    )
                                )
                            }
                        }
                    }
                }

                val downloaded = region.copy(
                    status = OfflineRegionStatus.DOWNLOADED,
                    sizeBytes = regionFile.length(),
                    lastUpdatedDate = "Updated just now",
                    downloadProgressPercent = 100,
                    updateAvailable = false
                )
                dao.updateRegion(OfflineRegionEntity.fromDomain(downloaded))
                return@withContext DownloadResult.Success
            } else {
                val failed = region.copy(status = OfflineRegionStatus.FAILED)
                dao.updateRegion(OfflineRegionEntity.fromDomain(failed))
                return@withContext DownloadResult.Error("Server returned HTTP error ${conn.responseCode}")
            }
        } catch (e: Exception) {
            val failed = region.copy(status = OfflineRegionStatus.FAILED)
            dao.updateRegion(OfflineRegionEntity.fromDomain(failed))
            return@withContext DownloadResult.Error("Network error: ${e.localizedMessage ?: "Failed to connect to offline tile server"}")
        }
    }

    suspend fun downloadRegion(region: OfflineRegion): DownloadResult {
        return updateRegion(region)
    }

    suspend fun deleteRegion(id: String) = withContext(Dispatchers.IO) {
        val regionFile = File(offlineTileDir, "$id.mbtiles")
        if (regionFile.exists()) {
            regionFile.delete()
        }
        dao.deleteRegion(id)
    }

    suspend fun addRegion(region: OfflineRegion) {
        dao.insertRegion(OfflineRegionEntity.fromDomain(region))
    }
}
