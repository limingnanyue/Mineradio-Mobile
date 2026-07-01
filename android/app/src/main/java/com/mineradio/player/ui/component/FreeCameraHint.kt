package com.mineradio.player.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mineradio.player.ui.theme.MineradioColors

/**
 * 自由镜头提示 —— 1:1 复刻桌面版 #free-camera-hint。
 *
 * 桌面版来源：public/index.html:2464
 *   "自由镜头 R 固定/退出 · WASD 移动 · 鼠标转向 · Shift 加速 · Space/Ctrl 升降 · Q/E 轻微旋转 · 滚轮景深 · K 回正"
 *
 * 移动端适配：键盘操作映射为触屏手势说明，保持视觉一致。
 */
@Composable
fun FreeCameraHint(
    visible: Boolean,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut(), modifier = modifier) {
        Box(
            Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF0A0A10).copy(alpha = 0.85f))
                .padding(horizontal = 14.dp, vertical = 8.dp),
        ) {
            Text(
                buildString {
                    append("自由镜头 ")
                    append("双指缩放景深 · 单指拖动转向 · 双指平移移动 · 长按回正")
                },
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal,
            )
        }
    }
}
