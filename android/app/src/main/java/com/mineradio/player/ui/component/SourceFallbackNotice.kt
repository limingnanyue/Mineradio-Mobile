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
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mineradio.player.ui.theme.MineradioColors

/**
 * 自动换源提示 —— 1:1 复刻桌面版 #source-fallback-notice。
 *
 * 桌面版来源：public/index.html:2649-2655
 *   - 当播放源回退（如网易云→QQ）时显示
 *   - 标题「自动换源」+ 换源说明文案 + 关闭按钮
 *
 * 样式：深色半透明背景，底部悬浮。
 */
@Composable
fun SourceFallbackNotice(
    visible: Boolean,
    text: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut(), modifier = modifier) {
        Column(
            Modifier
                .width(320.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MineradioColors.GlassDark.copy(alpha = 0.95f))
                .border(1.dp, MineradioColors.FcAccent.copy(alpha = 0.30f), RoundedCornerShape(12.dp))
                .padding(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.SwapHoriz, contentDescription = null, tint = MineradioColors.FcAccent, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("自动换源", color = MineradioColors.FcAccent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Text(
                    "×",
                    color = MineradioColors.FcMuted,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onDismiss() }.padding(horizontal = 4.dp),
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(text, color = MineradioColors.FcInk2, fontSize = 12.sp)
        }
    }
}
