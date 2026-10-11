package com.example.data.network

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.example.data.model.RobotSettings
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.ByteArrayInputStream
import java.util.concurrent.TimeUnit

class RobotApiClient {

    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private var currentBaseUrl: String = ""
    private var apiService: RobotApiService? = null

    private fun buildOkHttpClient(settings: RobotSettings): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        return OkHttpClient.Builder()
            .connectTimeout(settings.connectionTimeoutMs, TimeUnit.MILLISECONDS)
            .readTimeout(settings.requestTimeoutMs, TimeUnit.MILLISECONDS)
            .writeTimeout(settings.requestTimeoutMs, TimeUnit.MILLISECONDS)
            .retryOnConnectionFailure(true)
            .addInterceptor(logging)
            .build()
    }

    @Synchronized
    fun getApiService(settings: RobotSettings): RobotApiService {
        val baseUrl = "http://${settings.mainEspIp}:${settings.mainEspPort}/"
        if (apiService == null || currentBaseUrl != baseUrl) {
            currentBaseUrl = baseUrl
            val client = buildOkHttpClient(settings)
            val retrofit = Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
            apiService = retrofit.create(RobotApiService::class.java)
        }
        return apiService!!
    }

    suspend fun testEspConnection(settings: RobotSettings): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val service = getApiService(settings)
            val response = service.getStatus()
            if (response.isSuccessful) {
                Result.success(true)
            } else {
                Result.failure(Exception("HTTP ${response.code()}: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun captureCamImage(settings: RobotSettings): Result<Bitmap> = withContext(Dispatchers.IO) {
        try {
            val client = buildOkHttpClient(settings)
            val request = Request.Builder()
                .url(settings.camCaptureUrl)
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("Camera returned HTTP ${response.code}"))
                }
                val bytes = response.body?.bytes()
                    ?: return@withContext Result.failure(Exception("Empty camera response"))
                
                val bitmap = BitmapFactory.decodeStream(ByteArrayInputStream(bytes))
                if (bitmap != null) {
                    Result.success(bitmap)
                } else {
                    Result.failure(Exception("Failed to decode JPEG image from camera stream"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun testCamConnection(settings: RobotSettings): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val client = buildOkHttpClient(settings)
            val request = Request.Builder()
                .url(settings.camCaptureUrl)
                .head()
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful || response.code == 405) { // some esp32-cam only handle GET
                    Result.success(true)
                } else {
                    // Try quick GET with tiny read
                    val getReq = Request.Builder().url(settings.camCaptureUrl).get().build()
                    client.newCall(getReq).execute().use { getResp ->
                        if (getResp.isSuccessful) Result.success(true)
                        else Result.failure(Exception("Camera returned HTTP ${getResp.code}"))
                    }
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
