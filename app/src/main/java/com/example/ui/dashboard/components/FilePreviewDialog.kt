package com.example.ui.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.data.repository.NexFile
import com.example.ui.theme.NexBlue
import com.example.ui.theme.NexBorder
import com.example.ui.theme.NexCardBg
import com.example.ui.theme.NexCardElevated
import com.example.ui.theme.NexCyan
import com.example.ui.theme.NexTextMuted
import com.example.ui.theme.NexTextPrimary
import com.example.ui.theme.NexTextSecondary

@Composable
fun FilePreviewDialog(
    file: NexFile,
    signedUrl: String?,
    textContent: String?,
    isLoadingContent: Boolean,
    onDownload: () -> Unit,
    onShare: () -> Unit,
    onDismiss: () -> Unit
) {
    val isImage = file.name.substringAfterLast('.', "").lowercase() in
            listOf("jpg", "jpeg", "png", "webp", "gif", "svg", "bmp")

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = NexCardBg,
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.Brush.linearGradient(listOf(NexBorder, NexBorder))),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("file_preview_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📄 File Details",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = NexTextPrimary
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp).testTag("close_preview_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = NexTextSecondary
                        )
                    }
                }

                // Image Preview if image
                if (isImage && !signedUrl.isNullOrEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(NexCardElevated),
                        contentAlignment = Alignment.Center
                    ) {
                        SubcomposeAsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(signedUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = file.name,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxWidth(),
                            loading = {
                                Box(
                                    modifier = Modifier.padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        color = NexCyan,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                        )
                    }
                }

                // Text Content Preview if text file
                if (!textContent.isNullOrEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(NexCardElevated)
                            .padding(12.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = textContent,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = NexTextPrimary
                        )
                    }
                } else if (isLoadingContent) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = NexCyan, modifier = Modifier.size(28.dp))
                    }
                }

                // Metadata Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = NexCardElevated)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MetaRow(label = "Name", value = file.name)
                        MetaRow(label = "Size", value = file.formattedSize)
                        if (!file.mimeType.isNullOrBlank()) {
                            MetaRow(label = "MIME Type", value = file.mimeType)
                        }
                        MetaRow(label = "Path", value = file.fullPath)
                        if (!file.updatedAt.isNullOrBlank()) {
                            MetaRow(label = "Updated", value = file.updatedAt)
                        }
                    }
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onDownload,
                        modifier = Modifier.weight(1f).testTag("preview_download_button"),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NexBlue,
                            contentColor = NexTextPrimary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.size(6.dp))
                        Text("Download", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }

                    OutlinedButton(
                        onClick = onShare,
                        modifier = Modifier.weight(1f).testTag("preview_share_button"),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NexCyan)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.size(6.dp))
                        Text("Share", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun MetaRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = NexTextMuted,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            fontSize = 12.sp,
            color = NexTextPrimary,
            fontWeight = FontWeight.Normal,
            maxLines = 1
        )
    }
}
