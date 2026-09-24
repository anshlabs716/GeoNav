package org.geonav.app.data.model

enum class RegionType(val label: String) {
    COUNTRY("Country"),
    STATE_PROVINCE("State / Province"),
    CITY("City"),
    CUSTOM_AREA("Custom Map Area"),
    ROUTE_CORRIDOR("Route Corridor")
}

enum class OfflineDownloadProfile(val label: String, val description: String) {
    BASIC("Basic", "Roads, basic map, offline routing & search"),
    DETAILED("Detailed", "Roads, buildings, complete addresses & POIs"),
    COMPLETE("Complete", "Maximum POIs, terrain elevation, 3D structures")
}

enum class OfflineRegionStatus(val label: String) {
    NOT_DOWNLOADED("Not Downloaded"),
    DOWNLOADING("Downloading..."),
    DOWNLOADED("Downloaded"),
    UPDATE_AVAILABLE("Update Available"),
    SERVER_NOT_CONFIGURED("Server Not Configured"),
    FAILED("Download Failed")
}

enum class AutoUpdatePolicy(val label: String) {
    NEVER("Never"),
    WIFI_ONLY("Wi-Fi Only"),
    WIFI_AND_CHARGING("Wi-Fi + Charging"),
    ANY_CONNECTION("Any Connection")
}

data class OfflineRegion(
    val id: String,
    val name: String,
    val regionType: RegionType = RegionType.CITY,
    val country: String,
    val minLat: Double,
    val minLon: Double,
    val maxLat: Double,
    val maxLon: Double,
    val sizeBytes: Long,
    val lastUpdatedDate: String,
    val status: OfflineRegionStatus,
    val downloadProgressPercent: Int = 0,
    val updateAvailable: Boolean = false,
    val autoUpdatePolicy: AutoUpdatePolicy = AutoUpdatePolicy.WIFI_ONLY,
    val profile: OfflineDownloadProfile = OfflineDownloadProfile.DETAILED
) {
    val formattedSize: String
        get() {
            val mb = sizeBytes / (1024.0 * 1024.0)
            return if (mb >= 1000) {
                String.format("%.1f GB", mb / 1024.0)
            } else {
                String.format("%.0f MB", mb)
            }
        }
}
