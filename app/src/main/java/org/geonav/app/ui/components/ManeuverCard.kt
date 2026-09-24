package org.geonav.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.TurnLeft
import androidx.compose.material.icons.filled.TurnRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.geonav.app.data.model.ManeuverType
import org.geonav.app.data.model.RouteStep

@Composable
fun ManeuverCard(
    currentStep: RouteStep,
    distanceMeters: Double,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("maneuver_guidance_card")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Maneuver Icon
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getManeuverIcon(currentStep.maneuverType),
                    contentDescription = currentStep.instruction,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                // Distance Countdown
                val formattedDistance = if (distanceMeters >= 1000) {
                    String.format("%.1f km", distanceMeters / 1000)
                } else {
                    String.format("%.0f m", distanceMeters)
                }

                Text(
                    text = "In $formattedDistance",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )

                Text(
                    text = currentStep.roadName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
                )

                currentStep.laneGuidance?.let { lane ->
                    Text(
                        text = lane,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}

private fun getManeuverIcon(type: ManeuverType): androidx.compose.ui.graphics.vector.ImageVector {
    return when (type) {
        ManeuverType.TURN_LEFT, ManeuverType.TURN_SHARP_LEFT, ManeuverType.TURN_SLIGHT_LEFT -> Icons.Default.TurnLeft
        ManeuverType.TURN_RIGHT, ManeuverType.TURN_SHARP_RIGHT, ManeuverType.TURN_SLIGHT_RIGHT -> Icons.Default.TurnRight
        ManeuverType.U_TURN -> Icons.AutoMirrored.Filled.Undo
        ManeuverType.CONTINUE_STRAIGHT, ManeuverType.DEPART -> Icons.Default.ArrowUpward
        ManeuverType.ROUNDABOUT_ENTER, ManeuverType.ROUNDABOUT_EXIT -> Icons.Default.Navigation
        ManeuverType.RAMP_LEFT, ManeuverType.FORK_LEFT -> Icons.AutoMirrored.Filled.ArrowBack
        ManeuverType.RAMP_RIGHT, ManeuverType.FORK_RIGHT -> Icons.AutoMirrored.Filled.ArrowForward
        ManeuverType.ARRIVE -> Icons.Default.Navigation
        else -> Icons.Default.ArrowUpward
    }
}
