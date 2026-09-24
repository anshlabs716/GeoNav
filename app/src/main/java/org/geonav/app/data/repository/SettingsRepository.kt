package org.geonav.app.data.repository

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.geonav.app.data.model.AutoUpdatePolicy
import org.geonav.app.data.model.MapStyleType

enum class AppThemeMode(val title: String) {
    SYSTEM("System Default"),
    LIGHT("Light"),
    DARK("Dark")
}

enum class SpeedUnit(val label: String, val toKmhMultiplier: Float) {
    KMH("km/h", 1.0f),
    MPH("mph", 0.621371f),
    MS("m/s", 0.277778f),
    KNOTS("knots", 0.539957f);

    fun formatSpeed(speedKmh: Float): String {
        val converted = speedKmh * toKmhMultiplier
        return "${converted.toInt()} $label"
    }
}

class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("geonav_settings", Context.MODE_PRIVATE)

    // Theme mode
    private val _themeMode = MutableStateFlow(
        AppThemeMode.valueOf(prefs.getString("theme_mode", AppThemeMode.SYSTEM.name) ?: AppThemeMode.SYSTEM.name)
    )
    val themeMode: StateFlow<AppThemeMode> = _themeMode.asStateFlow()

    // Map Style
    private val _currentStyle = MutableStateFlow(
        MapStyleType.valueOf(prefs.getString("map_style", MapStyleType.STANDARD.name) ?: MapStyleType.STANDARD.name)
    )
    val currentStyle: StateFlow<MapStyleType> = _currentStyle.asStateFlow()

    // Satellite imagery custom provider URL
    private val _satelliteProviderUrl = MutableStateFlow(
        prefs.getString("satellite_provider_url", "") ?: ""
    )
    val satelliteProviderUrl: StateFlow<String> = _satelliteProviderUrl.asStateFlow()

    // Speed unit
    private val _speedUnit = MutableStateFlow(
        SpeedUnit.valueOf(prefs.getString("speed_unit", SpeedUnit.KMH.name) ?: SpeedUnit.KMH.name)
    )
    val speedUnit: StateFlow<SpeedUnit> = _speedUnit.asStateFlow()

    // Camera settings
    private val _autoZoom = MutableStateFlow(prefs.getBoolean("camera_auto_zoom", true))
    val autoZoom: StateFlow<Boolean> = _autoZoom.asStateFlow()

    private val _speedBasedCamera = MutableStateFlow(prefs.getBoolean("camera_speed_based", true))
    val speedBasedCamera: StateFlow<Boolean> = _speedBasedCamera.asStateFlow()

    private val _turnAwareCamera = MutableStateFlow(prefs.getBoolean("camera_turn_aware", true))
    val turnAwareCamera: StateFlow<Boolean> = _turnAwareCamera.asStateFlow()

    // Heading & Sensors
    private val _trueNorth = MutableStateFlow(prefs.getBoolean("true_north", true))
    val trueNorth: StateFlow<Boolean> = _trueNorth.asStateFlow()

    // Guidance
    private val _voiceGuidance = MutableStateFlow(prefs.getBoolean("voice_guidance", true))
    val voiceGuidance: StateFlow<Boolean> = _voiceGuidance.asStateFlow()

    private val _speedLimitAlerts = MutableStateFlow(prefs.getBoolean("speed_alerts", true))
    val speedLimitAlerts: StateFlow<Boolean> = _speedLimitAlerts.asStateFlow()

    // Offline maps
    private val _autoUpdatePolicy = MutableStateFlow(
        AutoUpdatePolicy.valueOf(prefs.getString("auto_update_policy", AutoUpdatePolicy.WIFI_ONLY.name) ?: AutoUpdatePolicy.WIFI_ONLY.name)
    )
    val autoUpdatePolicy: StateFlow<AutoUpdatePolicy> = _autoUpdatePolicy.asStateFlow()

    private val _offlineServerUrl = MutableStateFlow(
        prefs.getString("offline_server_url", "") ?: ""
    )
    val offlineServerUrl: StateFlow<String> = _offlineServerUrl.asStateFlow()

    fun setThemeMode(mode: AppThemeMode) {
        prefs.edit().putString("theme_mode", mode.name).apply()
        _themeMode.value = mode
    }

    fun setMapStyle(style: MapStyleType) {
        prefs.edit().putString("map_style", style.name).apply()
        _currentStyle.value = style
    }

    fun setSatelliteProviderUrl(url: String) {
        prefs.edit().putString("satellite_provider_url", url).apply()
        _satelliteProviderUrl.value = url
    }

    fun setSpeedUnit(unit: SpeedUnit) {
        prefs.edit().putString("speed_unit", unit.name).apply()
        _speedUnit.value = unit
    }

    fun setAutoZoom(enabled: Boolean) {
        prefs.edit().putBoolean("camera_auto_zoom", enabled).apply()
        _autoZoom.value = enabled
    }

    fun setSpeedBasedCamera(enabled: Boolean) {
        prefs.edit().putBoolean("camera_speed_based", enabled).apply()
        _speedBasedCamera.value = enabled
    }

    fun setTurnAwareCamera(enabled: Boolean) {
        prefs.edit().putBoolean("camera_turn_aware", enabled).apply()
        _turnAwareCamera.value = enabled
    }

    fun setTrueNorth(enabled: Boolean) {
        prefs.edit().putBoolean("true_north", enabled).apply()
        _trueNorth.value = enabled
    }

    fun setVoiceGuidance(enabled: Boolean) {
        prefs.edit().putBoolean("voice_guidance", enabled).apply()
        _voiceGuidance.value = enabled
    }

    fun setSpeedLimitAlerts(enabled: Boolean) {
        prefs.edit().putBoolean("speed_alerts", enabled).apply()
        _speedLimitAlerts.value = enabled
    }

    fun setAutoUpdatePolicy(policy: AutoUpdatePolicy) {
        prefs.edit().putString("auto_update_policy", policy.name).apply()
        _autoUpdatePolicy.value = policy
    }

    fun setOfflineServerUrl(url: String) {
        prefs.edit().putString("offline_server_url", url).apply()
        _offlineServerUrl.value = url
    }
}
