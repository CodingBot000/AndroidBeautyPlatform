package com.beauty.platform

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun WebViewContainer(
    url: String,
    modifier: Modifier = Modifier,
    indicatorHeight: Dp = 3.dp,
    indicatorColor: Color = Color(0xFFFF4F9A),
    indicatorTrack: Color = Color(0x33FF4F9A),
) {
    var progress by remember { mutableStateOf(0) }
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0, 100) / 100f,
        label = "web_progress_anim"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White) // 전체 화면(시스템바 아래까지) 하얀 배경
    ) {
        // 상단 분홍색 진행 막대 (컨테이너가 그린다)
        if (progress in 1..99) {
            LinearProgressIndicator(
                progress = { animated },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .fillMaxWidth()
                    .height(indicatorHeight),
                color = indicatorColor,
                trackColor = indicatorTrack
            )
        }

        // WebView 컨텐츠: 위는 인디케이터 높이만큼, 아래는 내비게이션 바 인셋만큼 패딩
//        ComposeWebView(
//            url = url,
//            modifier = Modifier
//                .windowInsetsPadding(WindowInsets.statusBars)
//                .padding(top = indicatorHeight)                // 상단 인디케이터 만큼
//                .windowInsetsPadding(WindowInsets.navigationBars), // 하단 시스템 내비게이션 바 만큼
//            onProgress = { progress = it },
//            showTopProgressBar = false // 내부 진행바는 끄고 컨테이너에서만 표시
//        )
    }
}
