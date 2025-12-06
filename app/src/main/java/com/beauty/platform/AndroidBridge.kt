package com.beauty.platform

import android.content.Context
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView

/**
 * WebView JavaScript Interface for Google Authentication
 * 웹에서 Android 네이티브 Google 로그인을 호출할 수 있게 하는 브리지
 */
class AndroidBridge(
    private val context: Context,
    private val webView: WebView
) {

    /**
     * 웹에서 호출 가능한 Google 로그인 메서드
     * JavaScript에서 호출: window.AndroidBridge.requestGoogleLogin()
     */
    @JavascriptInterface
    fun requestGoogleLogin() {
        Log.d("AndroidBridge", "Google 로그인 요청 받음 (웹에서 호출)")
        
        val googleAuthManager = GoogleAuthManager(
            context = context,
            onSuccess = { idToken ->
                Log.d("AndroidBridge", "Google 로그인 성공, ID Token을 웹으로 전달")
                // UI 스레드에서 JavaScript 실행
                webView.post {
                    webView.evaluateJavascript("window.onGoogleToken('$idToken')", null)
                }
            },
            onError = { errorMessage ->
                Log.e("AndroidBridge", "Google 로그인 실패: $errorMessage")
                // UI 스레드에서 JavaScript 실행
                webView.post {
                    webView.evaluateJavascript("window.onGoogleError('$errorMessage')", null)
                }
            }
        )
        
        // Google 로그인 시작
        googleAuthManager.signIn()
    }

    /**
     * 기존 WebView 뒤로가기 기능 유지
     * JavaScript에서 호출: window.AndroidBridge.goBack()
     */
    @JavascriptInterface
    fun goBack() {
        Log.d("AndroidBridge", "웹에서 뒤로가기 요청")
        webView.post {
            if (webView.canGoBack()) {
                Log.d("AndroidBridge", "WebView 히스토리 뒤로가기")
                webView.goBack()
            } else {
                Log.d("AndroidBridge", "WebView 히스토리가 없음")
                // 필요시 액티비티 종료 또는 다른 처리
            }
        }
    }

    /**
     * FCM 토큰 가져오기 (기존 기능 유지)
     * JavaScript에서 호출: window.AndroidBridge.getFCMToken()
     */
    @JavascriptInterface
    fun getFCMToken(): String? {
        val sharedPref = context.getSharedPreferences("fcm_token", Context.MODE_PRIVATE)
        val token = sharedPref.getString("token", null)
        Log.d("AndroidBridge", "JavaScript에서 FCM 토큰 요청: $token")
        return token
    }

    /**
     * 푸시 알림 처리 (기존 기능 유지)
     */
    @JavascriptInterface
    fun onPushNotificationReceived(notificationData: String) {
        Log.d("AndroidBridge", "웹에서 푸시 알림 데이터 처리 요청: $notificationData")
        // 기존 처리 로직 유지
    }
}