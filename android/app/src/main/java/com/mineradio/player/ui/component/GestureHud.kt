package com.mineradio.player.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mineradio.player.ui.theme.MineradioColors

/**
 * 手势 HUD —— 1:1 复刻桌面版 #gesture-hud。
 *
 * 桌面版来源：public/index.html:2326-2331
 *   - 手势标签（待命/手掌/捏合/握拳）
 *   - 确认提示（将手放进摄像头视野）
 *   - 手势进度条
 *   - 手势图例（手掌推开粒子 · 捏合旋转 · 握拳收束）
 *
 * 移动端适配：视觉占位完整还原；实际手势识别需摄像头权限 + ML Kit 手势识别，
 * 当前作为视觉占位与未来手势交互入口保留。
 */
@Composable
fun GestureHud(
    visible: Boolean,
    gestureLabel: String,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut(), modifier = modifier) {
        Column(
            Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF0A0A10).copy(alpha = 0.85f))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("手势：", color = MineradioColors.FcMuted, fontSize = 12.sp)
                Text(gestureLabel, color = MineradioColors.FcAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Text("将手放进摄像头视野", color = MineradioColors.Champagne, fontSize = 11.sp)
            // gesture-meter 进度条（占位 30%）
            LinearProgressIndicator(
                progress = { 0.3f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = MineradioColors.FcAccent,
                trackColor = MineradioColors.GlassDark,
            )
            Text(
                "手掌推开粒子 · 捏合旋转 · 握拳收束",
                color = MineradioColors.FcMuted,
                fontSize = 10.sp,
            )
        }
    }
}
