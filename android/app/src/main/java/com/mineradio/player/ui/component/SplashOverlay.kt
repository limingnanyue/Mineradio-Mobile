package com.mineradio.player.ui.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mineradio.player.ui.theme.MineradioColors

/**
 * 启动遮罩 —— 复刻桌面版 #splash（index.html:1888-1901, 26058 dismissSplash）。
 *
 * 桌面版：全屏深底 + MINERADIO 字标 + signal-line + "private visual radio" 副标 +
 * "点击进入"提示（呼吸闪烁）。用户点击任意位置 dismissSplash() 进入主界面。
 *
 * 移动端适配：用 Canvas 绘制信号线动画，字标用 Text，"点击进入"用闪烁 alpha。
 */
@Composable
fun SplashOverlay(
    onEnter: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // "点击进入" 呼吸动画
    val transition = rememberInfiniteTransition(label = "splash")
    val blinkAlpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "blink",
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF050608), Color(0xFF0A0C10)),
                )
            )
            .clickable(onClick = onEnter),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // 字标 MINERADIO
            Row {
                Text("Mine", color = MineradioColors.FcInk, fontSize = 32.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                Text("radio", color = MineradioColors.FcAccent, fontSize = 32.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
            }
            Spacer(Modifier.height(12.dp))
            // signal-line（青色细条）
            Box(
                Modifier
                    .width(120.dp)
                    .height(2.dp)
                    .clip(RoundedCornerShape(50))
                    .background(MineradioColors.FcAccent),
            )
            Spacer(Modifier.height(10.dp))
            Text("private visual radio", color = MineradioColors.FcMuted, fontSize = 11.sp, letterSpacing = 2.sp)
            Spacer(Modifier.height(48.dp))
            // 点击进入（闪烁）
            Text(
                "点击进入",
                color = MineradioColors.FcInk2,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.alpha(blinkAlpha),
            )
        }
    }
}
