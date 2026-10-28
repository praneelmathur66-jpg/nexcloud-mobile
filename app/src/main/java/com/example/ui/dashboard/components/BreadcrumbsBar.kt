package com.example.ui.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.NexBlue
import com.example.ui.theme.NexBorder
import com.example.ui.theme.NexTextMuted
import com.example.ui.theme.NexTextPrimary

@Composable
fun BreadcrumbsBar(
    currentPath: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val parts = if (currentPath.isBlank()) emptyList() else currentPath.split("/").filter { it.isNotBlank() }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .testTag("breadcrumbs_row"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Root / Home crumb
        val isHomeActive = parts.isEmpty()
        CrumbItem(
            label = "☁️ Home",
            isActive = isHomeActive,
            onClick = { onNavigate("") }
        )

        // Nested crumbs
        var accumulatedPath = ""
        parts.forEachIndexed { index, part ->
            Text(
                text = "›",
                color = NexTextMuted,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            accumulatedPath = if (accumulatedPath.isEmpty()) part else "$accumulatedPath/$part"
            val targetPath = accumulatedPath
            val isLast = index == parts.size - 1

            CrumbItem(
                label = part,
                isActive = isLast,
                onClick = { onNavigate(targetPath) }
            )
        }
    }
}

@Composable
private fun CrumbItem(
    label: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isActive) NexBlue else NexBorder)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Medium,
            color = NexTextPrimary
        )
    }
}
