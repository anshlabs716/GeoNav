package org.geonav.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import org.geonav.app.ui.screens.AboutScreen
import org.geonav.app.ui.screens.MainMapScreen
import org.geonav.app.ui.screens.OfflineMapsScreen
import org.geonav.app.ui.screens.SavedPlacesScreen
import org.geonav.app.ui.screens.SettingsScreen
import org.geonav.app.ui.theme.GeoNavTheme

enum class GeoNavScreen {
    MAP,
    SAVED_PLACES,
    OFFLINE_MAPS,
    SETTINGS,
    ABOUT
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            GeoNavTheme {
                val context = LocalContext.current
                val app = GeoNavApplication.instance
                val drawerState = androidx.compose.material3.rememberDrawerState(DrawerValue.Closed)
                val coroutineScope = rememberCoroutineScope()
                var currentScreen by remember { mutableStateOf(GeoNavScreen.MAP) }

                // Runtime location permission request
                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) { permissions ->
                    val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
                    val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
                    if (fineGranted || coarseGranted) {
                        app.positioningRepository.startPositioning()
                    }
                }

                LaunchedEffect(Unit) {
                    val hasFine = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                    if (hasFine) {
                        app.positioningRepository.startPositioning()
                    } else {
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                }

                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        ModalDrawerSheet(
                            drawerContainerColor = MaterialTheme.colorScheme.surface,
                            drawerShape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp),
                            modifier = Modifier.width(310.dp)
                        ) {
                            // GeoNav Brand Header with user icon
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF0D1B2A))
                                    .statusBarsPadding()
                                    .padding(20.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF13202E)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(
                                        painter = painterResource(R.drawable.img_app_icon),
                                        contentDescription = "GeoNav Icon",
                                        modifier = Modifier.size(50.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "GeoNav",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFF1F8F6)
                                )
                                Text(
                                    text = stringResource(R.string.app_tagline),
                                    fontSize = 13.sp,
                                    color = Color(0xFF82D2C1)
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Navigation Items
                            NavigationDrawerItem(
                                label = { Text("Map & Navigation", fontWeight = FontWeight.SemiBold) },
                                icon = { Icon(Icons.Default.Map, contentDescription = null) },
                                selected = currentScreen == GeoNavScreen.MAP,
                                onClick = {
                                    currentScreen = GeoNavScreen.MAP
                                    coroutineScope.launch { drawerState.close() }
                                },
                                colors = NavigationDrawerItemDefaults.colors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                            )

                            NavigationDrawerItem(
                                label = { Text("Saved Places", fontWeight = FontWeight.SemiBold) },
                                icon = { Icon(Icons.Default.Star, contentDescription = null) },
                                selected = currentScreen == GeoNavScreen.SAVED_PLACES,
                                onClick = {
                                    currentScreen = GeoNavScreen.SAVED_PLACES
                                    coroutineScope.launch { drawerState.close() }
                                },
                                colors = NavigationDrawerItemDefaults.colors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                            )

                            NavigationDrawerItem(
                                label = { Text("Offline Maps", fontWeight = FontWeight.SemiBold) },
                                icon = { Icon(Icons.Default.CloudDownload, contentDescription = null) },
                                selected = currentScreen == GeoNavScreen.OFFLINE_MAPS,
                                onClick = {
                                    currentScreen = GeoNavScreen.OFFLINE_MAPS
                                    coroutineScope.launch { drawerState.close() }
                                },
                                colors = NavigationDrawerItemDefaults.colors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                            )

                            NavigationDrawerItem(
                                label = { Text("Settings", fontWeight = FontWeight.SemiBold) },
                                icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                                selected = currentScreen == GeoNavScreen.SETTINGS,
                                onClick = {
                                    currentScreen = GeoNavScreen.SETTINGS
                                    coroutineScope.launch { drawerState.close() }
                                },
                                colors = NavigationDrawerItemDefaults.colors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                            )

                            NavigationDrawerItem(
                                label = { Text("About GeoNav", fontWeight = FontWeight.SemiBold) },
                                icon = { Icon(Icons.Default.Info, contentDescription = null) },
                                selected = currentScreen == GeoNavScreen.ABOUT,
                                onClick = {
                                    currentScreen = GeoNavScreen.ABOUT
                                    coroutineScope.launch { drawerState.close() }
                                },
                                colors = NavigationDrawerItemDefaults.colors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                            )
                        }
                    }
                ) {
                    when (currentScreen) {
                        GeoNavScreen.MAP -> {
                            MainMapScreen(
                                onOpenDrawer = {
                                    coroutineScope.launch { drawerState.open() }
                                }
                            )
                        }
                        GeoNavScreen.SAVED_PLACES -> {
                            SavedPlacesScreen(
                                onNavigateBack = { currentScreen = GeoNavScreen.MAP },
                                onSelectPlace = { place ->
                                    currentScreen = GeoNavScreen.MAP
                                }
                            )
                        }
                        GeoNavScreen.OFFLINE_MAPS -> {
                            OfflineMapsScreen(
                                onNavigateBack = { currentScreen = GeoNavScreen.MAP }
                            )
                        }
                        GeoNavScreen.SETTINGS -> {
                            SettingsScreen(
                                onNavigateBack = { currentScreen = GeoNavScreen.MAP },
                                onNavigateToAbout = { currentScreen = GeoNavScreen.ABOUT }
                            )
                        }
                        GeoNavScreen.ABOUT -> {
                            AboutScreen(
                                onNavigateBack = { currentScreen = GeoNavScreen.MAP }
                            )
                        }
                    }
                }
            }
        }
    }
}
