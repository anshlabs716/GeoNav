package org.geonav.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.geonav.app.data.model.FusionConfidence
import org.geonav.app.data.model.PositionFusionState

@Composable
fun PositioningStatusBadge(
    state: PositionFusionState,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = modifier.testTag("position_status_badge")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            val statusColor = when (state.confidence) {
                FusionConfidence.HIGH -> Color(0xFF2E7D32)
                FusionConfidence.MEDIUM -> Color(0xFFED6C02)
                FusionConfidence.LOW -> Color(0xFFF57C00)
                FusionConfidence.DEAD_RECKONING -> Color(0xFF0288D1)
                FusionConfidence.SEARCHING -> Color(0xFFFFB300)
                FusionConfidence.NO_FIX, FusionConfidence.LOST -> Color(0xFFD32F2F)
            }

            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(statusColor)
            )

            Spacer(modifier = Modifier.width(6.dp))

            val label = when (state.confidence) {
                FusionConfidence.HIGH -> {
                    val acc = state.accuracyMeters?.let { " ±${it.toInt()}m" } ?: ""
                    "GNSS Fix$acc"
                }
                FusionConfidence.MEDIUM -> "GNSS (Medium)"
                FusionConfidence.LOW -> "Network Fix"
                FusionConfidence.DEAD_RECKONING -> "Dead Reckoning"
                FusionConfidence.SEARCHING -> "Acquiring Fix..."
                FusionConfidence.NO_FIX -> "No GPS Fix"
                FusionConfidence.LOST -> "Signal Lost"
            }

            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (state.satellitesInUse != null && state.satellitesInUse > 0) {
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "• ${state.satellitesInUse} Sats",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (state.isTunnelMode) {
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "TUNNEL",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF0288D1)
                )
            }
        }
    }
}
