package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Streaming

// ==========================================
// AUTH DATA MODELS
// ==========================================

@JsonClass(generateAdapter = true)
data class LoginRequest(
    val email: String,
    val password: String
)

@JsonClass(generateAdapter = true)
data class SignUpRequest(
    val email: String,
    val password: String
)

@JsonClass(generateAdapter = true)
data class RecoverRequest(
    val email: String
)

@JsonClass(generateAdapter = true)
data class UpdateUserRequest(
    val password: String
)

@JsonClass(generateAdapter = true)
data class AuthResponse(
    @Json(name = "access_token") val accessToken: String? = null,
    @Json(name = "token_type") val tokenType: String? = null,
    @Json(name = "expires_in") val expiresIn: Long? = null,
    @Json(name = "refresh_token") val refreshToken: String? = null,
    val user: SupabaseUser? = null
)

@JsonClass(generateAdapter = true)
data class SupabaseUser(
    val id: String,
    val email: String? = null,
    @Json(name = "created_at") val createdAt: String? = null
)

// ==========================================
// STORAGE DATA MODELS
// ==========================================

@JsonClass(generateAdapter = true)
data class ListObjectsRequest(
    val prefix: String,
    val limit: Int = 1000,
    val sortBy: SortByOptions = SortByOptions("name", "asc")
)

@JsonClass(generateAdapter = true)
data class SortByOptions(
    val column: String = "name",
    val order: String = "asc"
)

@JsonClass(generateAdapter = true)
data class StorageItemResponse(
    val name: String,
    val id: String? = null,
    @Json(name = "updated_at") val updatedAt: String? = null,
    @Json(name = "created_at") val createdAt: String? = null,
    @Json(name = "last_accessed_at") val lastAccessedAt: String? = null,
    val metadata: StorageItemMetadata? = null
)

@JsonClass(generateAdapter = true)
data class StorageItemMetadata(
    val size: Long? = 0L,
    val mimetype: String? = null
)

@JsonClass(generateAdapter = true)
data class SignUrlRequest(
    val expiresIn: Int = 3600
)

@JsonClass(generateAdapter = true)
data class SignUrlResponse(
    @Json(name = "signedURL") val signedURL: String
)

@JsonClass(generateAdapter = true)
data class DeleteObjectsRequest(
    val prefixes: List<String>
)

// ==========================================
// RETROFIT API SERVICE
// ==========================================

interface SupabaseApiService {

    @POST("auth/v1/token?grant_type=password")
    suspend fun login(
        @Body request: LoginRequest
    ): AuthResponse

    @POST("auth/v1/signup")
    suspend fun signUp(
        @Body request: SignUpRequest
    ): AuthResponse

    @POST("auth/v1/recover")
    suspend fun recoverPassword(
        @Body request: RecoverRequest
    ): Map<String, Any?>

    @PUT("auth/v1/user")
    suspend fun updatePassword(
        @Header("Authorization") authorization: String,
        @Body request: UpdateUserRequest
    ): SupabaseUser

    @GET("auth/v1/user")
    suspend fun getCurrentUser(
        @Header("Authorization") authorization: String
    ): SupabaseUser

    @POST("storage/v1/object/list/files")
    suspend fun listObjects(
        @Header("Authorization") authorization: String?,
        @Body request: ListObjectsRequest
    ): List<StorageItemResponse>

    @POST("storage/v1/object/sign/files/{path}")
    suspend fun createSignedUrl(
        @Header("Authorization") authorization: String?,
        @Path(value = "path", encoded = true) path: String,
        @Body request: SignUrlRequest
    ): SignUrlResponse

    @POST("storage/v1/object/files/{path}")
    suspend fun uploadObject(
        @Header("Authorization") authorization: String?,
        @Header("x-upsert") upsert: String = "true",
        @Header("Content-Type") contentType: String,
        @Path(value = "path", encoded = true) path: String,
        @Body body: RequestBody
    ): Map<String, Any?>

    @HTTP(method = "DELETE", path = "storage/v1/object/files", hasBody = true)
    suspend fun deleteObjects(
        @Header("Authorization") authorization: String?,
        @Body request: DeleteObjectsRequest
    ): List<Map<String, Any?>>

    @GET("storage/v1/object/authenticated/files/{path}")
    @Streaming
    suspend fun downloadObject(
        @Header("Authorization") authorization: String?,
        @Path(value = "path", encoded = true) path: String
    ): ResponseBody
}
