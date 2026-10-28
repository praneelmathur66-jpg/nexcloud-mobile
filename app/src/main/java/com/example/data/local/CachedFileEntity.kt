package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_files")
data class CachedFileEntity(
    @PrimaryKey
    val id: String, // Full path inside bucket: userId/folder/file.ext
    val userId: String,
    val parentPath: String, // Folder path relative to user root, e.g. "" or "documents"
    val name: String,
    val isFolder: Boolean,
    val sizeBytes: Long,
    val mimeType: String?,
    val updatedAt: Long,
    val isFavorite: Boolean = false,
    val localUri: String? = null
)
