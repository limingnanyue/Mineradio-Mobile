package com.mineradio.player.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mineradio.player.ui.fx.VisualPresets
import com.mineradio.player.ui.theme.MineradioColors

/**
 * 壁纸模式 DIY 面板 —— 复刻桌面版 FX 面板里的壁纸分区
 * （index.html:2165, 2179, 20088-20096 预设）。
 *
 * 桌面版 wallpaperMode 处于 DEVELOPMENT_LOCKED 状态：toggle 带「开发中」徽标且 disabled，
 * 透明度滑块 disabled。这里忠实保留该状态（UI 完整呈现，但不可操作，与桌面版一致）。
 */
@Composable
fun WallpaperDiyPanel(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    devLocked: Boolean,
    opacity: Float,
    onOpacityChange: (Float) -> Unit,
    preset: Int,
    onPresetChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // 壁纸模式开关（dev-locked 时带徽标 + disabled）
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .alpha(if (devLocked) 0.5f else 1f)
                .clickable(enabled = !devLocked) { onEnabledChange(!enabled) }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("壁纸模式", color = MineradioColors.FcInk, fontSize = 13.sp, modifier = Modifier.weight(1f))
            if (devLocked) {
                Text(
                    "开发中",
                    color = MineradioColors.Champagne,
                    fontSize = 10.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(MineradioColors.Champagne.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                )
                Spacer(Modifier.width(8.dp))
            }
            Switch(checked = enabled && !devLocked, onCheckedChange = null, enabled = !devLocked)
        }

        // 视觉预设（7 个，按 displayOrder 展示）
        Text("视觉预设", color = MineradioColors.FcMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            VisualPresets.inDisplayOrder().forEach { p ->
                val selected = p.index == preset
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selected) MineradioColors.FcAccent.copy(alpha = 0.12f) else Color.Transparent)
                        .border(
                            width = if (selected) 1.dp else 0.dp,
                            color = if (selected) MineradioColors.FcAccent else Color.Transparent,
                            shape = RoundedCornerShape(8.dp),
                        )
                        .clickable { onPresetChange(p.index) }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(p.name, color = MineradioColors.FcInk, fontSize = 12.sp, modifier = Modifier.weight(1f))
                    Text(p.desc, color = MineradioColors.FcMuted, fontSize = 10.sp)
                }
            }
        }

        // 透明度滑块（dev-locked 时 disabled）
        Column(modifier = Modifier.alpha(if (devLocked) 0.5f else 1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("壁纸透明度", color = MineradioColors.FcMuted, fontSize = 11.sp, modifier = Modifier.weight(1f))
                Text("${"%.2f".format(opacity)}", color = MineradioColors.Champagne, fontSize = 11.sp)
            }
            Slider(
                value = opacity,
                onValueChange = onOpacityChange,
                valueRange = 0.35f..1f,
                enabled = !devLocked,
            )
        }
    }
}
