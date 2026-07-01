package com.mineradio.player.ui.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mineradio.player.ui.theme.MineradioColors

/**
 * 全屏加载浮层 —— 复刻桌面版 #loading-overlay（含 spinner）。
 *
 * 桌面版来源：public/index.html:2465 `<div id="loading-overlay"><div class="spinner"></div></div>`
 * 桌面版在 fetchMusicSearchResults / loadHomeDiscover / refreshLoginStatus 等场景调用 showLoading()。
 *
 * 移动端用 Compose 旋转渐变弧 + 文案实现等价视觉。
 */
@Composable
fun LoadingOverlay(
    text: String = "加载中…",
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "loading")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "loading-rotation",
    )
    Box(
        modifier
            .fillMaxSize()
            .background(Color(0xCC000000)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // spinner：旋转的渐变圆环（对应桌面版 .spinner）
            Box(
                Modifier
                    .size(44.dp)
                    .rotate(rotation)
                    .clip(RoundedCornerShape(50))
                    .background(
                        Brush.sweepGradient(
                            listOf(
                                Color.Transparent,
                                MineradioColors.FcAccent,
                                MineradioColors.FcAccent,
                            ),
                        ),
                    ),
            )
            Text(
                text,
                color = MineradioColors.FcInk2,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}
