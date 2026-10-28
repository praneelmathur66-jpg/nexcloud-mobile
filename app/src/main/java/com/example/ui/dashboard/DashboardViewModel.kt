package com.example.ui.dashboard

import android.app.Application
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.remote.SupabaseClient
import com.example.data.repository.NexCloudRepository
import com.example.data.repository.NexFile
import com.example.data.repository.NexFolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

enum class SortOption(val displayName: String) {
    NAME_ASC("Name (A-Z)"),
    NAME_DESC("Name (Z-A)"),
    SIZE_ASC("Size (Smallest)"),
    SIZE_DESC("Size (Largest)"),
    DATE_DESC("Newest First")
}

data class FeedbackMessage(
    val text: String,
    val isSuccess: Boolean
)

data class DashboardUiState(
    val isLoggedIn: Boolean = false,
    val userEmail: String? = null,
    val currentPath: String = "",
    val folders: List<NexFolder> = emptyList(),
    val files: List<NexFile> = emptyList(),
    val isLoading: Boolean = false,
    val storageUsedBytes: Long = 0L,
    val quotaBytes: Long = SupabaseClient.QUOTA_BYTES,
    val isCalculatingStorage: Boolean = false,
    val searchQuery: String = "",
    val sortOption: SortOption = SortOption.NAME_ASC,
    val feedbackMessage: FeedbackMessage? = null,
    val isUploading: Boolean = false,
    val uploadStatusText: String? = null,
    val showCreateFolderDialog: Boolean = false,
    val folderToDelete: NexFolder? = null,
    val fileToDelete: NexFile? = null,
    val selectedPreviewFile: NexFile? = null,
    val previewSignedUrl: String? = null,
    val previewTextContent: String? = null,
    val isLoadingPreviewContent: Boolean = false
) {
    val filteredFolders: List<NexFolder>
        get() {
            var list = if (searchQuery.isBlank()) folders else folders.filter {
                it.name.contains(searchQuery, ignoreCase = true)
            }
            return when (sortOption) {
                SortOption.NAME_ASC -> list.sortedBy { it.name.lowercase() }
                SortOption.NAME_DESC -> list.sortedByDescending { it.name.lowercase() }
                else -> list
            }
        }

    val filteredFiles: List<NexFile>
        get() {
            var list = if (searchQuery.isBlank()) files else files.filter {
                it.name.contains(searchQuery, ignoreCase = true)
            }
            return when (sortOption) {
                SortOption.NAME_ASC -> list.sortedBy { it.name.lowercase() }
                SortOption.NAME_DESC -> list.sortedByDescending { it.name.lowercase() }
                SortOption.SIZE_ASC -> list.sortedBy { it.sizeBytes }
                SortOption.SIZE_DESC -> list.sortedByDescending { it.sizeBytes }
                SortOption.DATE_DESC -> list.sortedByDescending { it.updatedAt ?: "" }
            }
        }
}

class DashboardViewModel @JvmOverloads constructor(
    application: Application,
    private val repository: NexCloudRepository = NexCloudRepository.create(application)
) : AndroidViewModel(application) {

    companion object {
        fun provideFactory(
            application: Application,
            repository: NexCloudRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return DashboardViewModel(application, repository) as T
            }
        }
    }

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        checkSession()
    }

    fun checkSession() {
        val loggedIn = repository.session.isLoggedIn()
        val email = repository.session.getUserEmail()
        _uiState.update {
            it.copy(
                isLoggedIn = loggedIn,
                userEmail = email
            )
        }
        if (loggedIn) {
            loadFiles("")
            calculateStorageUsage()
        }
    }

    fun loadFiles(path: String = _uiState.value.currentPath) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, currentPath = path) }
            val result = repository.listFilesAndFolders(path)
            result.onSuccess { (folders, files) ->
                _uiState.update {
                    it.copy(
                        folders = folders,
                        files = files,
                        isLoading = false
                    )
                }
                // Preload signed preview URLs for image files in the background
                preloadImageUrls(files)
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        feedbackMessage = FeedbackMessage("Failed to load files: ${err.message}", false)
                    )
                }
            }
        }
    }

    private fun preloadImageUrls(files: List<NexFile>) {
        viewModelScope.launch {
            val imageFiles = files.filter {
                val ext = it.name.substringAfterLast('.', "").lowercase()
                ext in listOf("jpg", "jpeg", "png", "webp", "gif", "svg")
            }
            if (imageFiles.isEmpty()) return@launch

            val updatedFiles = _uiState.value.files.toMutableList()
            var modified = false

            for (file in imageFiles) {
                if (file.previewUrl == null) {
                    val urlResult = repository.getSignedUrl(file.fullPath, expiresInSeconds = 1800)
                    urlResult.onSuccess { url ->
                        val index = updatedFiles.indexOfFirst { it.fullPath == file.fullPath }
                        if (index != -1) {
                            updatedFiles[index] = updatedFiles[index].copy(previewUrl = url)
                            modified = true
                        }
                    }
                }
            }

            if (modified) {
                _uiState.update { it.copy(files = updatedFiles) }
            }
        }
    }

    fun navigateTo(path: String) {
        _uiState.update { it.copy(currentPath = path, searchQuery = "") }
        loadFiles(path)
    }

    fun openFolder(folder: NexFolder) {
        navigateTo(folder.fullPath)
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun setSortOption(option: SortOption) {
        _uiState.update { it.copy(sortOption = option) }
    }

    fun setShowCreateFolderDialog(show: Boolean) {
        _uiState.update { it.copy(showCreateFolderDialog = show) }
    }

    fun setFolderToDelete(folder: NexFolder?) {
        _uiState.update { it.copy(folderToDelete = folder) }
    }

    fun setFileToDelete(file: NexFile?) {
        _uiState.update { it.copy(fileToDelete = file) }
    }

    fun createFolder(name: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(showCreateFolderDialog = false) }
            val currentPath = _uiState.value.currentPath
            val result = repository.createFolder(currentPath, name)
            result.onSuccess {
                _uiState.update {
                    it.copy(
                        feedbackMessage = FeedbackMessage("Folder created! 📁", true)
                    )
                }
                loadFiles(currentPath)
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        feedbackMessage = FeedbackMessage("Folder creation failed: ${err.message}", false)
                    )
                }
            }
        }
    }

    fun deleteFolderConfirmed() {
        val folder = _uiState.value.folderToDelete ?: return
        _uiState.update { it.copy(folderToDelete = null) }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    feedbackMessage = FeedbackMessage("Deleting folder...", false)
                )
            }
            val currentPath = _uiState.value.currentPath
            val result = repository.deleteFolder(currentPath, folder.name)
            result.onSuccess {
                _uiState.update {
                    it.copy(
                        feedbackMessage = FeedbackMessage("Folder deleted successfully! 🗑️", true)
                    )
                }
                loadFiles(currentPath)
                calculateStorageUsage()
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        feedbackMessage = FeedbackMessage("Could not delete folder: ${err.message}", false)
                    )
                }
            }
        }
    }

    fun deleteFileConfirmed() {
        val file = _uiState.value.fileToDelete ?: return
        _uiState.update { it.copy(fileToDelete = null) }

        viewModelScope.launch {
            val result = repository.deleteFile(file.fullPath)
            result.onSuccess {
                _uiState.update {
                    it.copy(
                        feedbackMessage = FeedbackMessage("File deleted! 🗑️", true)
                    )
                }
                loadFiles(_uiState.value.currentPath)
                calculateStorageUsage()
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        feedbackMessage = FeedbackMessage("Delete failed: ${err.message}", false)
                    )
                }
            }
        }
    }

    fun uploadFilesFromUris(context: Context, uris: List<Uri>) {
        if (uris.isEmpty()) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isUploading = true,
                    uploadStatusText = "Uploading ${uris.size} file(s)..."
                )
            }

            var uploaded = 0
            var failed = 0
            val currentPath = _uiState.value.currentPath

            for ((index, uri) in uris.withIndex()) {
                _uiState.update {
                    it.copy(uploadStatusText = "Uploading ${index + 1} of ${uris.size}...")
                }
                try {
                    val fileName = getFileName(context, uri) ?: "upload_${System.currentTimeMillis()}"
                    val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"
                    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }

                    if (bytes != null) {
                        val result = repository.uploadFile(currentPath, fileName, mimeType, bytes)
                        if (result.isSuccess) {
                            uploaded++
                        } else {
                            failed++
                        }
                    } else {
                        failed++
                    }
                } catch (e: Exception) {
                    failed++
                }
            }

            _uiState.update {
                it.copy(
                    isUploading = false,
                    uploadStatusText = null,
                    feedbackMessage = if (failed == 0) {
                        FeedbackMessage("File uploaded! 🎉 ($uploaded files)", true)
                    } else {
                        FeedbackMessage("$uploaded uploaded, $failed failed.", false)
                    }
                )
            }

            loadFiles(currentPath)
            calculateStorageUsage()
        }
    }

    fun openFilePreview(file: NexFile) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    selectedPreviewFile = file,
                    previewSignedUrl = null,
                    previewTextContent = null,
                    isLoadingPreviewContent = true
                )
            }

            // Fetch signed URL
            val signedUrlResult = repository.getSignedUrl(file.fullPath, expiresInSeconds = 3600)
            val signedUrl = signedUrlResult.getOrNull()

            val ext = file.name.substringAfterLast('.', "").lowercase()
            var textContent: String? = null

            // If text / code file, download preview text
            if (ext in listOf("txt", "md", "json", "js", "html", "css", "kt", "xml", "csv", "log", "py")) {
                val bytesResult = repository.downloadFileBytes(file.fullPath)
                bytesResult.onSuccess { bytes ->
                    textContent = if (bytes.size > 200_000) {
                        String(bytes.take(200_000).toByteArray()) + "\n... [Truncated for preview]"
                    } else {
                        String(bytes)
                    }
                }
            }

            _uiState.update {
                it.copy(
                    previewSignedUrl = signedUrl,
                    previewTextContent = textContent,
                    isLoadingPreviewContent = false
                )
            }
        }
    }

    fun dismissFilePreview() {
        _uiState.update {
            it.copy(
                selectedPreviewFile = null,
                previewSignedUrl = null,
                previewTextContent = null
            )
        }
    }

    fun downloadFile(context: Context, file: NexFile) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(feedbackMessage = FeedbackMessage("Downloading ${file.name}...", false))
            }

            val bytesResult = repository.downloadFileBytes(file.fullPath)
            bytesResult.onSuccess { bytes ->
                val savedUri = saveFileToDownloads(context, file.name, file.mimeType, bytes)
                if (savedUri != null) {
                    _uiState.update {
                        it.copy(feedbackMessage = FeedbackMessage("Saved to Downloads: ${file.name} 📥", true))
                    }
                } else {
                    _uiState.update {
                        it.copy(feedbackMessage = FeedbackMessage("Saved file to device storage.", true))
                    }
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(feedbackMessage = FeedbackMessage("Download failed: ${err.message}", false))
                }
            }
        }
    }

    fun shareFile(context: Context, file: NexFile) {
        viewModelScope.launch {
            val signedResult = repository.getSignedUrl(file.fullPath, expiresInSeconds = 86400)
            signedResult.onSuccess { url ->
                val sendIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_SUBJECT, "NexCloud File: ${file.name}")
                    putExtra(Intent.EXTRA_TEXT, "View file on NexCloud:\n$url")
                    type = "text/plain"
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                val chooser = Intent.createChooser(sendIntent, "Share file link via").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(chooser)
            }.onFailure { err ->
                _uiState.update {
                    it.copy(feedbackMessage = FeedbackMessage("Could not generate share link: ${err.message}", false))
                }
            }
        }
    }

    fun calculateStorageUsage() {
        viewModelScope.launch {
            _uiState.update { it.copy(isCalculatingStorage = true) }
            val result = repository.calculateTotalStorageUsed()
            result.onSuccess { total ->
                _uiState.update {
                    it.copy(
                        storageUsedBytes = total,
                        isCalculatingStorage = false
                    )
                }
            }.onFailure {
                _uiState.update { it.copy(isCalculatingStorage = false) }
            }
        }
    }

    fun clearFeedbackMessage() {
        _uiState.update { it.copy(feedbackMessage = null) }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
            _uiState.update {
                DashboardUiState(isLoggedIn = false)
            }
        }
    }

    // ==========================================
    // FILE SAVE HELPERS
    // ==========================================

    private suspend fun saveFileToDownloads(
        context: Context,
        fileName: String,
        mimeType: String?,
        bytes: ByteArray
    ): Uri? = withContext(Dispatchers.IO) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType ?: "application/octet-stream")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/NexCloud")
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { it.write(bytes) }
                }
                uri
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val targetDir = File(downloadsDir, "NexCloud").apply { mkdirs() }
                val targetFile = File(targetDir, fileName)
                FileOutputStream(targetFile).use { it.write(bytes) }
                Uri.fromFile(targetFile)
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun getFileName(context: Context, uri: Uri): String? {
        var name: String? = null
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index != -1) {
                        name = it.getString(index)
                    }
                }
            }
        }
        if (name == null) {
            name = uri.path?.substringAfterLast('/')
        }
        return name
    }
}
