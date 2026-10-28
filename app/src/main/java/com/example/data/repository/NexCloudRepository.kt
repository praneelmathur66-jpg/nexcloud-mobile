package com.example.data.repository

import android.content.Context
import com.example.data.local.CachedFileDao
import com.example.data.local.CachedFileEntity
import com.example.data.local.NexCloudDatabase
import com.example.data.local.SessionManager
import com.example.data.remote.AuthResponse
import com.example.data.remote.DeleteObjectsRequest
import com.example.data.remote.ListObjectsRequest
import com.example.data.remote.LoginRequest
import com.example.data.remote.RecoverRequest
import com.example.data.remote.SignUrlRequest
import com.example.data.remote.SignUpRequest
import com.example.data.remote.StorageItemResponse
import com.example.data.remote.SupabaseApiService
import com.example.data.remote.SupabaseClient
import com.example.data.remote.SupabaseUser
import com.example.data.remote.UpdateUserRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.text.DecimalFormat

data class NexFile(
    val id: String,
    val name: String,
    val fullPath: String,
    val sizeBytes: Long,
    val mimeType: String?,
    val updatedAt: String?,
    val isFavorite: Boolean = false,
    val previewUrl: String? = null
) {
    val formattedSize: String get() = formatBytes(sizeBytes)
}

data class NexFolder(
    val name: String,
    val fullPath: String
)

class NexCloudRepository(
    private val apiService: SupabaseApiService = SupabaseClient.apiService,
    private val sessionManager: SessionManager,
    private val cachedFileDao: CachedFileDao
) {

    companion object {
        fun create(context: Context): NexCloudRepository {
            val session = SessionManager(context)
            val db = NexCloudDatabase.getInstance(context)
            return NexCloudRepository(
                apiService = SupabaseClient.apiService,
                sessionManager = session,
                cachedFileDao = db.cachedFileDao()
            )
        }
    }

    val session: SessionManager get() = sessionManager

    // ==========================================
    // AUTHENTICATION
    // ==========================================

    suspend fun login(email: String, password: String): Result<SupabaseUser> =
        withContext(Dispatchers.IO) {
            try {
                val response = apiService.login(LoginRequest(email.trim(), password))
                val token = response.accessToken
                val user = response.user
                if (!token.isNullOrEmpty() && user != null) {
                    sessionManager.saveSession(
                        accessToken = token,
                        refreshToken = response.refreshToken,
                        userId = user.id,
                        email = user.email ?: email.trim()
                    )
                    Result.success(user)
                } else {
                    Result.failure(Exception("Login succeeded but no token returned"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun signUp(email: String, password: String): Result<AuthResponse> =
        withContext(Dispatchers.IO) {
            try {
                val response = apiService.signUp(SignUpRequest(email.trim(), password))
                if (response.accessToken != null && response.user != null) {
                    sessionManager.saveSession(
                        accessToken = response.accessToken,
                        refreshToken = response.refreshToken,
                        userId = response.user.id,
                        email = response.user.email ?: email.trim()
                    )
                }
                Result.success(response)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun sendPasswordReset(email: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                apiService.recoverPassword(RecoverRequest(email.trim()))
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun updatePassword(newPassword: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val authHeader = sessionManager.getAuthHeader()
                    ?: return@withContext Result.failure(Exception("Not authenticated"))
                apiService.updatePassword(authHeader, UpdateUserRequest(newPassword))
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun logout() = withContext(Dispatchers.IO) {
        val currentUserId = sessionManager.getUserId()
        if (currentUserId != null) {
            try {
                cachedFileDao.clearUser(currentUserId)
            } catch (_: Exception) { }
        }
        sessionManager.clearSession()
    }

    // ==========================================
    // STORAGE / FILE MANAGEMENT
    // ==========================================

    fun getCachedFiles(folderPath: String): Flow<List<CachedFileEntity>> {
        val userId = sessionManager.getUserId() ?: ""
        return cachedFileDao.getFilesByFolder(userId, folderPath)
    }

    /**
     * Lists files & folders in the given relative folder path.
     * folderPath: "" for root, "Docs" or "Docs/Work" for subfolders.
     */
    suspend fun listFilesAndFolders(
        folderPath: String
    ): Result<Pair<List<NexFolder>, List<NexFile>>> = withContext(Dispatchers.IO) {
        val userId = sessionManager.getUserId()
            ?: return@withContext Result.failure(Exception("User not authenticated"))

        val prefix = if (folderPath.isEmpty()) userId else "$userId/$folderPath"

        try {
            val items = apiService.listObjects(
                authorization = sessionManager.getAuthHeader(),
                request = ListObjectsRequest(prefix = prefix, limit = 1000)
            )

            val folders = mutableListOf<NexFolder>()
            val files = mutableListOf<NexFile>()
            val entitiesToCache = mutableListOf<CachedFileEntity>()

            for (item in items) {
                val isDirectory = item.id == null || item.metadata == null
                if (isDirectory) {
                    val fullItemPath = if (folderPath.isEmpty()) item.name else "$folderPath/${item.name}"
                    folders.add(NexFolder(name = item.name, fullPath = fullItemPath))
                    entitiesToCache.add(
                        CachedFileEntity(
                            id = "$prefix/${item.name}",
                            userId = userId,
                            parentPath = folderPath,
                            name = item.name,
                            isFolder = true,
                            sizeBytes = 0L,
                            mimeType = null,
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                } else if (item.name != ".folder") {
                    val fullPath = "$prefix/${item.name}"
                    val size = item.metadata?.size ?: 0L
                    val mime = item.metadata?.mimetype

                    files.add(
                        NexFile(
                            id = item.id ?: fullPath,
                            name = item.name,
                            fullPath = fullPath,
                            sizeBytes = size,
                            mimeType = mime,
                            updatedAt = item.updatedAt ?: item.createdAt
                        )
                    )
                    entitiesToCache.add(
                        CachedFileEntity(
                            id = fullPath,
                            userId = userId,
                            parentPath = folderPath,
                            name = item.name,
                            isFolder = false,
                            sizeBytes = size,
                            mimeType = mime,
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                }
            }

            // Update local Room cache for this folder
            try {
                cachedFileDao.clearFolder(userId, folderPath)
                cachedFileDao.insertAll(entitiesToCache)
            } catch (_: Exception) { }

            Result.success(Pair(folders, files))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Creates a new folder by uploading an empty .folder marker file.
     */
    suspend fun createFolder(parentFolderPath: String, folderName: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            val userId = sessionManager.getUserId()
                ?: return@withContext Result.failure(Exception("User not authenticated"))

            val cleanName = cleanPart(folderName)
            if (cleanName.isBlank()) {
                return@withContext Result.failure(Exception("Invalid folder name"))
            }

            val storagePath = buildString {
                append(userId)
                append("/")
                if (parentFolderPath.isNotEmpty()) {
                    append(parentFolderPath)
                    append("/")
                }
                append(cleanName)
                append("/.folder")
            }

            try {
                val emptyBody = "".toRequestBody("text/plain".toMediaTypeOrNull())
                apiService.uploadObject(
                    authorization = sessionManager.getAuthHeader(),
                    upsert = "true",
                    contentType = "text/plain",
                    path = storagePath,
                    body = emptyBody
                )
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Uploads a file with binary data.
     */
    suspend fun uploadFile(
        folderPath: String,
        fileName: String,
        mimeType: String,
        fileBytes: ByteArray
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val userId = sessionManager.getUserId()
            ?: return@withContext Result.failure(Exception("User not authenticated"))

        val cleanName = cleanPart(fileName)
        val storagePath = buildString {
            append(userId)
            append("/")
            if (folderPath.isNotEmpty()) {
                append(folderPath)
                append("/")
            }
            append(cleanName)
        }

        try {
            val contentType = mimeType.ifBlank { "application/octet-stream" }
            val requestBody = fileBytes.toRequestBody(contentType.toMediaTypeOrNull())

            apiService.uploadObject(
                authorization = sessionManager.getAuthHeader(),
                upsert = "true",
                contentType = contentType,
                path = storagePath,
                body = requestBody
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Deletes a single file by full path.
     */
    suspend fun deleteFile(fullPath: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            apiService.deleteObjects(
                authorization = sessionManager.getAuthHeader(),
                request = DeleteObjectsRequest(prefixes = listOf(fullPath))
            )
            cachedFileDao.deleteById(fullPath)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Recursively scans and deletes all files inside a folder and its subfolders.
     */
    suspend fun deleteFolder(parentFolderPath: String, folderName: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            val userId = sessionManager.getUserId()
                ?: return@withContext Result.failure(Exception("User not authenticated"))

            val targetPrefix = buildString {
                append(userId)
                append("/")
                if (parentFolderPath.isNotEmpty()) {
                    append(parentFolderPath)
                    append("/")
                }
                append(folderName)
            }

            try {
                // Find all keys inside targetPrefix
                val allPaths = getAllStorageFiles(targetPrefix)
                if (allPaths.isEmpty()) {
                    // Try removing the marker .folder directly
                    val marker = "$targetPrefix/.folder"
                    apiService.deleteObjects(
                        authorization = sessionManager.getAuthHeader(),
                        request = DeleteObjectsRequest(prefixes = listOf(marker))
                    )
                } else {
                    // Delete in chunks of 100 items (matching web app)
                    allPaths.chunked(100).forEach { batch ->
                        apiService.deleteObjects(
                            authorization = sessionManager.getAuthHeader(),
                            request = DeleteObjectsRequest(prefixes = batch)
                        )
                    }
                }
                cachedFileDao.deleteByPrefix(targetPrefix)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Recursively retrieves all storage object paths under a prefix.
     */
    private suspend fun getAllStorageFiles(prefix: String): List<String> {
        val results = mutableListOf<String>()
        try {
            val items = apiService.listObjects(
                authorization = sessionManager.getAuthHeader(),
                request = ListObjectsRequest(prefix = prefix, limit = 1000)
            )
            for (item in items) {
                val fullPath = "$prefix/${item.name}"
                val isDirectory = item.id == null || item.metadata == null
                if (isDirectory) {
                    val nested = getAllStorageFiles(fullPath)
                    results.addAll(nested)
                } else {
                    // Include all files and .folder markers
                    results.add(fullPath)
                }
            }
        } catch (_: Exception) { }
        return results
    }

    /**
     * Creates a signed download URL valid for [expiresInSeconds].
     */
    suspend fun getSignedUrl(fullPath: String, expiresInSeconds: Int = 3600): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val response = apiService.createSignedUrl(
                    authorization = sessionManager.getAuthHeader(),
                    path = fullPath,
                    request = SignUrlRequest(expiresIn = expiresInSeconds)
                )
                val resolved = SupabaseClient.resolveFullUrl(response.signedURL)
                Result.success(resolved)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Calculates user's total stored bytes by scanning all folders under userId.
     */
    suspend fun calculateTotalStorageUsed(): Result<Long> = withContext(Dispatchers.IO) {
        val userId = sessionManager.getUserId()
            ?: return@withContext Result.failure(Exception("User not authenticated"))

        var total = 0L

        suspend fun scan(path: String) {
            try {
                val items = apiService.listObjects(
                    authorization = sessionManager.getAuthHeader(),
                    request = ListObjectsRequest(prefix = path, limit = 1000)
                )
                for (item in items) {
                    val fullPath = "$path/${item.name}"
                    val isDirectory = item.id == null || item.metadata == null
                    if (isDirectory) {
                        scan(fullPath)
                    } else if (item.name != ".folder") {
                        total += (item.metadata?.size ?: 0L)
                    }
                }
            } catch (_: Exception) { }
        }

        try {
            scan(userId)
            Result.success(total)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Downloads raw bytes of an object for in-app viewing or saving.
     */
    suspend fun downloadFileBytes(fullPath: String): Result<ByteArray> =
        withContext(Dispatchers.IO) {
            try {
                val responseBody = apiService.downloadObject(
                    authorization = sessionManager.getAuthHeader(),
                    path = fullPath
                )
                val bytes = responseBody.bytes()
                Result.success(bytes)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    fun cleanPart(name: String): String {
        return name
            .replace(Regex("[^a-zA-Z0-9._ -]"), "_")
            .replace(Regex("^\\.+$"), "_")
            .trim()
    }
}

fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    var digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
    if (digitGroups >= units.size) digitGroups = units.size - 1
    val df = DecimalFormat("#,##0.##")
    return "${df.format(bytes / Math.pow(1024.0, digitGroups.toDouble()))} ${units[digitGroups]}"
}
