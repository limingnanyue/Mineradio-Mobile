package com.mineradio.player.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mineradio.player.ui.theme.MineradioColors

/**
 * 玻璃面板 —— 复刻桌面版 --glass-bg / --glass-border / --glass-shadow。
 * 桌面版用 backdrop-filter: blur(12px) saturate(1.8) brightness(1.16)，
 * Compose 用 Modifier.blur 模拟（API31+ 真模糊，低版本退化阴影）。
 */
@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    cornerRadius: Int = 24,
    content: @Composable BoxScope.() -> Unit,
) {
    val shape = RoundedCornerShape(cornerRadius.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0x9E484A4C),
                        Color(0xB3181B1E),
                        Color(0xBD080C0E),
                    ),
                )
            )
            .border(1.dp, MineradioColors.FcAccent.copy(alpha = 0.30f), shape)
            .shadow(
                elevation = 24.dp,
                shape = shape,
                ambientColor = Color.Black.copy(alpha = 0.30f),
                spotColor = MineradioColors.FcAccent.copy(alpha = 0.05f),
            )
            .padding(0.dp),
        content = content,
    )
}
