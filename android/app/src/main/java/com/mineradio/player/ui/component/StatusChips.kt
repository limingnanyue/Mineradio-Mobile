package com.mineradio.player.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mineradio.player.ui.theme.MineradioColors

private val BeatGreen = Color(0xFF7EE2A8)

/**
 * 节奏分析状态角标 —— 1:1 复刻桌面版 #beat-chip。
 *
 * 桌面版来源：public/index.html:2320 + 11968 showBeatChip(text)
 *   - 分析节奏时显示绿色旋转指示 + 文案
 *   - 分析完成后隐藏
 *
 * 样式：深色背景 + 绿色边框/文字，右上角悬浮。
 */
@Composable
fun BeatChip(
    text: String?,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(visible = !text.isNullOrEmpty(), enter = fadeIn(), exit = fadeOut(), modifier = modifier) {
        Row(
            Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF0A0A10).copy(alpha = 0.85f))
                .border(1.dp, BeatGreen.copy(alpha = 0.30f), RoundedCornerShape(14.dp))
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(11.dp),
                strokeWidth = 1.5.dp,
                color = BeatGreen,
                trackColor = BeatGreen.copy(alpha = 0.25f),
            )
            Text(
                text ?: "分析节奏…",
                color = BeatGreen,
                fontSize = 11.sp,
                letterSpacing = 0.4.sp,
            )
        }
    }
}

/**
 * AI 深度估计状态角标 —— 1:1 复刻桌面版 #ai-depth-chip。
 *
 * 桌面版来源：public/index.html:2317
 *   - AI 深度估计管线运行时显示香槟色旋转指示 + 文案
 *   - 完成后隐藏
 *
 * 样式：深色背景 + 香槟色边框/文字，右上角悬浮（位于 beat-chip 上方）。
 */
@Composable
fun AiDepthChip(
    text: String?,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(visible = !text.isNullOrEmpty(), enter = fadeIn(), exit = fadeOut(), modifier = modifier) {
        Row(
            Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF0A0A10).copy(alpha = 0.85f))
                .border(1.dp, MineradioColors.Champagne.copy(alpha = 0.32f), RoundedCornerShape(14.dp))
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(11.dp),
                strokeWidth = 1.5.dp,
                color = MineradioColors.Champagne,
                trackColor = MineradioColors.Champagne.copy(alpha = 0.24f),
            )
            Text(
                text ?: "AI 深度估计…",
                color = MineradioColors.Champagne,
                fontSize = 11.sp,
                letterSpacing = 0.4.sp,
            )
        }
    }
}
