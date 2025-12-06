package com.beauty.platform


import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker

/** SDK 버전에 맞춰 필요한 권한 배열 반환 */
fun requiredCameraGalleryPermissions(): Array<String> {
    val camera = android.Manifest.permission.CAMERA
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(camera, android.Manifest.permission.READ_MEDIA_IMAGES)
    } else {
        arrayOf(camera, android.Manifest.permission.READ_EXTERNAL_STORAGE)
    }
}

/** 모든 권한이 허용되었는지 체크 */
fun allGranted(permissions: Array<String>, check: (String) -> Int): Boolean =
    permissions.all { check(it) == PermissionChecker.PERMISSION_GRANTED }

/**
 * 앱 시작 시 권한 요청 컴포저블
 *
 * @param onAllGranted  모든 권한 허용 시 콜백
 * @param onDenied      하나라도 거부 시 콜백(일부/전체 거부 포함)
 * @param autoRequest   최초 컴포지션 시 자동 요청
 */
@Composable
fun CameraGalleryPermissionRequester(
    onAllGranted: () -> Unit,
    onDenied: (denied: List<String>, permanentlyDenied: Boolean) -> Unit,
    autoRequest: Boolean = true,
    onRequestingChange: (Boolean) -> Unit = {}   // ⬅️ 추가
) {
    val context = LocalContext.current
    val permissions = remember { requiredCameraGalleryPermissions() }
    var launched by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        onRequestingChange(false)                 // ⬅️ 요청 끝
        val denied = result.filterValues { !it }.keys.toList()
        if (denied.isEmpty()) {
            onAllGranted()
        } else {
            val permanentlyDenied = denied.any { p ->
                val activity = context as? Activity
                activity != null && !ActivityCompat.shouldShowRequestPermissionRationale(activity, p)
            }
            onDenied(denied, permanentlyDenied)
        }
    }

    LaunchedEffect(Unit) {
        if (!autoRequest || launched) return@LaunchedEffect
        launched = true

        val granted = allGranted(permissions) {
            ContextCompat.checkSelfPermission(context, it)
        }
        if (granted) {
            onAllGranted()
        } else {
            onRequestingChange(true)              // ⬅️ 요청 시작
            launcher.launch(permissions)
        }
    }

    PermissionController.current = object : PermissionController.Controller {
        override fun request() {
            onRequestingChange(true)              // ⬅️ 다시 요청 시작
            launcher.launch(permissions)
        }
        override fun openAppSettings() { /* 동일 */ }
    }
}

/** 전역 접근용 컨트롤러 (재요청/설정화면 이동) */
object PermissionController {
    interface Controller {
        fun request()
        fun openAppSettings()
    }
    var current: Controller? by mutableStateOf(null)
}
