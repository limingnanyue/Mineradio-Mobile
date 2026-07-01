package com.mineradio.player.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * DIY 浮区 —— 复刻桌面版 #fullscreen-diy-zone（index.html:77-88, 1879-1881）。
 *
 * 桌面版逻辑：仅在全屏模式下显示，pointer 接近时 peek 显出 DIY 按钮，点击切换 DIY 模式。
 * 移动端横屏永远全屏，因此常驻显示（按桌面版 peek 行为：透明度 0→1 渐入）。
 *
 * 位置：top=24dp, left=距右边 510px 处（移动端横屏宽度通常 ≥800，左侧留出空间）。
 */
@Composable
fun DiyFloatingZone(
    visible: Boolean,
    diyMode: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically(initialOffsetY = { -18 }),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { -18 }),
        modifier = modifier,
    ) {
        Box(Modifier.padding(end = 16.dp)) {
            DesktopModeButton(
                text = "DIY",
                isOn = diyMode,
                onClick = onToggle,
            )
        }
    }
}
