package org.geonav.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.geonav.app.R
import org.geonav.app.data.repository.SpeedUnit
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NavigationBottomBar(
    remainingSeconds: Long,
    remainingDistanceMeters: Double,
    currentSpeedKmh: Float,
    speedUnit: SpeedUnit = SpeedUnit.KMH,
    speedLimitKmh: Int? = null,
    onStopNavigation: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("navigation_bottom_bar")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                // ETA, Remaining Duration, Remaining Distance
                Column {
                    val etaTime = Date(System.currentTimeMillis() + (remainingSeconds * 1000))
                    val formatter = SimpleDateFormat("h:mm a", Locale.getDefault())
                    val formattedEta = formatter.format(etaTime)

                    val remainingMinutes = (remainingSeconds / 60).coerceAtLeast(1)
                    val timeText = if (remainingMinutes >= 60) {
                        val hours = remainingMinutes / 60
                        val mins = remainingMinutes % 60
                        "${hours}h ${mins}m"
                    } else {
                        "$remainingMinutes min"
                    }

                    val distanceText = if (remainingDistanceMeters >= 1000) {
                        String.format("%.1f km", remainingDistanceMeters / 1000)
                    } else {
                        String.format("%.0f m", remainingDistanceMeters)
                    }

                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = timeText,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "($distanceText)",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Text(
                        text = "ETA $formattedEta",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Speedometer & Stop Button
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val convertedSpeed = (currentSpeedKmh * speedUnit.toKmhMultiplier).toInt()
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = convertedSpeed.toString(),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = speedUnit.label,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Road Speed Limit Sign (only if real speed limit exists)
                    speedLimitKmh?.let { limit ->
                        Spacer(modifier = Modifier.width(12.dp))
                        val convertedLimit = (limit * speedUnit.toKmhMultiplier).toInt()
                        val isOverSpeed = convertedSpeed > convertedLimit
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .border(
                                    width = 3.dp,
                                    color = if (isOverSpeed) Color(0xFFD32F2F) else Color(0xFFC62828),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = convertedLimit.toString(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = Color.Black
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Close / Stop Navigation Button
                    IconButton(
                        onClick = onStopNavigation,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFD32F2F).copy(alpha = 0.15f))
                            .testTag("stop_navigation_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.stop_navigation),
                            tint = Color(0xFFD32F2F)
                        )
                    }
                }
            }
        }
    }
}
