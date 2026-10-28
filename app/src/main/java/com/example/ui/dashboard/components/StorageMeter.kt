package com.example.ui.dashboard.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.formatBytes
import com.example.ui.theme.NexBlue
import com.example.ui.theme.NexBorder
import com.example.ui.theme.NexCardBg
import com.example.ui.theme.NexCyan
import com.example.ui.theme.NexDanger
import com.example.ui.theme.NexTextPrimary
import com.example.ui.theme.NexTextSecondary

@Composable
fun StorageMeter(
    usedBytes: Long,
    quotaBytes: Long,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    val fraction = if (quotaBytes > 0) {
        (usedBytes.toFloat() / quotaBytes.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val animatedFraction by animateFloatAsState(
        targetValue = fraction,
        animationSpec = tween(durationMillis = 800),
        label = "StorageProgress"
    )

    val percentage = (fraction * 100).toInt()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("storage_meter_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = NexCardBg),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(NexBorder, NexBorder)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "☁️",
                        fontSize = 18.sp
                    )
                    Text(
                        text = "Storage",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = NexTextPrimary
                    )
                }

                Text(
                    text = if (isLoading) "Calculating..." else "${formatBytes(usedBytes)} / ${formatBytes(quotaBytes)} ($percentage%)",
                    fontSize = 13.sp,
                    color = NexTextSecondary,
                    fontWeight = FontWeight.Medium
                )
            }

            // Progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(NexBorder)
            ) {
                val progressBrush = if (fraction > 0.9f) {
                    Brush.horizontalGradient(listOf(NexBlue, NexDanger))
                } else {
                    Brush.horizontalGradient(listOf(NexBlue, NexCyan))
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction = animatedFraction.coerceAtLeast(0.01f))
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(progressBrush)
                )
            }
        }
    }
}
