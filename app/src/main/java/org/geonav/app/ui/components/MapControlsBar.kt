package org.geonav.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.geonav.app.R

@Composable
fun MapControlsBar(
    compassHeading: Float,
    is3D: Boolean,
    isFollowingLocation: Boolean,
    onCompassClick: () -> Unit,
    on3DToggle: () -> Unit,
    onLayersClick: () -> Unit,
    onRecenterClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val animatedCompassRotation by animateFloatAsState(
        targetValue = -compassHeading,
        animationSpec = tween(durationMillis = 250),
        label = "compass_rotation"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        // True North Compass
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 3.dp,
            shadowElevation = 4.dp,
            modifier = Modifier
                .size(48.dp)
                .clickable { onCompassClick() }
                .testTag("compass_button")
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "N",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFD32F2F),
                    modifier = Modifier.align(Alignment.TopCenter)
                )
                Icon(
                    imageVector = Icons.Default.Navigation,
                    contentDescription = stringResource(R.string.compass_content_description),
                    tint = Color(0xFFD32F2F),
                    modifier = Modifier
                        .size(22.dp)
                        .rotate(animatedCompassRotation)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 3D Perspective Toggle
        SmallFloatingActionButton(
            onClick = on3DToggle,
            containerColor = if (is3D) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
            contentColor = if (is3D) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
            elevation = FloatingActionButtonDefaults.elevation(3.dp),
            shape = CircleShape,
            modifier = Modifier
                .size(48.dp)
                .testTag("toggle_3d_button")
        ) {
            Icon(
                imageVector = Icons.Default.ViewInAr,
                contentDescription = "Toggle 3D View",
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Direct Layer Switcher Button
        SmallFloatingActionButton(
            onClick = onLayersClick,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            elevation = FloatingActionButtonDefaults.elevation(3.dp),
            shape = CircleShape,
            modifier = Modifier
                .size(48.dp)
                .testTag("layers_button")
        ) {
            Icon(
                imageVector = Icons.Default.Layers,
                contentDescription = stringResource(R.string.map_layers),
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Recenter FAB
        FloatingActionButton(
            onClick = onRecenterClick,
            containerColor = if (isFollowingLocation) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
            contentColor = if (isFollowingLocation) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
            elevation = FloatingActionButtonDefaults.elevation(5.dp),
            shape = CircleShape,
            modifier = Modifier
                .size(54.dp)
                .testTag("recenter_location_button")
        ) {
            Icon(
                imageVector = Icons.Default.MyLocation,
                contentDescription = stringResource(R.string.recenter_map),
                modifier = Modifier.size(26.dp)
            )
        }
    }
}
