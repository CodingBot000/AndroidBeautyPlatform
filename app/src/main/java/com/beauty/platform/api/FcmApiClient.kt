package com.beauty.platform.api

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object FcmApiClient {
    private const val TAG = "FcmApiClient"
    
    // TODO: 환경에 따라 변경
    private const val BASE_URL = "https://ed-unfoliated-nontumultuously.ngrok-free.dev"
    // private const val BASE_URL = "https://mimotok.com" // 프로덕션
    
    /**
     * FCM 토큰 등록
     */
    suspend fun registerToken(
        fcmToken: String,
        deviceId: String,
        platform: String = "android",
        preferredLanguage: String = "en"
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BASE_URL/api/fcm/register")
            val connection = url.openConnection() as HttpURLConnection
            
            connection.apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "application/json")
                doOutput = true
                connectTimeout = 30000
                readTimeout = 30000
            }
            
            val jsonBody = JSONObject().apply {
                put("fcmToken", fcmToken)
                put("deviceId", deviceId)
                put("platform", platform)
                put("preferredLanguage", preferredLanguage)
            }
            
            Log.d(TAG, "📤 Registering token...")
            Log.d(TAG, "  URL: $BASE_URL/api/fcm/register")
            Log.d(TAG, "  Body: $jsonBody")
            
            connection.outputStream.use { os ->
                os.write(jsonBody.toString().toByteArray())
            }
            
            val responseCode = connection.responseCode
            if (responseCode in 200..299) {
                val responseBody = connection.inputStream.bufferedReader().readText()
                Log.d(TAG, "✅ Token registered successfully: $responseCode")
                Log.d(TAG, "  Response: $responseBody")
                Result.success(Unit)
            } else {
                val errorBody = connection.errorStream?.bufferedReader()?.readText() ?: "No error details"
                Log.e(TAG, "❌ Register failed: $responseCode")
                Log.e(TAG, "  Error: $errorBody")
                Result.failure(Exception("HTTP $responseCode: $errorBody"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Register error", e)
            Result.failure(e)
        }
    }
    
    /**
     * 로그인 시 회원 연결
     */
    suspend fun connectMember(
        fcmToken: String,
        idUuidMember: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BASE_URL/api/fcm/connect-member")
            val connection = url.openConnection() as HttpURLConnection
            
            connection.apply {
                requestMethod = "PUT"
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "application/json")
                doOutput = true
                connectTimeout = 30000
                readTimeout = 30000
            }
            
            val jsonBody = JSONObject().apply {
                put("fcmToken", fcmToken)
                put("idUuidMember", idUuidMember)
            }
            
            Log.d(TAG, "📤 Connecting member...")
            Log.d(TAG, "  URL: $BASE_URL/api/fcm/connect-member")
            Log.d(TAG, "  Body: $jsonBody")
            
            connection.outputStream.use { os ->
                os.write(jsonBody.toString().toByteArray())
            }
            
            val responseCode = connection.responseCode
            if (responseCode in 200..299) {
                val responseBody = connection.inputStream.bufferedReader().readText()
                Log.d(TAG, "✅ Member connected successfully: $responseCode")
                Log.d(TAG, "  Response: $responseBody")
                Result.success(Unit)
            } else {
                val errorBody = connection.errorStream?.bufferedReader()?.readText() ?: "No error details"
                Log.e(TAG, "❌ Connect member failed: $responseCode")
                Log.e(TAG, "  Error: $errorBody")
                Result.failure(Exception("HTTP $responseCode: $errorBody"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Connect member error", e)
            Result.failure(e)
        }
    }
    
    /**
     * 로그아웃 시 회원 연결 해제
     */
    suspend fun disconnectMember(fcmToken: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BASE_URL/api/fcm/disconnect-member")
            val connection = url.openConnection() as HttpURLConnection
            
            connection.apply {
                requestMethod = "PUT"
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "application/json")
                doOutput = true
                connectTimeout = 30000
                readTimeout = 30000
            }
            
            val jsonBody = JSONObject().apply {
                put("fcmToken", fcmToken)
            }
            
            Log.d(TAG, "📤 Disconnecting member...")
            Log.d(TAG, "  URL: $BASE_URL/api/fcm/disconnect-member")
            Log.d(TAG, "  Body: $jsonBody")
            
            connection.outputStream.use { os ->
                os.write(jsonBody.toString().toByteArray())
            }
            
            val responseCode = connection.responseCode
            if (responseCode in 200..299) {
                val responseBody = connection.inputStream.bufferedReader().readText()
                Log.d(TAG, "✅ Member disconnected successfully: $responseCode")
                Log.d(TAG, "  Response: $responseBody")
                Result.success(Unit)
            } else {
                val errorBody = connection.errorStream?.bufferedReader()?.readText() ?: "No error details"
                Log.e(TAG, "❌ Disconnect member failed: $responseCode")
                Log.e(TAG, "  Error: $errorBody")
                Result.failure(Exception("HTTP $responseCode: $errorBody"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Disconnect member error", e)
            Result.failure(e)
        }
    }
    
    /**
     * 언어 설정 변경
     */
    suspend fun updateLanguage(
        fcmToken: String,
        preferredLanguage: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BASE_URL/api/fcm/update-language")
            val connection = url.openConnection() as HttpURLConnection
            
            connection.apply {
                requestMethod = "PUT"
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "application/json")
                doOutput = true
                connectTimeout = 30000
                readTimeout = 30000
            }
            
            val jsonBody = JSONObject().apply {
                put("fcmToken", fcmToken)
                put("preferredLanguage", preferredLanguage)
            }
            
            Log.d(TAG, "📤 Updating language...")
            Log.d(TAG, "  URL: $BASE_URL/api/fcm/update-language")
            Log.d(TAG, "  Body: $jsonBody")
            
            connection.outputStream.use { os ->
                os.write(jsonBody.toString().toByteArray())
            }
            
            val responseCode = connection.responseCode
            if (responseCode in 200..299) {
                val responseBody = connection.inputStream.bufferedReader().readText()
                Log.d(TAG, "✅ Language updated successfully: $responseCode")
                Log.d(TAG, "  Response: $responseBody")
                Result.success(Unit)
            } else {
                val errorBody = connection.errorStream?.bufferedReader()?.readText() ?: "No error details"
                Log.e(TAG, "❌ Update language failed: $responseCode")
                Log.e(TAG, "  Error: $errorBody")
                Result.failure(Exception("HTTP $responseCode: $errorBody"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Update language error", e)
            Result.failure(e)
        }
    }
    
    /**
     * 푸시 설정 변경
     */
    suspend fun updatePreferences(
        fcmToken: String,
        allowGeneral: Boolean,
        allowActivity: Boolean,
        allowMarketing: Boolean
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BASE_URL/api/fcm/update-preferences")
            val connection = url.openConnection() as HttpURLConnection
            
            connection.apply {
                requestMethod = "PUT"
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "application/json")
                doOutput = true
                connectTimeout = 30000
                readTimeout = 30000
            }
            
            val jsonBody = JSONObject().apply {
                put("fcmToken", fcmToken)
                put("allowGeneral", allowGeneral)
                put("allowActivity", allowActivity)
                put("allowMarketing", allowMarketing)
            }
            
            Log.d(TAG, "📤 Updating preferences...")
            Log.d(TAG, "  URL: $BASE_URL/api/fcm/update-preferences")
            Log.d(TAG, "  Body: $jsonBody")
            
            connection.outputStream.use { os ->
                os.write(jsonBody.toString().toByteArray())
            }
            
            val responseCode = connection.responseCode
            if (responseCode in 200..299) {
                val responseBody = connection.inputStream.bufferedReader().readText()
                Log.d(TAG, "✅ Preferences updated successfully: $responseCode")
                Log.d(TAG, "  Response: $responseBody")
                Result.success(Unit)
            } else {
                val errorBody = connection.errorStream?.bufferedReader()?.readText() ?: "No error details"
                Log.e(TAG, "❌ Update preferences failed: $responseCode")
                Log.e(TAG, "  Error: $errorBody")
                Result.failure(Exception("HTTP $responseCode: $errorBody"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Update preferences error", e)
            Result.failure(e)
        }
    }
}