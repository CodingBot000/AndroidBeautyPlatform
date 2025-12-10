package com.beauty.platform


import android.app.Activity
import android.content.Context
import android.net.Uri
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import com.beauty.platform.component.NestedScrollWebView
import android.widget.Toast
import android.os.Handler
import android.os.Looper
import org.json.JSONObject

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

    // JavaScript에서 이 이름을 사용하여 메서드를 호출합니다. (예: Android.onLoginResult(...))
    @JavascriptInterface
    fun onLoginResult(jsonResult: String) {
        // 백그라운드 스레드에서 호출될 수 있으므로, 여기서 바로 UI를 건드리면 안 됩니다.
        // 콜백을 통해 결과를 전달하여 메인 스레드에서 처리하도록 합니다.
        try {
            val jsonObject = JSONObject(jsonResult)
            val isLoggedIn = jsonObject.optBoolean("isLoggedIn", false)
            onCheckLoginResult(isLoggedIn)
        } catch (e: Exception) {
            onCheckLoginResult(false) // 파싱 실패 시 로그인 안 된 것으로 간주
        }
    }

    @JavascriptInterface
    fun getFCMToken(): String? {
        val sharedPref = context.getSharedPreferences("fcm_token", Context.MODE_PRIVATE)
        val token = sharedPref.getString("token", null)
        Log.d("WebAppInterface", "JavaScript에서 FCM 토큰 요청: $token")
        return token
    }

    @JavascriptInterface
    fun sendTokenToServer(token: String, userId: String? = null) {
        Log.d("WebAppInterface", "웹에서 토큰 서버 전송 요청: token=$token, userId=$userId")
        // 여기서 실제 서버 전송 로직을 구현하거나, 
        // MainActivity의 콜백으로 전달하여 처리할 수 있습니다.
    }

    @JavascriptInterface
    fun onPushNotificationReceived(notificationData: String) {
        Log.d("WebAppInterface", "웹에서 푸시 알림 데이터 처리 요청: $notificationData")
        try {
            val jsonObject = JSONObject(notificationData)
            // 웹에서 전달받은 알림 데이터를 처리
            val title = jsonObject.optString("title", "")
            val body = jsonObject.optString("body", "")
            val url = jsonObject.optString("url", "")
            
            // 필요한 처리 로직 구현
        } catch (e: Exception) {
            Log.e("WebAppInterface", "푸시 알림 데이터 파싱 실패", e)
        }
    }


    @JavascriptInterface
    fun goBack() {
        Log.d("WebAppInterface", "웹에서 뒤로가기 요청")
        // UI 스레드에서 실행해야 함
        (context as? Activity)?.runOnUiThread {
            if (webView.canGoBack()) {
                Log.d("WebAppInterface", "WebView 히스토리 뒤로가기")
                webView.goBack()
            } else {
                Log.d("WebAppInterface", "WebView 히스토리가 없음, 액티비티 종료")
                // WebView history가 없으면 액티비티 종료
                (context as? Activity)?.finish()
            }
        }
    }

    @JavascriptInterface
    fun onLoadComplete() {
        Log.d("WebAppInterface", "페이지 모든 리소스 로드 완료")
        // JS에서 호출되므로 메인 스레드로 전환
        mainHandler.post {
            onPageFullyLoaded?.invoke()
        }
    }

    companion object {
        const val INTERFACE_NAME = "Android"
    }
}
