package com.beauty.platform

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.webkit.*
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.beauty.platform.WebAppInterface
import com.beauty.platform.WebPageError

/**
 * 사용자 정의 WebChromeClient
 */
class CustomWebChromeClient(
    private val onProgressChanged: (Int) -> Unit,
    private val onTitleReceived: (String?) -> Unit,
    private val fileChooserLauncher: androidx.activity.result.ActivityResultLauncher<Intent>,
    private val filePathCallback: androidx.compose.runtime.MutableState<ValueCallback<Array<Uri?>>?>
) : WebChromeClient() {
    
    override fun onProgressChanged(view: WebView?, newProgress: Int) {
        onProgressChanged(newProgress)
    }
    
    override fun onReceivedTitle(view: WebView?, title: String?) {
        onTitleReceived(title)
    }
    
    override fun onShowFileChooser(
        webView: WebView?,
        filePathCallbackParameter: ValueCallback<Array<Uri?>>?,
        fileChooserParams: FileChooserParams?
    ): Boolean {
        // 이미 다른 파일 선택 작업이 진행 중이면 무시
        if (filePathCallback.value != null) {
            filePathCallback.value!!.onReceiveValue(null)
            filePathCallback.value = null
        }
        filePathCallback.value = filePathCallbackParameter

        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = fileChooserParams?.acceptTypes?.joinToString(",")?.takeIf { it.isNotBlank() } ?: "*/*"
            if (fileChooserParams?.mode == FileChooserParams.MODE_OPEN_MULTIPLE) {
                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            }
        }

        try {
            fileChooserLauncher.launch(intent)
        } catch (e: Exception) {
            Log.e("CustomWebChromeClient", "Cannot launch file chooser", e)
            filePathCallback.value?.onReceiveValue(null)
            filePathCallback.value = null
            return false
        }
        return true
    }
}

/**
 * 사용자 정의 WebViewClient
 */
class CustomWebViewClient(
    private val context: android.content.Context,
    private val onPageFinished: (Boolean) -> Unit,
    private val onPageError: (WebPageError) -> Unit
) : WebViewClient() {
    
    private fun shouldHandleInWebView(url: String): Boolean {
        // 개발 및 프로덕션 도메인 패턴
        val shouldHandle = url.contains("mimotok.com") || 
               url.contains("localhost") || 
               url.contains("127.0.0.1") || 
               url.contains("10.0.2.2") ||
               url.contains("192.168.0.13") ||
               // ngrok 관련 도메인들
               url.contains("ngrok-free.dev") ||
               url.contains("ngrok-free.app") ||
               url.contains("ngrok.io") ||
               url.contains("ngrok.app") ||
               url.contains(".ngrok.io") ||         // 하위 도메인 포함
               url.contains(".ngrok.app") ||        // 하위 도메인 포함
               url.contains(".ngrok-free.dev") ||   // 하위 도메인 포함
                url.contains("accounts.google.com") ||  // 추가
                url.contains("googleapis.com") ||
                // IP 기반 URL
               url.startsWith("http://localhost") ||
               url.startsWith("http://127.0.0.1") ||
               url.startsWith("http://10.0.2.2") ||
               url.startsWith("https://localhost") ||
               url.startsWith("https://127.0.0.1") ||
               url.startsWith("https://10.0.2.2") ||
               url.contains("http://192.168.0.13")
        
        Log.d("CustomWebViewClient", "shouldHandleInWebView: $url -> $shouldHandle")
        return shouldHandle
    }
    
    
    override fun shouldOverrideUrlLoading(
        view: WebView?,
        request: WebResourceRequest?
    ): Boolean {
        val currentUrl = request?.url?.toString() ?: return false
        
        Log.d("CustomWebViewClient", "URL loading request: $currentUrl")
        
        // 개발 환경 및 프로덕션 도메인은 WebView 내에서 처리
        if (shouldHandleInWebView(currentUrl)) {
            Log.d("CustomWebViewClient", "Allowed domain - handling in WebView")
            return false
        }
        
        // 특정 URL 스킴 처리
        if (currentUrl.startsWith("tel:") || currentUrl.startsWith("mailto:")) {
            Log.d("CustomWebViewClient", "External scheme - opening in external app")
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(currentUrl))
                context.startActivity(intent)
            } catch (e: Exception) {
                Log.e("CustomWebViewClient", "Failed to open external app", e)
            }
            return true
        } else if (currentUrl.startsWith("intent:")) {
            Log.d("CustomWebViewClient", "Intent scheme detected")
            return true
        }
        
        // 외부 URL은 브라우저로 열기
        Log.d("CustomWebViewClient", "External URL - opening in browser")
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(currentUrl))
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("CustomWebViewClient", "Failed to open browser", e)
        }
        return true
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        view?.let {
            onPageFinished(it.canGoBack())
        }
    }

    @Suppress("DEPRECATION")
    override fun onReceivedError(
        view: WebView?,
        errorCode: Int,
        description: String?,
        failingUrl: String?
    ) {
        super.onReceivedError(view, errorCode, description, failingUrl)
        onPageError(WebPageError(errorCode, description.orEmpty(), failingUrl))
    }

    override fun onReceivedError(
        view: WebView?,
        request: WebResourceRequest?,
        error: WebResourceError?
    ) {
        super.onReceivedError(view, request, error)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            onPageError(
                WebPageError(
                    code = error?.errorCode ?: -1,
                    description = error?.description?.toString().orEmpty(),
                    failingUrl = request?.url?.toString()
                )
            )
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ComposeWebView(
    url: String,
    modifier: Modifier = Modifier,
    savedState: Bundle? = null,
    onProgress: (Int) -> Unit = {},
    onError: (WebPageError) -> Unit = {},
    onTitle: (String?) -> Unit = {},
    onCanGoBackChanged: (Boolean) -> Unit = {}, // 이 콜백 유지
    progressBarColor: Color = Color(0xFFFF4F9A),
    progressBarTrackColor: Color = Color(0x33FF4F9A),
    progressBarHeight: Dp = 3.dp,
    showTopProgressBar: Boolean = true,
    onWebViewCreated: (webView: WebView) -> Unit,
    onJsInterfaceReady: (WebAppInterface) -> Unit
) {
    val context = LocalContext.current
    var progressState by remember { mutableIntStateOf(0) } // 내부 프로그레스 상태

    val animatedProgress by animateFloatAsState(
        targetValue = progressState.coerceIn(0, 100) / 100f,
        label = "web_progress_animation"
    )
    // 파일 선택 결과를 처리하기 위한 콜백
    val filePathCallback = remember { mutableStateOf<ValueCallback<Array<Uri?>>?>(null) }

    // ActivityResultLauncher를 사용하여 파일 선택 인텐트의 결과를 받습니다.
    val fileChooserLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (filePathCallback.value == null) return@rememberLauncherForActivityResult

        var uris: Array<Uri?>? = null
        if (result.resultCode == Activity.RESULT_OK) {
            val dataString = result.data?.dataString
            val clipData = result.data?.clipData
            if (clipData != null) { // 여러 파일 선택 (input multiple)
                uris = Array(clipData.itemCount) { i -> clipData.getItemAt(i).uri }
            } else if (dataString != null) { // 단일 파일 선택
                uris = arrayOf(Uri.parse(dataString))
            }
        }
        filePathCallback.value?.onReceiveValue(uris) // 선택된 파일 URI 또는 null을 WebView에 전달
        filePathCallback.value = null // 콜백 참조 해제
    }

    Box(modifier) {
        AndroidView(
            factory = {
                WebView(context).apply {
                    onWebViewCreated(this) // WebView 생성 시 콜백 호출

                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        cacheMode = WebSettings.LOAD_DEFAULT
                        databaseEnabled = true
                        setSupportMultipleWindows(false)
                        allowFileAccess = true // 파일 접근 허용 (중요)
                        allowContentAccess = true // 컨텐츠 접근 허용
                        
                        // 쿠키 설정 - 세션 유지를 위해 필수
                        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

//                        userAgentString = settings.userAgentString + " AppWebViewMobileMimotok" // User Agent 수정 예시
//

                    }
                    
                    // 글로벌 쿠키 설정  
                    val cookieManager = CookieManager.getInstance()
                    cookieManager.setAcceptCookie(true)
                    cookieManager.setAcceptThirdPartyCookies(this, true)
                    cookieManager.flush() // 쿠키 설정을 즉시 적용
                    val ua = settings.userAgentString ?: ""
                    settings.userAgentString = "$ua MyAppWebView/1.0 (Android)"
                    
                    // Android Bridge for Google Authentication
                    val androidBridge = AndroidBridge(context, this)
                    addJavascriptInterface(androidBridge, "AndroidBridge") // "AndroidBridge"라는 이름으로 JS에서 접근 가능
                    
                    // 기존 WebAppInterface도 유지 (FCM 토큰 등을 위해)
                    val webAppInterface = WebAppInterface(context, this) { isLoggedIn ->
                        Log.d("ComposeWebView", "로그인 상태: $isLoggedIn")
                    }
                    addJavascriptInterface(webAppInterface, "Android") // 기존 이름 유지
                    onJsInterfaceReady(webAppInterface)


                    webChromeClient = CustomWebChromeClient(
                        onProgressChanged = { newProgress ->
                            progressState = newProgress
                            onProgress(newProgress)
                        },
                        onTitleReceived = onTitle,
                        fileChooserLauncher = fileChooserLauncher,
                        filePathCallback = filePathCallback
                    )

                    webViewClient = CustomWebViewClient(
                        context = context,
                        onPageFinished = onCanGoBackChanged,
                        onPageError = onError
                    )
//                    webViewClient = WebViewClient()  // 아무 처리 없이 기본
//                    loadUrl("https://ed-unfoliated-nontumultuously.ngrok-free.dev")

                    if (savedState != null) {
                        restoreState(savedState)
                    }
                    // 초기 URL 로드는 AppScreen에서 webViewInstance.loadUrl()을 통해 수행하거나,
                    // onWebViewCreated 콜백 이후 AppScreen에서 명시적으로 호출합니다.
                    // factory에서는 직접 loadUrl(url)을 호출하지 않도록 하여, AppScreen의 로직과 충돌 방지.
                    // 만약 초기 URL을 여기서 설정해야 한다면, onWebViewCreated 콜백 이후 AppScreen에서 로드하는 것이
                    // 상태 관리 측면에서 더 명확합니다.
                }
            },
            update = { view ->
                // AppScreen에서 webViewInstance.loadUrl()로 URL을 직접 제어하므로
                // update 블록에서 URL 변경에 따른 view.loadUrl(url) 호출은 필요하지 않거나
                // 중복될 수 있습니다. AppScreen의 로직을 우선합니다.
                // 만약 이 컴포저블의 'url' prop이 변경되었을 때 특별한 처리가 필요하다면 여기에 작성합니다.
                // 예: if (view.url != url && savedState == null) { view.loadUrl(url) }
                // 하지만 현재 구조에서는 AppScreen에서 currentUrl 변경 시 webViewInstance.loadUrl을 호출하므로
                // 이 부분은 비워두거나 제거해도 됩니다.
            },
            onRelease = {
                it.stopLoading()
                it.webChromeClient = null
                // it.webViewClient = null // WebViewClient는 destroy 시 내부적으로 정리될 수 있음
                it.destroy()
            }
        )

        if (showTopProgressBar && progressState in 1..99) { // progressState 사용
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth()
                    .height(progressBarHeight),
                color = progressBarColor,
                trackColor = progressBarTrackColor
            )
        }
    }
}
