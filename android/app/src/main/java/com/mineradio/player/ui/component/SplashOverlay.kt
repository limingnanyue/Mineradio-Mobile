package com.mineradio.player.ui.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mineradio.player.ui.theme.MineradioColors
import kotlin.math.cos
import kotlin.math.sin

/**
 * 启动遮罩 —— 复刻桌面版 #splash（index.html:1888-1901, 26058 dismissSplash）。
 *
 * 桌面版：全屏深底 + MINERADIO 字标 + signal-line + "private visual radio" 副标 +
 * "点击进入"提示（呼吸闪烁）。用户点击任意位置 dismissSplash() 进入主界面。
 *
 * 移动端适配：用 Canvas 绘制粒子信号线动画（从中心向外辐射的青色射线 + 旋转扫描线），
 * 字标用 Text，"点击进入"用闪烁 alpha。
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
    // 信号线旋转角（0..360 循环）
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "rotation",
    )
    // 信号脉冲（0..1 周期性扩散）
    val pulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "pulse",
    )

    // 12 条信号线的固定角度偏移（每 30°）
    val lineAngles = remember { List(12) { it * 30f } }

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
        // 1. 粒子信号线层（中心向外辐射 + 旋转扫描）
        Canvas(Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val maxR = minOf(size.width, size.height) * 0.46f
            val accent = MineradioColors.FcAccent

            // 1a. 静态辐射信号线（12 条，长度随 pulse 呼吸）
            lineAngles.forEach { angleDeg ->
                val rad = Math.toRadians(angleDeg.toDouble())
                val len = maxR * (0.55f + 0.45f * pulse)
                val end = Offset(
                    (center.x + cos(rad) * len).toFloat(),
                    (center.y + sin(rad) * len).toFloat(),
                )
                drawLine(
                    color = accent.copy(alpha = 0.10f + 0.18f * (1f - pulse)),
                    start = center,
                    end = end,
                    strokeWidth = 1f,
                    cap = StrokeCap.Round,
                )
            }

            // 1b. 旋转扫描线（一条高亮长射线）
            val sweepRad = Math.toRadians(rotation.toDouble())
            val sweepEnd = Offset(
                (center.x + cos(sweepRad) * maxR).toFloat(),
                (center.y + sin(sweepRad) * maxR).toFloat(),
            )
            drawLine(
                color = accent.copy(alpha = 0.55f),
                start = center,
                end = sweepEnd,
                strokeWidth = 1.6f,
                cap = StrokeCap.Round,
            )

            // 1c. 中心脉冲圆环（半径随 pulse 扩散，alpha 随之衰减）
            val ringR = maxR * 0.18f + maxR * 0.32f * pulse
            drawCircle(
                color = accent.copy(alpha = 0.35f * (1f - pulse)),
                radius = ringR,
                center = center,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.4f),
            )
            // 中心实心点
            drawCircle(
                color = accent.copy(alpha = 0.85f),
                radius = 3f,
                center = center,
            )
        }

        // 2. 字标 + 副标（置于 Canvas 之上）
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
