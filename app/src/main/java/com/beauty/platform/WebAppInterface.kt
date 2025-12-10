package com.beauty.platform

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import com.beauty.platform.api.FcmApiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.Locale

/**
 * 웹뷰의 JavaScript가 호출할 수 있는 메서드를 정의하는 클래스.
 * @param context Android Context
 * @param webView WebView 인스턴스
 * @param onCheckLoginResult 로그인 상태 확인 결과를 처리할 콜백. (isLoggedIn: Boolean) -> Unit
 */
class WebAppInterface(
    private val context: Context,
    private val webView: WebView,
    private val onCheckLoginResult: (Boolean) -> Unit,
    private val onPageFullyLoaded: (() -> Unit)? = null
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(Dispatchers.Main)
    
    companion object {
        const val INTERFACE_NAME = "Android"
        private const val TAG = "WebAppInterface"
        private const val PREF_FCM = "fcm_token"
        private const val KEY_TOKEN = "token"
        private const val KEY_MEMBER_ID = "member_id"
    }

    /**
     * FCM 토큰 가져오기 (기존)
     */
    @JavascriptInterface
    fun getFCMToken(): String? {
        return try {
            val sharedPref = context.getSharedPreferences(PREF_FCM, Context.MODE_PRIVATE)
            val token = sharedPref.getString(KEY_TOKEN, null)
            Log.d(TAG, "📱 FCM Token: ${token?.take(20)}...")
            token
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to get FCM token", e)
            null
        }
    }
    
    /**
     * 디바이스 ID 가져오기
     */
    @JavascriptInterface
    fun getDeviceId(): String {
        return Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        )
    }
    
    /**
     * 디바이스 언어 가져오기
     */
    @JavascriptInterface
    fun getDeviceLanguage(): String {
        return Locale.getDefault().language // "en", "ko", "ja", "zh" 등
    }

    /**
     * 서버에 토큰 등록 (개선)
     */
    @JavascriptInterface
    fun sendTokenToServer(token: String, userId: String? = null) {
        scope.launch {
            try {
                val deviceId = getDeviceId()
                val language = getDeviceLanguage()
                
                Log.d(TAG, "📤 Registering token to server...")
                Log.d(TAG, "  - Token: ${token.take(20)}...")
                Log.d(TAG, "  - Device: $deviceId")
                Log.d(TAG, "  - Language: $language")
                Log.d(TAG, "  - UserID: $userId")
                
                // 1. 토큰 등록
                val registerResult = FcmApiClient.registerToken(
                    fcmToken = token,
                    deviceId = deviceId,
                    platform = "android",
                    preferredLanguage = language
                )
                
                if (registerResult.isSuccess) {
                    Log.d(TAG, "✅ Token registered to server")
                    
                    // 2. 로그인 상태면 회원 연결
                    if (userId != null) {
                        val connectResult = FcmApiClient.connectMember(token, userId)
                        if (connectResult.isSuccess) {
                            Log.d(TAG, "✅ Member connected to token")
                            saveMemberId(userId)
                        }
                    }
                } else {
                    Log.e(TAG, "❌ Failed to register token")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Send token error", e)
            }
        }
    }

    /**
     * 로그인 결과 처리 (개선)
     */
    @JavascriptInterface
    fun onLoginResult(jsonResult: String) {
        try {
            Log.d(TAG, "📥 Login result: $jsonResult")
            val json = JSONObject(jsonResult)
            val isLoggedIn = json.optBoolean("isLoggedIn", false)
            
            // 기존 콜백 호출
            onCheckLoginResult(isLoggedIn)
            
            if (isLoggedIn) {
                val userId = json.optString("userId", "")
                if (userId.isNotEmpty()) {
                    Log.d(TAG, "✅ User logged in: $userId")
                    
                    // FCM 토큰 연결
                    connectMemberToToken(userId)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Login result error", e)
            onCheckLoginResult(false) // 파싱 실패 시 로그인 안 된 것으로 간주
        }
    }

    /**
     * 로그아웃 처리 (신규)
     */
    @JavascriptInterface
    fun onLogout() {
        scope.launch {
            try {
                val token = getFCMToken()
                if (token != null) {
                    Log.d(TAG, "📤 Disconnecting member from token...")
                    val result = FcmApiClient.disconnectMember(token)
                    
                    if (result.isSuccess) {
                        Log.d(TAG, "✅ Member disconnected")
                        clearMemberId()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Logout error", e)
            }
        }
    }

    /**
     * 언어 변경 (신규)
     */
    @JavascriptInterface
    fun updateLanguage(language: String) {
        scope.launch {
            try {
                val token = getFCMToken()
                if (token != null) {
                    Log.d(TAG, "📤 Updating language to: $language")
                    val result = FcmApiClient.updateLanguage(token, language)
                    
                    if (result.isSuccess) {
                        Log.d(TAG, "✅ Language updated")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Update language error", e)
            }
        }
    }

    /**
     * 푸시 설정 변경 (신규)
     */
    @JavascriptInterface
    fun updatePushPreferences(
        allowGeneral: Boolean,
        allowActivity: Boolean,
        allowMarketing: Boolean
    ) {
        scope.launch {
            try {
                val token = getFCMToken()
                if (token != null) {
                    Log.d(TAG, "📤 Updating push preferences...")
                    val result = FcmApiClient.updatePreferences(
                        fcmToken = token,
                        allowGeneral = allowGeneral,
                        allowActivity = allowActivity,
                        allowMarketing = allowMarketing
                    )
                    
                    if (result.isSuccess) {
                        Log.d(TAG, "✅ Preferences updated")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Update preferences error", e)
            }
        }
    }

    @JavascriptInterface
    fun onPushNotificationReceived(notificationData: String) {
        Log.d(TAG, "웹에서 푸시 알림 데이터 처리 요청: $notificationData")
        try {
            val jsonObject = JSONObject(notificationData)
            // 웹에서 전달받은 알림 데이터를 처리
            val title = jsonObject.optString("title", "")
            val body = jsonObject.optString("body", "")
            val url = jsonObject.optString("url", "")
            
            // 필요한 처리 로직 구현
        } catch (e: Exception) {
            Log.e(TAG, "푸시 알림 데이터 파싱 실패", e)
        }
    }

    @JavascriptInterface
    fun goBack() {
        Log.d(TAG, "웹에서 뒤로가기 요청")
        // UI 스레드에서 실행해야 함
        (context as? Activity)?.runOnUiThread {
            if (webView.canGoBack()) {
                Log.d(TAG, "WebView 히스토리 뒤로가기")
                webView.goBack()
            } else {
                Log.d(TAG, "WebView 히스토리가 없음, 액티비티 종료")
                // WebView history가 없으면 액티비티 종료
                (context as? Activity)?.finish()
            }
        }
    }

    @JavascriptInterface
    fun onLoadComplete() {
        Log.d(TAG, "페이지 모든 리소스 로드 완료")
        // JS에서 호출되므로 메인 스레드로 전환
        mainHandler.post {
            onPageFullyLoaded?.invoke()
        }
    }
    
    // ===========================
    // Private 헬퍼 함수
    // ===========================
    
    private fun connectMemberToToken(memberId: String) {
        scope.launch {
            try {
                val token = getFCMToken()
                if (token != null) {
                    Log.d(TAG, "📤 Connecting member to token...")
                    val result = FcmApiClient.connectMember(token, memberId)
                    
                    if (result.isSuccess) {
                        Log.d(TAG, "✅ Member connected")
                        saveMemberId(memberId)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Connect member error", e)
            }
        }
    }
    
    private fun saveMemberId(memberId: String) {
        val sharedPref = context.getSharedPreferences(PREF_FCM, Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString(KEY_MEMBER_ID, memberId)
            apply()
        }
        Log.d(TAG, "💾 Member ID saved")
    }
    
    private fun clearMemberId() {
        val sharedPref = context.getSharedPreferences(PREF_FCM, Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            remove(KEY_MEMBER_ID)
            apply()
        }
        Log.d(TAG, "🗑️ Member ID cleared")
    }
}
