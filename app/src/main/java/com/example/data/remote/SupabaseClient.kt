package com.example.data.remote

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object SupabaseClient {
    const val SUPABASE_URL = "https://ckrxrqrvipizesmqaoku.supabase.co"
    const val SUPABASE_ANON_KEY = "sb_publishable_6PZ105oS0ckVwgKayME4CQ_rIOeUTdc"
    const val BUCKET_NAME = "files"
    const val QUOTA_BYTES = 1L * 1024L * 1024L * 1024L // 1 GB quota matching website

    private val authInterceptor = Interceptor { chain ->
        val original = chain.request()
        val requestBuilder = original.newBuilder()
            .header("apikey", SUPABASE_ANON_KEY)

        // If no Authorization header is set yet, also set apikey as Bearer fallback for storage
        if (original.header("Authorization") == null) {
            requestBuilder.header("Authorization", "Bearer $SUPABASE_ANON_KEY")
        }

        chain.proceed(requestBuilder.build())
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
    }

    val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(if (SUPABASE_URL.endsWith("/")) SUPABASE_URL else "$SUPABASE_URL/")
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    val apiService: SupabaseApiService = retrofit.create(SupabaseApiService::class.java)

    /**
     * Resolves signed URL or authenticated URL relative to base URL if needed.
     */
    fun resolveFullUrl(pathOrUrl: String): String {
        return if (pathOrUrl.startsWith("http://") || pathOrUrl.startsWith("https://")) {
            pathOrUrl
        } else {
            val cleanPath = pathOrUrl.removePrefix("/")
            "$SUPABASE_URL/$cleanPath"
        }
    }
}
