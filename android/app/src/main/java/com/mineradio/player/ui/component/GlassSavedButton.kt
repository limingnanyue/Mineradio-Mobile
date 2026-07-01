package com.mineradio.player.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mineradio.player.ui.theme.MineradioColors

/**
 * 玻璃保存按钮纹理 —— 复刻桌面版 .glass-saved-button / --saved-button-glass-*。
 *
 * 桌面版：background rgba(0,0,0,.10) + backdrop-filter blur(12px) saturate(1.8) brightness(1.16)
 * + inset 高光阴影。Compose 用半透明渐变 + border + shadow 模拟。
 *
 * 这是桌面版所有按钮（icon-btn / fx-mini-btn / fx-toggle / preset-card 等）共享的玻璃质感。
 */
@Composable
fun GlassSavedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    cornerRadius: Int = 12,
    content: @Composable BoxScope.() -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val shape = RoundedCornerShape(cornerRadius.dp)
    val bg = if (pressed) Color(0x0EFFFFFF) else Color(0x1A000000)
    val borderColor = if (pressed) Color(0x6BFFFFFF) else Color(0x57FFFFFF)
    Box(
        modifier = modifier
            .clip(shape)
            .background(bg)
            .border(1.dp, borderColor, shape)
            .shadow(
                elevation = if (pressed) 12.dp else 10.dp,
                shape = shape,
                ambientColor = Color.Black.copy(alpha = 0.18f),
                spotColor = Color.White.copy(alpha = 0.06f),
            )
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 14.dp, vertical = 8.dp),
        content = content,
    )
}

/**
 * 玻璃面板变体（--saved-panel-glass-*）—— 50px 圆角，更重阴影，用于模态/舞台。
 */
@Composable
fun GlassSavedPanel(
    modifier: Modifier = Modifier,
    cornerRadius: Int = 50,
    content: @Composable BoxScope.() -> Unit,
) {
    val shape = RoundedCornerShape(cornerRadius.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(Color(0x1A000000))
            .border(1.dp, Color(0x29FFFFFF), shape)
            .shadow(
                elevation = 30.dp,
                shape = shape,
                ambientColor = Color.Black.copy(alpha = 0.30f),
                spotColor = MineradioColors.FcAccent.copy(alpha = 0.05f),
            ),
        content = content,
    )
}
