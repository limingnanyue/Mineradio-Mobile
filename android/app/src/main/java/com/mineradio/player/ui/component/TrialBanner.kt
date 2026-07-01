package com.mineradio.player.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
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
 * 试听片段横幅 —— 1:1 复刻桌面版 #trial-banner。
 *
 * 桌面版来源：public/index.html:2309-2314
 *   - 当 song/url 返回 data.trial=true 时显示
 *   - 文案随登录态/VIP 等级变化
 *   - 未登录时显示「扫码登录」按钮
 *   - 可手动关闭
 *
 * 样式：香槟色半透明背景 + 边框，居中悬浮于顶部下方。
 */
@Composable
fun TrialBanner(
    visible: Boolean,
    text: String,
    loggedIn: Boolean,
    onLoginClick: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut(), modifier = modifier) {
        Row(
            Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(MineradioColors.Champagne.copy(alpha = 0.11f))
                .border(1.dp, MineradioColors.Champagne.copy(alpha = 0.40f), RoundedCornerShape(12.dp))
                .clickable(enabled = false) {}
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                Icons.Filled.Info,
                contentDescription = null,
                tint = MineradioColors.Champagne,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text,
                color = MineradioColors.Champagne,
                fontSize = 12.sp,
                letterSpacing = 0.3.sp,
            )
            if (!loggedIn) {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.White.copy(alpha = 0.10f))
                        .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                        .clickable { onLoginClick() }
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text("扫码登录", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                }
            }
            Text(
                "×",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clickable { onDismiss() }
                    .padding(horizontal = 4.dp, vertical = 2.dp),
            )
        }
    }
}
