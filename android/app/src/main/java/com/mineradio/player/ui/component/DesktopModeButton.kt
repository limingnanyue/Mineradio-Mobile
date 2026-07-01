package com.mineradio.player.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mineradio.player.ui.theme.MineradioColors

/**
 * 桌面模式按钮 —— 复刻桌面版 .desktop-mode-btn（index.html:55-59）。
 * 胶囊形，30px 高，min-width 78px，左侧小圆点指示 .on 状态（红色 #ff5367）。
 * 用于 DIY 浮区按钮与标题栏 DIY 按钮。
 */
@Composable
fun DesktopModeButton(
    text: String,
    isOn: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scale by animateFloatAsState(
        targetValue = if (isOn) 1f else 0.96f,
        animationSpec = tween(340),
        label = "diyBtnScale",
    )
    val shape = RoundedCornerShape(50)
    Box(
        modifier = modifier
            .height(30.dp)
            .clip(shape)
            .background(Color(0x1A000000))
            .border(1.dp, if (isOn) MineradioColors.FcAccent.copy(alpha = 0.5f) else Color(0x29FFFFFF), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp)
            .scale(scale),
        contentAlignment = Alignment.Center,
    ) {
        // 左侧圆点指示（.on 时红色 #ff5367）
        Box(
            Modifier
                .align(Alignment.CenterStart)
                .size(6.dp)
                .clip(RoundedCornerShape(50))
                .background(if (isOn) Color(0xFFFF5367) else Color(0x55FFFFFF))
                .alpha(if (isOn) 1f else 0.5f),
        )
        Text(
            text,
            color = MineradioColors.FcInk,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 12.dp),
        )
    }
}
