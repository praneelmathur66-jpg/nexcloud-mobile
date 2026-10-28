package com.example.ui.dashboard

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.dashboard.components.BreadcrumbsBar
import com.example.ui.dashboard.components.CreateFolderDialog
import com.example.ui.dashboard.components.FileItemCard
import com.example.ui.dashboard.components.FilePreviewDialog
import com.example.ui.dashboard.components.FolderItemCard
import com.example.ui.dashboard.components.StorageMeter
import com.example.ui.theme.NexBlue
import com.example.ui.theme.NexBorder
import com.example.ui.theme.NexCardBg
import com.example.ui.theme.NexCardElevated
import com.example.ui.theme.NexCyan
import com.example.ui.theme.NexDanger
import com.example.ui.theme.NexDangerBg
import com.example.ui.theme.NexDarkBg
import com.example.ui.theme.NexSuccess
import com.example.ui.theme.NexSuccessBg
import com.example.ui.theme.NexTextMuted
import com.example.ui.theme.NexTextPrimary
import com.example.ui.theme.NexTextSecondary

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    var showLogoutConfirm by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }

    // File pickers
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.uploadFilesFromUris(context, uris)
        }
    }

    val singleFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.uploadFilesFromUris(context, uris)
        }
    }

    Scaffold(
        containerColor = NexDarkBg,
        topBar = {
            DashboardTopBar(
                userEmail = uiState.userEmail ?: "",
                onRefresh = {
                    viewModel.loadFiles()
                    viewModel.calculateStorageUsage()
                },
                onSettings = onOpenSettings,
                onLogout = { showLogoutConfirm = true }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("dashboard_lazy_column"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Storage Quota Meter
            item {
                StorageMeter(
                    usedBytes = uiState.storageUsedBytes,
                    quotaBytes = uiState.quotaBytes,
                    isLoading = uiState.isCalculatingStorage
                )
            }

            // Breadcrumbs Navigation Bar
            item {
                BreadcrumbsBar(
                    currentPath = uiState.currentPath,
                    onNavigate = { path -> viewModel.navigateTo(path) }
                )
            }

            // Upload Box (Matching web interface)
            item {
                UploadBoxCard(
                    isUploading = uiState.isUploading,
                    uploadStatusText = uiState.uploadStatusText,
                    onChooseFile = {
                        try {
                            filePickerLauncher.launch(arrayOf("*/*"))
                        } catch (_: Exception) {
                            singleFilePickerLauncher.launch("*/*")
                        }
                    },
                    onUploadBatch = {
                        singleFilePickerLauncher.launch("*/*")
                    },
                    onCreateFolder = { viewModel.setShowCreateFolderDialog(true) }
                )
            }

            // Inline Feedback Message Banner
            uiState.feedbackMessage?.let { feedback ->
                item {
                    FeedbackBanner(
                        message = feedback.text,
                        isSuccess = feedback.isSuccess,
                        onDismiss = { viewModel.clearFeedbackMessage() }
                    )
                }
            }

            // Search & Sort Bar
            item {
                SearchAndSortBar(
                    searchQuery = uiState.searchQuery,
                    onSearchQueryChange = { viewModel.setSearchQuery(it) },
                    selectedSort = uiState.sortOption,
                    onSortSelected = { viewModel.setSortOption(it) },
                    isMenuExpanded = showSortMenu,
                    onMenuExpandedChange = { showSortMenu = it }
                )
            }

            // Section Title: "📁 Your Files"
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📁 Your Files",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = NexTextPrimary
                    )

                    val count = uiState.filteredFolders.size + uiState.filteredFiles.size
                    Text(
                        text = "$count item${if (count != 1) "s" else ""}",
                        fontSize = 13.sp,
                        color = NexTextSecondary
                    )
                }
            }

            // Loading indicator
            if (uiState.isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(color = NexCyan)
                            Text(
                                text = "Loading files...",
                                color = NexTextSecondary,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            } else if (uiState.filteredFolders.isEmpty() && uiState.filteredFiles.isEmpty()) {
                // Empty state matching web: "This folder is empty."
                item {
                    EmptyFolderCard(searchQuery = uiState.searchQuery)
                }
            } else {
                // Folders list
                items(
                    items = uiState.filteredFolders,
                    key = { "folder_${it.name}" }
                ) { folder ->
                    FolderItemCard(
                        folder = folder,
                        onOpen = { viewModel.openFolder(it) },
                        onDelete = { viewModel.setFolderToDelete(it) }
                    )
                }

                // Files list
                items(
                    items = uiState.filteredFiles,
                    key = { "file_${it.id}" }
                ) { file ->
                    FileItemCard(
                        file = file,
                        onPreview = { viewModel.openFilePreview(it) },
                        onDownload = { viewModel.downloadFile(context, it) },
                        onShare = { viewModel.shareFile(context, it) },
                        onDelete = { viewModel.setFileToDelete(it) }
                    )
                }
            }

            // Bottom spacer for comfortable scrolling
            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // ==========================================
    // MODALS & DIALOGS
    // ==========================================

    // Create Folder Dialog
    if (uiState.showCreateFolderDialog) {
        CreateFolderDialog(
            onDismiss = { viewModel.setShowCreateFolderDialog(false) },
            onConfirm = { name -> viewModel.createFolder(name) }
        )
    }

    // Delete Folder Confirmation Dialog
    uiState.folderToDelete?.let { folder ->
        AlertDialog(
            onDismissRequest = { viewModel.setFolderToDelete(null) },
            containerColor = NexCardBg,
            title = {
                Text(
                    text = "Delete Folder",
                    color = NexTextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Delete \"${folder.name}\" and EVERYTHING inside it?",
                    color = NexTextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.deleteFolderConfirmed() },
                    colors = ButtonDefaults.buttonColors(containerColor = NexDanger)
                ) {
                    Text("Delete", fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.setFolderToDelete(null) }) {
                    Text("Cancel", color = NexTextMuted)
                }
            }
        )
    }

    // Delete File Confirmation Dialog
    uiState.fileToDelete?.let { file ->
        AlertDialog(
            onDismissRequest = { viewModel.setFileToDelete(null) },
            containerColor = NexCardBg,
            title = {
                Text(
                    text = "Delete File",
                    color = NexTextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete \"${file.name}\"?",
                    color = NexTextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.deleteFileConfirmed() },
                    colors = ButtonDefaults.buttonColors(containerColor = NexDanger)
                ) {
                    Text("Delete", fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.setFileToDelete(null) }) {
                    Text("Cancel", color = NexTextMuted)
                }
            }
        )
    }

    // Logout Confirmation Dialog
    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            containerColor = NexCardBg,
            title = {
                Text(
                    text = "Sign Out",
                    color = NexTextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to sign out of NexCloud?",
                    color = NexTextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutConfirm = false
                        viewModel.logout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NexDanger)
                ) {
                    Text("Logout", fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirm = false }) {
                    Text("Cancel", color = NexTextMuted)
                }
            }
        )
    }

    // File Preview Dialog
    uiState.selectedPreviewFile?.let { file ->
        FilePreviewDialog(
            file = file,
            signedUrl = uiState.previewSignedUrl,
            textContent = uiState.previewTextContent,
            isLoadingContent = uiState.isLoadingPreviewContent,
            onDownload = {
                viewModel.downloadFile(context, file)
                viewModel.dismissFilePreview()
            },
            onShare = {
                viewModel.shareFile(context, file)
            },
            onDismiss = { viewModel.dismissFilePreview() }
        )
    }
}

// ==========================================
// SUB-COMPONENTS
// ==========================================

@Composable
private fun DashboardTopBar(
    userEmail: String,
    onRefresh: () -> Unit,
    onSettings: () -> Unit,
    onLogout: () -> Unit
) {
    Surface(
        color = NexCardBg,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Brand & User
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "☁️",
                        fontSize = 20.sp
                    )
                    Text(
                        text = "NexCloud",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = NexTextPrimary
                    )
                }
                if (userEmail.isNotEmpty()) {
                    Text(
                        text = userEmail,
                        fontSize = 12.sp,
                        color = NexCyan,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Top action buttons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier.size(40.dp).testTag("topbar_refresh_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = NexTextSecondary
                    )
                }

                IconButton(
                    onClick = onSettings,
                    modifier = Modifier.size(40.dp).testTag("topbar_settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = NexTextSecondary
                    )
                }

                IconButton(
                    onClick = onLogout,
                    modifier = Modifier.size(40.dp).testTag("topbar_logout_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = "Logout",
                        tint = NexDanger
                    )
                }
            }
        }
    }
}

@Composable
private fun UploadBoxCard(
    isUploading: Boolean,
    uploadStatusText: String?,
    onChooseFile: () -> Unit,
    onUploadBatch: () -> Unit,
    onCreateFolder: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("upload_box_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = NexCardBg),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(NexBorder, NexBorder)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Upload to NexCloud",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = NexTextPrimary
                )
                Text(
                    text = "Upload files or create subfolders",
                    fontSize = 13.sp,
                    color = NexTextSecondary
                )
            }

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onChooseFile,
                    enabled = !isUploading,
                    modifier = Modifier.weight(1f).testTag("upload_file_button"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NexBlue,
                        contentColor = NexTextPrimary
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudUpload,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("File", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }

                FilledTonalButton(
                    onClick = onUploadBatch,
                    enabled = !isUploading,
                    modifier = Modifier.weight(1f).testTag("upload_batch_button"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = NexBorder,
                        contentColor = NexTextPrimary
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Batch", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }

                OutlinedButton(
                    onClick = onCreateFolder,
                    enabled = !isUploading,
                    modifier = Modifier.weight(1.1f).testTag("new_folder_button"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NexCyan),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Folder", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            if (isUploading) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(NexCardElevated)
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CircularProgressIndicator(
                        color = NexCyan,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = uploadStatusText ?: "Uploading...",
                        fontSize = 13.sp,
                        color = NexTextPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchAndSortBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedSort: SortOption,
    onSortSelected: (SortOption) -> Unit,
    isMenuExpanded: Boolean,
    onMenuExpandedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text("Search files & folders...", fontSize = 13.sp) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = NexTextMuted,
                    modifier = Modifier.size(18.dp)
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear",
                            tint = NexTextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            },
            singleLine = true,
            modifier = Modifier
                .weight(1f)
                .testTag("search_text_field"),
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = NexBlue,
                unfocusedBorderColor = NexBorder,
                focusedTextColor = NexTextPrimary,
                unfocusedTextColor = NexTextPrimary
            )
        )

        Box {
            FilledTonalButton(
                onClick = { onMenuExpandedChange(true) },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = NexBorder,
                    contentColor = NexTextPrimary
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                modifier = Modifier.testTag("sort_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Sort,
                    contentDescription = "Sort",
                    modifier = Modifier.size(18.dp)
                )
            }

            DropdownMenu(
                expanded = isMenuExpanded,
                onDismissRequest = { onMenuExpandedChange(false) },
                modifier = Modifier.background(NexCardBg)
            ) {
                SortOption.values().forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = option.displayName,
                                color = if (option == selectedSort) NexBlue else NexTextPrimary,
                                fontWeight = if (option == selectedSort) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        },
                        onClick = {
                            onSortSelected(option)
                            onMenuExpandedChange(false)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun FeedbackBanner(
    message: String,
    isSuccess: Boolean,
    onDismiss: () -> Unit
) {
    val bgColor = if (isSuccess) NexSuccessBg else NexDangerBg
    val textColor = if (isSuccess) NexSuccess else NexDanger

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .clickable(onClick = onDismiss)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = message,
            color = textColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.Default.Clear,
            contentDescription = "Dismiss",
            tint = textColor,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun EmptyFolderCard(searchQuery: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = NexCardBg),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(NexBorder, NexBorder)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(text = "☁️", fontSize = 42.sp)
            Text(
                text = if (searchQuery.isNotEmpty()) "No matching files found" else "This folder is empty.",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = NexTextPrimary
            )
            Text(
                text = if (searchQuery.isNotEmpty()) "Try a different search query" else "Upload a file or create a folder to get started.",
                fontSize = 13.sp,
                color = NexTextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}
