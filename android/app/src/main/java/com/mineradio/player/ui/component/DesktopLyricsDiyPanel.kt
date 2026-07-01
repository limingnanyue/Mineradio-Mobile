package com.mineradio.player.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mineradio.player.ui.fx.LyricColorPresets
import com.mineradio.player.ui.fx.OverlayColors
import com.mineradio.player.ui.theme.MineradioColors

/**
 * 桌面歌词 DIY 面板 —— 复刻桌面版 FX 面板里的桌面歌词分区
 * （index.html:2096-2108 颜色选择器、2162-2177 滑块/帧率、20107-20126 预设色）。
 *
 * 包含：
 *  - 18 色预设网格（点击应用 fx.lyricColor）
 *  - 主色 / 高亮 / 溢光 / 次级色 取色行
 *  - 尺寸 / 透明度 / Y 位置 滑块
 *  - 锁定开关（clickThrough）
 *  - 帧率分段（24/30/60/120/无上限）
 */
@Composable
fun DesktopLyricsDiyPanel(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    size: Float,
    onSizeChange: (Float) -> Unit,
    opacity: Float,
    onOpacityChange: (Float) -> Unit,
    yPos: Float,
    onYChange: (Float) -> Unit,
    locked: Boolean,
    onLockedChange: (Boolean) -> Unit,
    fps: Int,
    onFpsChange: (Int) -> Unit,
    lyricColor: Color,
    onLyricColorChange: (Color) -> Unit,
    highlightColor: Color,
    onHighlightColorChange: (Color) -> Unit,
    glowColor: Color,
    onGlowColorChange: (Color) -> Unit,
    tintColor: Color,
    onTintColorChange: (Color) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // 总开关
        DiyRow("桌面歌词", enabled) { onEnabledChange(!enabled) }

        // 预设色网格
        Text("歌词配色预设", color = MineradioColors.FcMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        LazyVerticalGrid(
            columns = GridCells.Fixed(6),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.height(180.dp),
        ) {
            items(LyricColorPresets.all) { preset ->
                Box(
                    Modifier
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(preset.color)
                        .border(
                            width = if (preset.color == lyricColor) 2.dp else 0.dp,
                            color = MineradioColors.FcAccent,
                            shape = RoundedCornerShape(8.dp),
                        )
                        .clickable { onLyricColorChange(preset.color) }
                )
            }
        }

        // 取色行
        ColorPickerRow("主色", lyricColor, onLyricColorChange)
        ColorPickerRow("高亮", highlightColor, onHighlightColorChange)
        ColorPickerRow("溢光", glowColor, onGlowColorChange)
        ColorPickerRow("次级", tintColor, onTintColorChange)

        // 滑块
        SliderRow("尺寸", size, 0.72f, 1.55f, onSizeChange)
        SliderRow("透明度", opacity, 0.28f, 1f, onOpacityChange)
        SliderRow("垂直位置", yPos, 0.08f, 0.92f, onYChange)

        // 锁定
        DiyRow("锁定（防误触）", locked) { onLockedChange(!locked) }

        // 帧率分段
        Text("帧率", color = MineradioColors.FcMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(24, 30, 60, 120, 0).forEach { f ->
                val label = if (f == 0) "无上限" else "$f"
                Box(
                    Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (fps == f) MineradioColors.FcAccent else Color(0x1AFFFFFF))
                        .clickable { onFpsChange(f) }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Text(
                        label,
                        color = if (fps == f) MineradioColors.ChillInk else MineradioColors.FcInk2,
                        fontSize = 11.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun DiyRow(label: String, on: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = MineradioColors.FcInk, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Switch(checked = on, onCheckedChange = { onToggle() })
    }
}

@Composable
private fun SliderRow(label: String, value: Float, min: Float, max: Float, onChange: (Float) -> Unit) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = MineradioColors.FcMuted, fontSize = 11.sp, modifier = Modifier.weight(1f))
            Text("${"%.2f".format(value)}", color = MineradioColors.Champagne, fontSize = 11.sp)
        }
        Slider(value = value, onValueChange = onChange, valueRange = min..max)
    }
}

@Composable
private fun ColorPickerRow(label: String, color: Color, onChange: (Color) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
        Text(label, color = MineradioColors.FcInk2, fontSize = 12.sp, modifier = Modifier.weight(1f))
        Box(
            Modifier
                .size(42.dp, 32.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(color)
                .border(1.dp, Color(0x29FFFFFF), RoundedCornerShape(12.dp))
                .clickable { /* 移动端原生取色器由上层弹 */ onChange(color) }
        )
    }
}
