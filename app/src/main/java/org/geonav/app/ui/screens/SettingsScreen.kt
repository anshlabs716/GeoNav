package org.geonav.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CompassCalibration
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.geonav.app.GeoNavApplication
import org.geonav.app.R
import org.geonav.app.data.model.MapStyleType
import org.geonav.app.data.repository.AppThemeMode
import org.geonav.app.data.repository.SpeedUnit
import org.geonav.app.data.storage.ExchangeResult
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAbout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = GeoNavApplication.instance
    val coroutineScope = rememberCoroutineScope()

    val currentTheme by app.settingsRepository.themeMode.collectAsState()
    val currentStyle by app.settingsRepository.currentStyle.collectAsState()
    val satelliteUrl by app.settingsRepository.satelliteProviderUrl.collectAsState()
    val speedUnit by app.settingsRepository.speedUnit.collectAsState()
    val autoZoom by app.settingsRepository.autoZoom.collectAsState()
    val speedBasedCamera by app.settingsRepository.speedBasedCamera.collectAsState()
    val trueNorth by app.settingsRepository.trueNorth.collectAsState()
    val voiceGuidance by app.settingsRepository.voiceGuidance.collectAsState()
    val fusionState by app.positioningRepository.fusionState.collectAsState()

    var showSatelliteDialog by remember { mutableStateOf(false) }
    var satelliteInputText by remember { mutableStateOf(satelliteUrl) }
    var showExportDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        modifier = modifier.testTag("settings_screen")
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Section 1: Appearance & Theme
            item {
                SettingsSectionHeader("APPEARANCE & THEME")
            }

            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Theme Mode", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            AppThemeMode.entries.forEach { mode ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable { app.settingsRepository.setThemeMode(mode) }
                                ) {
                                    RadioButton(
                                        selected = currentTheme == mode,
                                        onClick = { app.settingsRepository.setThemeMode(mode) }
                                    )
                                    Text(mode.title, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }

            // Section 2: Speedometer & Navigation
            item {
                SettingsSectionHeader("SPEEDOMETER & UNITS")
            }

            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Speed, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Speed Unit", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                            SpeedUnit.entries.forEach { unit ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable { app.settingsRepository.setSpeedUnit(unit) }
                                ) {
                                    RadioButton(
                                        selected = speedUnit == unit,
                                        onClick = { app.settingsRepository.setSpeedUnit(unit) }
                                    )
                                    Text(unit.label, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                        SettingsSwitchRow(
                            icon = Icons.Default.VolumeUp,
                            title = "Voice Turn Guidance",
                            subtitle = "Speak upcoming maneuvers and navigation alerts",
                            checked = voiceGuidance,
                            onCheckedChange = { app.settingsRepository.setVoiceGuidance(it) }
                        )
                    }
                }
            }

            // Section 3: Map Style & Satellite Architecture
            item {
                SettingsSectionHeader("MAP LAYERS & SATELLITE PROVIDER")
            }

            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    satelliteInputText = satelliteUrl
                                    showSatelliteDialog = true
                                }
                        ) {
                            Icon(Icons.Default.Layers, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Satellite Imagery Provider", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                Text(
                                    text = if (satelliteUrl.isBlank()) "Provider-independent (Not configured)" else satelliteUrl,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "Configure",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Section 4: Camera & Sensors
            item {
                SettingsSectionHeader("CAMERA & POSITION FUSION")
            }

            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        SettingsSwitchRow(
                            icon = Icons.Default.CameraAlt,
                            title = "Speed-Based Auto Zoom",
                            subtitle = "Adjust camera distance smoothly as velocity increases",
                            checked = autoZoom,
                            onCheckedChange = { app.settingsRepository.setAutoZoom(it) }
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                        SettingsSwitchRow(
                            icon = Icons.Default.CompassCalibration,
                            title = "True North Orientation",
                            subtitle = "Compensate for geomagnetic declination automatically",
                            checked = trueNorth,
                            onCheckedChange = { app.settingsRepository.setTrueNorth(it) }
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Sensors, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Detected Hardware Sensors", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                val sensorsStr = if (fusionState.availableSensors.isNotEmpty()) {
                                    fusionState.availableSensors.joinToString(", ")
                                } else {
                                    "Scanning device sensors..."
                                }
                                Text(
                                    text = sensorsStr,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Section 5: Data Exchange (Import / Export)
            item {
                SettingsSectionHeader("DATA MANAGEMENT (IMPORT / EXPORT)")
            }

            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showExportDialog = true }
                                .padding(vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.FileDownload, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Export Saved Places & Waypoints", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                Text("Supports GPX, KML, GeoJSON, CSV formats", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    coroutineScope.launch {
                                        app.searchRepository.clearHistory()
                                        Toast.makeText(context, "Search history cleared", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                .padding(vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Clear Search History", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                Text("Wipes local query logs from device storage", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            // Section 6: About
            item {
                SettingsSectionHeader("ABOUT & SYSTEM")
            }

            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToAbout() }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("About GeoNav", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text("FOSS • Zero telemetry • MapLibre & OpenStreetMap", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // Dialog: Satellite URL Configuration
        if (showSatelliteDialog) {
            AlertDialog(
                onDismissRequest = { showSatelliteDialog = false },
                title = { Text("Configure Satellite Provider") },
                text = {
                    Column {
                        Text(
                            text = "GeoNav does not scrape unauthorized tiles. Enter your legitimate raster or vector tile endpoint (e.g., WMTS/XYZ URL template with {z}/{x}/{y}):",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = satelliteInputText,
                            onValueChange = { satelliteInputText = it },
                            label = { Text("Tile Provider URL") },
                            placeholder = { Text("https://example.com/tiles/{z}/{x}/{y}.png") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            app.settingsRepository.setSatelliteProviderUrl(satelliteInputText)
                            showSatelliteDialog = false
                            Toast.makeText(context, "Provider URL saved", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showSatelliteDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Dialog: Export Format Selector
        if (showExportDialog) {
            AlertDialog(
                onDismissRequest = { showExportDialog = false },
                title = { Text("Export Format") },
                text = {
                    Column {
                        Text("Select format for saved waypoints and places:")
                        Spacer(modifier = Modifier.height(8.dp))
                        listOf("GPX", "KML", "GeoJSON", "CSV").forEach { format ->
                            TextButton(
                                onClick = {
                                    showExportDialog = false
                                    coroutineScope.launch {
                                        val places = app.placesRepository.savedPlaces.first()
                                        val ext = format.lowercase()
                                        val file = File(context.filesDir, "geonav_export_${System.currentTimeMillis()}.$ext")
                                        FileOutputStream(file).use { out ->
                                            when (format) {
                                                "GPX" -> app.dataExchangeManager.exportGpx(places, out)
                                                "KML" -> app.dataExchangeManager.exportKml(places, out)
                                                "GeoJSON" -> app.dataExchangeManager.exportGeoJson(places, out)
                                                "CSV" -> app.dataExchangeManager.exportCsv(places, out)
                                            }
                                        }
                                        Toast.makeText(context, "Exported ${places.size} places to ${file.name}", Toast.LENGTH_LONG).show()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(format, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showExportDialog = false }) {
                        Text("Close")
                    }
                }
            )
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp, top = 20.dp, bottom = 8.dp)
    )
}

@Composable
private fun SettingsSwitchRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
