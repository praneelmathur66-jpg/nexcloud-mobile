package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CachedFileDao {
    @Query("SELECT * FROM cached_files WHERE userId = :userId AND parentPath = :parentPath ORDER BY isFolder DESC, name ASC")
    fun getFilesByFolder(userId: String, parentPath: String): Flow<List<CachedFileEntity>>

    @Query("SELECT * FROM cached_files WHERE userId = :userId AND isFavorite = 1 ORDER BY name ASC")
    fun getFavorites(userId: String): Flow<List<CachedFileEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(files: List<CachedFileEntity>)

    @Query("DELETE FROM cached_files WHERE userId = :userId AND parentPath = :parentPath")
    suspend fun clearFolder(userId: String, parentPath: String)

    @Query("DELETE FROM cached_files WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM cached_files WHERE id LIKE :idPrefix || '%'")
    suspend fun deleteByPrefix(idPrefix: String)

    @Query("UPDATE cached_files SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun setFavorite(id: String, isFavorite: Boolean)

    @Query("DELETE FROM cached_files WHERE userId = :userId")
    suspend fun clearUser(userId: String)

    @Query("SELECT SUM(sizeBytes) FROM cached_files WHERE userId = :userId AND isFolder = 0")
    suspend fun getTotalStorageBytes(userId: String): Long?
}
