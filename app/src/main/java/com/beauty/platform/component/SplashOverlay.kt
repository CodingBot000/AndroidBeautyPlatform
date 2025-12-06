package com.beauty.platform.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.beauty.platform.R
import com.beauty.platform.utils.LOTTIE_ANIMATION_SPEED

@Composable
fun SplashOverlay(
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        exit = fadeOut(animationSpec = tween(300)),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            // Lottie 애니메이션 로드
            val composition = rememberLottieComposition(
                LottieCompositionSpec.RawRes(R.raw.loading_logo)
            )
            
            // 애니메이션 진행 상태
            val progress = animateLottieCompositionAsState(
                composition = composition.value,
                iterations = LottieConstants.IterateForever, // 무한 반복
                speed = LOTTIE_ANIMATION_SPEED
            )
            
            LottieAnimation(
                composition = composition.value,
                progress = { progress.value },
                modifier = Modifier.size(200.dp)
            )
        }
    }
}