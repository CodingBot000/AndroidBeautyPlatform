package com.beauty.platform

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.webkit.WebView
import com.beauty.platform.component.NestedScrollWebView
import android.widget.Toast
import java.net.URI
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessaging
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

import com.beauty.platform.ui.theme.MyApplicationTheme

import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.ime
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Alignment
import kotlinx.coroutines.delay
import com.beauty.platform.component.SplashOverlay
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.beauty.platform.utils.SPLASH_TIMEOUT
import com.beauty.platform.utils.urlManager.isHomeUrl

//startUrl
//private val startUrl = "https://www.mimotok.com"
// startUrl
//private val startUrl = "http://10.0.2.2:3000"
//// macIpUrl
//private val macIpUrl = "http://192.168.0.13:3000"
////ngrogUrl
private val startUrl = "https://ed-unfoliated-nontumultuously.ngrok-free.dev"
class MainActivity : ComponentActivity() {

    private var webViewInstance: NestedScrollWebView? = null
    private var backPressedTime: Long = 0
    private var backPressToast: Toast? = null
    
    // 스플래시 상태 관리
    var showSplash by mutableStateOf(true)
    private var splashDismissed = false

    companion object {
        private const val BACK_PRESS_TIMEOUT = 2000L

    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Log.d("FCM", "알림 권한이 허용되었습니다.")
            setupFCM()
        } else {
            Log.d("FCM", "알림 권한이 거부되었습니다.")
        }
    }

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        // Android 12+ 기본 스플래시 스크린 즉시 종료
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { false }
        
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        requestNotificationPermission()
        handleNotificationIntent(intent)
        handleDeepLinkIntent(intent)

        // 라이트 상태바/내비게이션바 아이콘 (흰 배경일 때 아이콘이 어둡게)
//        WindowCompat.getInsetsController(window, window.decorView).apply {
//            isAppearanceLightStatusBars = true
//            isAppearanceLightNavigationBars = true
//        }

        setContent {
            MyApplicationTheme {
                // 시스템 인셋을 자동으로 패딩하지 않게(전체 하얀 배경이 바 아래까지 깔리도록)
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
//                    contentWindowInsets = WindowInsets(0)
                ) { innerPadding ->
                    AppScreen(mainActivity = this@MainActivity)
//                    WebViewContainer(
//                        url = "https://www.mimotok.com",
//                        modifier = Modifier.padding(innerPadding)
//                    )
//
//                    AppEntry()
                }
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    setupFCM()
                }
                else -> {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            setupFCM()
        }
    }

    private fun setupFCM() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("FCM", "FCM 토큰 가져오기 실패", task.exception)
                return@addOnCompleteListener
            }

            val token = task.result
            Log.d("FCM", "FCM 토큰: $token")
            
            // 토큰을 SharedPreferences에 저장
            saveFCMToken(token)
        }
    }

    private fun saveFCMToken(token: String) {
        val sharedPref = getSharedPreferences("fcm_token", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString("token", token)
            putLong("token_timestamp", System.currentTimeMillis())
            apply()
        }
    }

    fun getFCMToken(): String? {
        val sharedPref = getSharedPreferences("fcm_token", Context.MODE_PRIVATE)
        return sharedPref.getString("token", null)
    }

    private fun handleNotificationIntent(intent: android.content.Intent?) {
        intent?.extras?.let { extras ->
            Log.d("FCM", "알림을 통해 앱이 실행되었습니다: $extras")
            // 필요에 따라 특정 페이지로 이동하거나 특별한 처리를 수행
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDeepLinkIntent(intent)
    }

    private fun handleDeepLinkIntent(intent: Intent?) {
        intent?.data?.let { uri ->
            Log.d("DeepLink", "Deep link received: $uri")
            if (uri.path?.startsWith("/api/auth/google/callback") == true) {
                Log.d("GoogleLogin", "Google 로그인 콜백 URL: $uri")
                webViewInstance?.loadUrl(uri.toString())
            }
        }
    }

    fun setWebViewInstance(webView: NestedScrollWebView) {
        this.webViewInstance = webView
    }

    // 스플래시 dismiss 함수 (중복 호출 방지)
    @Synchronized
    fun dismissSplash() {
        if (splashDismissed) return
        splashDismissed = true
        showSplash = false
    }


    private fun handleHomeBackPress() {
        val currentTime = System.currentTimeMillis()
        
        if (currentTime - backPressedTime < BACK_PRESS_TIMEOUT) {
            backPressToast?.cancel()
            finishAffinity()
        } else {
            backPressedTime = currentTime
            showExitToast()
        }
    }

    private fun showExitToast() {
        backPressToast?.cancel()
        backPressToast = Toast.makeText(
            this,
            getString(R.string.back_press_exit_message),
            Toast.LENGTH_SHORT
        ).also { it.show() }
    }

    private fun navigateToHome() {
        webViewInstance?.let { webView ->
            val currentUrl = webView.url ?: startUrl
            val homeUrl = extractLocaleAndCreateHomeUrl(currentUrl)
            webView.loadUrl(homeUrl)
        }
    }

    private fun extractLocaleAndCreateHomeUrl(currentUrl: String): String {
        return try {
            val uri = URI(currentUrl)
            val path = uri.path ?: ""
            
            when {
                path.startsWith("/ko") -> "$startUrl/ko"
                path.startsWith("/en") -> "$startUrl/en"
                path.startsWith("/ja") -> "$startUrl/ja"
                path.startsWith("/zh-CN") -> "$startUrl/zh-CN"
                path.startsWith("/zh-TW") -> "$startUrl/zh-TW"
                else -> startUrl
            }
        } catch (e: Exception) {
            startUrl
        }
    }

    fun handleBackPress() {
        val currentUrl = webViewInstance?.url ?: startUrl
        
        when {
            isHomeUrl(currentUrl) -> handleHomeBackPress()
            webViewInstance?.canGoBack() == true -> webViewInstance?.goBack()
            else -> navigateToHome()
        }
    }

    override fun onDestroy() {
        backPressToast?.cancel()
        super.onDestroy()
    }
}

data class BottomNavItem(
    val label: String,
    val icon: ImageVector,
    val route: String // 예시로 각 아이템에 대한 라우트(경로)를 추가할 수 있습니다.
)
// ComposeWebView에서 사용하던 WebPageError 데이터 클래스 (필요하면 별도 파일로 이동)
data class WebPageError(val code: Int, val description: String, val failingUrl: String?)

@Composable
fun AppScreen(mainActivity: MainActivity) {
    var selectedItemIndex by remember { mutableIntStateOf(0) }
//    val items = listOf(
//        BottomNavItem("Home", Icons.Filled.Home, "https://www.mimotok.com/home"),
//        BottomNavItem("Treatment-Info", Icons.Filled.Info, "https://www.mimotok.com/treatments_info"),
//        BottomNavItem("community", Icons.Filled.ThumbUp, "https://www.mimotok.com/community"),
//        BottomNavItem("List", Icons.Filled.Menu, "https://www.mimotok.com/hospital"),
//        BottomNavItem("Diagnosis", Icons.Filled.Build, "https://www.mimotok.com/recommend_estimate"),
//        BottomNavItem("MyPage", Icons.Filled.Person, "https://www.mimotok.com/user/my-page")
//    )


    var webViewInstance by remember { mutableStateOf<NestedScrollWebView?>(null) }
    var currentUrl by rememberSaveable { mutableStateOf(startUrl) }
    var canGoBack by remember { mutableStateOf(false) } // WebView 뒤로가기 가능 여부 상태

    // WebView의 로딩 진행 상태
    var webViewProgress by remember { mutableIntStateOf(0) }

    // 3초 타임아웃 처리
    LaunchedEffect(Unit) {
        delay(SPLASH_TIMEOUT)
        mainActivity.dismissSplash()
    }

    // BackHandler 로직 - 커스텀 뒤로가기 처리
    BackHandler(enabled = true) {
        mainActivity.handleBackPress()
    }
    // 시스템 인셋을 자동으로 패딩하지 않게(전체 하얀 배경이 바 아래까지 깔리도록)
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        // contentWindowInsets = WindowInsets(0) // Scaffold가 자동으로 패딩을 관리하므로 주석 처리하거나 필요에 맞게 조정
//        bottomBar = {
//            NavigationBar {
//                items.forEachIndexed { index, item ->
//                    NavigationBarItem(
//                        selected = selectedItemIndex == index,
//                        onClick = {
//                            if (selectedItemIndex != index) { // 현재 선택된 아이템과 다른 아이템을 클릭했을 때만 URL 로드
//                                selectedItemIndex = index
//                                currentUrl = item.route
//                                webViewInstance?.loadUrl(item.route)
//                            }
//                        },
//                        label = { Text(item.label) },
//                        icon = { Icon(item.icon, contentDescription = item.label) }
//                    )
//                }
//            }
//        }
    ) { innerPadding ->
        // AppEntry 또는 WebViewContainer를 innerPadding을 적용하여 배치합니다.
        // 현재 로직에서는 AppEntry가 WebViewContainer 또는 DeniedScreen을 표시하므로 AppEntry를 사용합니다.
        Box(modifier = Modifier.padding(innerPadding)) {
            // 메인 컨텐츠 (뒤에서 로딩)
            Column {
            AppEntry(
                initialUrl = currentUrl,
                onWebViewReady = { webView ->
                    webViewInstance = webView
                    // MainActivity에 WebView 인스턴스 등록 (Deep Link 처리용)
                    mainActivity.setWebViewInstance(webView)
                    // Debug 모드에서만 WebView 디버깅 활성화  
                    // WebView.setWebContentsDebuggingEnabled(false) // 프레임 레이트 로그 줄이기 위해 비활성화
                    // 초기 URL 로드 (AppEntry에서 ComposeWebView가 생성된 후)
                    if (webView.url != currentUrl) {
                        webView.loadUrl(currentUrl)
                    }
                    // canGoBack 초기 상태 업데이트
                    canGoBack = webView.canGoBack()
                },
                // ComposeWebView에서 사용하던 콜백들을 AppEntry를 통해 전달받도록 함
                onProgressChanged = { progress ->
                    webViewProgress = progress // 진행 상태 업데이트
                },
                onPageError = { error ->
                    Log.e("AppScreenWebViewError", "Error: ${error.description} at ${error.failingUrl}")
                    // 여기에 에러 발생 시 사용자에게 보여줄 UI 처리 등을 추가할 수 있습니다.
                },
                onPageTitleChanged = { title ->
                    Log.d("AppScreenWebViewTitle", "Title: $title")
                    // 페이지 제목이 변경될 때 필요한 로직 추가 (예: 액션바 제목 변경)
                },
                onCanGoBackStateChanged = { canGo -> // canGoBack 상태 업데이트 콜백
                    canGoBack = canGo
                },
                onHomePageLoaded = { // 홈페이지 로드 완료 콜백 추가
                    mainActivity.dismissSplash()
                }
            )
            }
            
            // 스플래시 오버레이 (위에 덮음)
            SplashOverlay(visible = mainActivity.showSplash)
        }
    }
}

// MainActivity.kt (Compose setContent 내부)
//@Composable
//fun AppEntry(
//    initialUrl: String,
//    onWebViewReady: (WebView) -> Unit,
//    onProgressChanged: (Int) -> Unit, // 추가된 콜백
//    onPageError: (WebPageError) -> Unit, // 추가된 콜백
//    onPageTitleChanged: (String?) -> Unit, // 추가된 콜백
//    onCanGoBackStateChanged: (Boolean) -> Unit // 추가된 콜백
//) {
//
//    // 권한 상태를 나타내는 열거형 또는 sealed class 사용 가능
//    enum class PermissionStatus {
//        LOADING, // 초기 상태 또는 권한 확인 중
//        GRANTED,
//        DENIED_TEMPORARILY,
//        DENIED_PERMANENTLY
//    }
//    var ready by remember { mutableStateOf(false) }
//    var permanentlyDenied by remember { mutableStateOf(false) }
//
//    // 시작 시 권한 요청
//    CameraGalleryPermissionRequester(
//        onAllGranted = { ready = true; permanentlyDenied = false },
//        onDenied = { _, permDenied -> ready = false; permanentlyDenied = permDenied }
//    )
//
//    when {
//        ready -> {
//            // 권한 OK: 실제 화면
////            WebViewContainer(url = "https://www.mimotok.com")
//            ComposeWebView(
//                url = initialUrl,
//                modifier = Modifier.fillMaxSize(),
//                onWebViewCreated = { webView ->
//                    onWebViewReady(webView) // 생성된 WebView 인스턴스를 AppScreen으로 전달
//                },
//                // ComposeWebView의 콜백들을 AppScreen에서 받은 콜백으로 연결
//                onProgress = onProgressChanged,
//                onError = onPageError,
//                onTitle = onPageTitleChanged,
//                onCanGoBackChanged = onCanGoBackStateChanged, // 이 콜백 연결
//                // progressBar 관련 설정은 필요에 따라 ComposeWebView에 직접 전달하거나,
//                // AppScreen에서 상태로 관리하여 전달할 수 있습니다.
//                // 여기서는 ComposeWebView의 기본값을 사용한다고 가정합니다.
//                showTopProgressBar = true // 프로그레스바 표시 여부 (예시)
//            )
//        }
//        permanentlyDenied -> {
//            // 영구 거부: 설정으로 유도
//            DeniedScreen(
//                onOpenSettings = { PermissionController.current?.openAppSettings() },
//                onRetry = { PermissionController.current?.request() }
//            )
//        }
//        else -> {
//            // 일시 거부: 다시 요청 버튼 제공
//            DeniedScreen(
//                onOpenSettings = { PermissionController.current?.openAppSettings() },
//                onRetry = { PermissionController.current?.request() }
//            )
//        }
//    }
//}
//
@Composable
fun DeniedScreen(
    onOpenSettings: () -> Unit,
    onRetry: () -> Unit
) {
    // 원하는 UI로 바꾸세요 (간단 예시)
    Column(
        modifier = androidx.compose.ui.Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
    ) {
        androidx.compose.material3.Text(
            "카메라/갤러리 접근 권한이 필요합니다.",
            style = androidx.compose.material3.MaterialTheme.typography.titleMedium
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRetry) { androidx.compose.material3.Text("다시 요청") }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onOpenSettings) { androidx.compose.material3.Text("앱 설정 열기") }
    }
}

enum class PermissionStatus {
    LOADING, // 초기 상태 또는 권한 확인 중
    GRANTED,
    DENIED_TEMPORARILY,
    DENIED_PERMANENTLY
}
@Composable
fun AppEntry(
    initialUrl: String,
    onWebViewReady: (NestedScrollWebView) -> Unit,
    onProgressChanged: (Int) -> Unit,
    onPageError: (WebPageError) -> Unit,
    onPageTitleChanged: (String?) -> Unit,
    onCanGoBackStateChanged: (Boolean) -> Unit,
    onHomePageLoaded: () -> Unit = {} // 홈페이지 로드 완료 콜백 추가
) {
    // 권한 상태를 나타내는 열거형 또는 sealed class 사용 가능


    var permissionStatus by remember { mutableStateOf(PermissionStatus.LOADING) }

    // CameraGalleryPermissionRequester는 LaunchedEffect 등을 통해 권한을 요청하고
    // 그 결과에 따라 permissionStatus를 업데이트합니다.
    CameraGalleryPermissionRequester(
        onAllGranted = { permissionStatus = PermissionStatus.GRANTED },
        onDenied = { _, permDenied ->
            permissionStatus = if (permDenied) {
                PermissionStatus.DENIED_PERMANENTLY
            } else {
                PermissionStatus.DENIED_TEMPORARILY
            }
        }
    )

    when (permissionStatus) {
        PermissionStatus.LOADING -> {
            // 권한 상태 확인 중: 로딩 인디케이터 또는 빈 화면 표시
            // 예: 로딩 인디케이터
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                // CircularProgressIndicator() // 머티리얼3 사용 시
                // androidx.compose.material.CircularProgressIndicator() // 머티리얼 사용 시
            }
            // 또는 아예 아무것도 표시하지 않음 (잠깐의 빈 화면)
            // Spacer(modifier = Modifier.fillMaxSize())
        }
        PermissionStatus.GRANTED -> {
            ComposeWebView(
                url = initialUrl,
                modifier = Modifier.fillMaxSize(),
                onWebViewCreated = { webView ->
                    onWebViewReady(webView)
                },
                onProgress = onProgressChanged,
                onError = onPageError,
                onTitle = onPageTitleChanged,
                onCanGoBackChanged = onCanGoBackStateChanged,
                showTopProgressBar = true,
                onJsInterfaceReady = {

                },
                onHomePageLoaded = onHomePageLoaded
            )
        }
        PermissionStatus.DENIED_PERMANENTLY -> {
            DeniedScreen(
                onOpenSettings = { PermissionController.current?.openAppSettings() },
                onRetry = {
                    permissionStatus = PermissionStatus.LOADING // 재시도 시 다시 로딩 상태로 변경
                    PermissionController.current?.request()
                }
            )
        }
        PermissionStatus.DENIED_TEMPORARILY -> {
            DeniedScreen(
                onOpenSettings = { PermissionController.current?.openAppSettings() }, // 이 버튼이 여기도 필요한지?
                onRetry = {
                    permissionStatus = PermissionStatus.LOADING // 재시도 시 다시 로딩 상태로 변경
                    PermissionController.current?.request()
                }
            )
        }
    }
}