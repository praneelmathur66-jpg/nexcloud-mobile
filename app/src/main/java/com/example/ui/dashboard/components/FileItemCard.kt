package com.example.ui.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.repository.NexFile
import com.example.ui.theme.NexBlue
import com.example.ui.theme.NexBorder
import com.example.ui.theme.NexCardBg
import com.example.ui.theme.NexCyan
import com.example.ui.theme.NexDanger
import com.example.ui.theme.NexTextPrimary
import com.example.ui.theme.NexTextSecondary

@Composable
fun FileItemCard(
    file: NexFile,
    onPreview: (NexFile) -> Unit,
    onDownload: (NexFile) -> Unit,
    onShare: (NexFile) -> Unit,
    onDelete: (NexFile) -> Unit,
    modifier: Modifier = Modifier
) {
    val isImage = isImageExtension(file.name)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("file_card_${file.name}")
            .clip(RoundedCornerShape(12.dp))
            .clickable { onPreview(file) },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = NexCardBg),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.Brush.linearGradient(listOf(NexBorder, NexBorder)))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Icon or Thumbnail + Name & Size
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (isImage && !file.previewUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(file.previewUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = file.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(NexBorder)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(getIconBgColor(file.name)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = getFileEmoji(file.name),
                            fontSize = 20.sp
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = file.name,
                        color = NexTextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = file.formattedSize,
                        color = NexTextSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            // Right: Actions (Preview, Download, Share, Delete)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                IconButton(
                    onClick = { onPreview(file) },
                    modifier = Modifier.size(36.dp).testTag("preview_file_${file.name}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = "Preview",
                        tint = NexCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = { onDownload(file) },
                    modifier = Modifier.size(36.dp).testTag("download_file_${file.name}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Download",
                        tint = NexBlue,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = { onShare(file) },
                    modifier = Modifier.size(36.dp).testTag("share_file_${file.name}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = NexTextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = { onDelete(file) },
                    modifier = Modifier.size(36.dp).testTag("delete_file_${file.name}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = NexDanger,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

private fun isImageExtension(name: String): Boolean {
    val ext = name.substringAfterLast('.', "").lowercase()
    return ext in listOf("jpg", "jpeg", "png", "webp", "gif", "svg", "bmp")
}

private fun getFileEmoji(name: String): String {
    val ext = name.substringAfterLast('.', "").lowercase()
    return when (ext) {
        "jpg", "jpeg", "png", "webp", "gif", "svg", "bmp" -> "🖼️"
        "pdf" -> "📄"
        "mp3", "wav", "flac", "m4a", "ogg" -> "🎵"
        "mp4", "mkv", "mov", "avi", "webm" -> "🎬"
        "zip", "rar", "7z", "tar", "gz" -> "📦"
        "kt", "java", "py", "js", "ts", "html", "css", "json", "xml" -> "📝"
        "doc", "docx", "txt", "md" -> "📄"
        "xls", "xlsx", "csv" -> "📊"
        "ppt", "pptx" -> "📑"
        else -> "📄"
    }
}

private fun getIconBgColor(name: String): Color {
    val ext = name.substringAfterLast('.', "").lowercase()
    return when (ext) {
        "jpg", "jpeg", "png", "webp", "gif" -> Color(0xFF1E3A5F)
        "pdf" -> Color(0xFF3B1D28)
        "mp3", "wav", "m4a" -> Color(0xFF2E1F47)
        "mp4", "mov" -> Color(0xFF3F2A1E)
        "zip", "rar", "7z" -> Color(0xFF2C3E2D)
        "kt", "py", "js", "html", "json" -> Color(0xFF1E2E42)
        else -> NexBorder
    }
}
