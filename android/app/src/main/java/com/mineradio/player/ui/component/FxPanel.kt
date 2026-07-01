package com.mineradio.player.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mineradio.player.ui.fx.FxState
import com.mineradio.player.ui.fx.VisualPresets
import com.mineradio.player.ui.theme.MineradioColors

/**
 * FX 视觉控制台 —— 复刻桌面版 #fx-panel 折叠分区结构
 * （index.html:20080-20260 的 fold：预设 / 主控 / 歌词 / 叠加 / 3D 架 / 高级）。
 *
 * 这是 DIY 模式的总控制台，把粒子星河、歌词字体、3D 架、色彩叠加等全部参数
 * 集中到一个可滚动的折叠面板里。每个 fold 可独立展开/收起。
 *
 * 直接绑定 FxState：所有改动通过 [onUpdate] 回调以 transform 语义写回，
 * 与 MainViewModel.updateFx 签名一致，可传 `vm::updateFx`。
 */
@Composable
fun FxPanel(
    fx: FxState,
    onUpdate: ((FxState) -> FxState) -> Unit,
    onOpenColorLab: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // 1. 视觉预设
        FoldSection(title = "视觉预设", defaultOpen = true) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                VisualPresets.inDisplayOrder().forEach { p ->
                    val selected = fx.preset == p.index
                    Box(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selected) MineradioColors.FcAccent else Color(0x14FFFFFF))
                            .clickable { onUpdate { it.copy(preset = p.index) } }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            p.name,
                            color = if (selected) MineradioColors.ChillInk else MineradioColors.FcInk2,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }

        // 2. 主控 —— DIY / 沉浸 / 桌面歌词总开关
        FoldSection(title = "主控") {
            SwitchRow("DIY 模式", fx.diyMode) { v -> onUpdate { it.copy(diyMode = v) } }
            SwitchRow("沉浸模式", fx.immersiveMode) { v -> onUpdate { it.copy(immersiveMode = v) } }
            SwitchRow("桌面歌词", fx.desktopLyrics) { v -> onUpdate { it.copy(desktopLyrics = v) } }
            SwitchRow("影院遮幅", fx.desktopLyricsCinema) { v -> onUpdate { it.copy(desktopLyricsCinema = v) } }
            SwitchRow("高亮态", fx.desktopLyricsHighlight) { v -> onUpdate { it.copy(desktopLyricsHighlight = v) } }
            SwitchRow("锁定（防误触）", fx.desktopLyricsClickThrough) { v -> onUpdate { it.copy(desktopLyricsClickThrough = v) } }
            SliderRow("桌面歌词尺寸", fx.desktopLyricsSize, 0.72f, 1.55f) { v -> onUpdate { it.copy(desktopLyricsSize = v) } }
            SliderRow("桌面歌词透明度", fx.desktopLyricsOpacity, 0.28f, 1f) { v -> onUpdate { it.copy(desktopLyricsOpacity = v) } }
            SliderRow("桌面歌词垂直位置", fx.desktopLyricsY, 0.08f, 0.92f) { v -> onUpdate { it.copy(desktopLyricsY = v) } }
            Text("帧率", color = MineradioColors.FcMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(24, 30, 60, 120, 0).forEach { f ->
                    val label = if (f == 0) "无上限" else "$f"
                    SegChip(label, fx.desktopLyricsFps == f) { onUpdate { it.copy(desktopLyricsFps = f) } }
                }
            }
        }

        // 3. 歌词字体 / 配色
        FoldSection(title = "歌词") {
            Text("字体", color = MineradioColors.FcMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            FontGrid(fx.lyricFont) { v -> onUpdate { it.copy(lyricFont = v) } }
            Spacer(Modifier.height(4.dp))
            SliderRow("字间距", fx.lyricLetterSpacing, -0.04f, 0.18f) { v -> onUpdate { it.copy(lyricLetterSpacing = v) } }
            SliderRow("行高", fx.lyricLineHeight, 0.9f, 1.8f) { v -> onUpdate { it.copy(lyricLineHeight = v) } }
            SliderRow("字重", fx.lyricWeight.toFloat(), 100f, 900f) { v -> onUpdate { it.copy(lyricWeight = v.toInt()) } }
            SliderRow("缩放", fx.lyricScale, 0.35f, 1.65f) { v -> onUpdate { it.copy(lyricScale = v) } }
            Text("配色模式", color = MineradioColors.FcMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SegChip("自动", fx.lyricColorMode == "auto") { onUpdate { it.copy(lyricColorMode = "auto") } }
                SegChip("自定义", fx.lyricColorMode == "custom") { onUpdate { it.copy(lyricColorMode = "custom") } }
            }
            ColorLabRow("主色", fx.lyricColor) { onOpenColorLab("lyric") }
            ColorLabRow("高亮色", fx.lyricHighlightColor) { onOpenColorLab("highlight") }
            ColorLabRow("溢光色", fx.lyricGlowColor) { onOpenColorLab("glow") }
            ColorLabRow("次级色", fx.visualTintColor) { onOpenColorLab("tint") }
            SwitchRow("歌词溢光粒子", fx.lyricGlowParticles) { v -> onUpdate { it.copy(lyricGlowParticles = v) } }
        }

        // 4. 壁纸 / 叠加
        FoldSection(title = "叠加 / 壁纸") {
            SwitchRow("壁纸模式${if (fx.wallpaperDevLocked) "（开发中）" else ""}", fx.wallpaperMode && !fx.wallpaperDevLocked) {
                if (!fx.wallpaperDevLocked) onUpdate { it.copy(wallpaperMode = !it.wallpaperMode) }
            }
            SliderRow("壁纸透明度", fx.wallpaperOpacity, 0.35f, 1f) { v -> onUpdate { it.copy(wallpaperOpacity = v) } }
        }

        // 5. 3D 歌单架
        FoldSection(title = "3D 歌单架") {
            SliderRow("架尺寸", fx.shelfSize, 0.5f, 1.7f) { v -> onUpdate { it.copy(shelfSize = v) } }
            SliderRow("X 偏移", fx.shelfX, -1.2f, 1.2f) { v -> onUpdate { it.copy(shelfX = v) } }
            SliderRow("Y 偏移", fx.shelfY, -1f, 1f) { v -> onUpdate { it.copy(shelfY = v) } }
            SliderRow("Z 偏移", fx.shelfZ, -1.5f, 1.5f) { v -> onUpdate { it.copy(shelfZ = v) } }
            SliderRow("旋转角度", fx.shelfAngle, -30f, 30f) { v -> onUpdate { it.copy(shelfAngle = v) } }
            SliderRow("架透明度", fx.shelfOpacity, 0.2f, 1f) { v -> onUpdate { it.copy(shelfOpacity = v) } }
            SliderRow("背景板透明度", fx.shelfBgAlpha, 0f, 0.6f) { v -> onUpdate { it.copy(shelfBgAlpha = v) } }
            ColorLabRow("架描边色", fx.shelfAccent) { onOpenColorLab("shelfAccent") }
            SwitchRow("显示播客", fx.shelfShowPodcasts) { v -> onUpdate { it.copy(shelfShowPodcasts = v) } }
            SwitchRow("合并收藏", fx.shelfMergeCollections) { v -> onUpdate { it.copy(shelfMergeCollections = v) } }
            Text("镜头模式", color = MineradioColors.FcMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SegChip("动态", fx.shelfCameraMode == 0) { onUpdate { it.copy(shelfCameraMode = 0) } }
                SegChip("静态", fx.shelfCameraMode == 1) { onUpdate { it.copy(shelfCameraMode = 1) } }
            }
            Text("在场模式", color = MineradioColors.FcMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SegChip("自动隐藏", fx.shelfPresenceMode == 0) { onUpdate { it.copy(shelfPresenceMode = 0) } }
                SegChip("常驻", fx.shelfPresenceMode == 1) { onUpdate { it.copy(shelfPresenceMode = 1) } }
            }
            Text("摄像头交互", color = MineradioColors.FcMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SegChip("关闭", fx.cameraInteraction == 0) { onUpdate { it.copy(cameraInteraction = 0) } }
                SegChip("手势触碰", fx.cameraInteraction == 1) { onUpdate { it.copy(cameraInteraction = 1) } }
            }
        }

        // 6. 粒子星河高级参数
        FoldSection(title = "粒子星河 · 高级", defaultOpen = false) {
            SliderRow("粒子尺寸", fx.particleSize, 0.4f, 2.4f) { v -> onUpdate { it.copy(particleSize = v) } }
            SliderRow("流速", fx.particleSpeed, 0.2f, 2.6f) { v -> onUpdate { it.copy(particleSpeed = v) } }
            SliderRow("扭曲", fx.particleTwist, 0f, 2.4f) { v -> onUpdate { it.copy(particleTwist = v) } }
            SliderRow("色彩张力", fx.particleColor, 0f, 1.8f) { v -> onUpdate { it.copy(particleColor = v) } }
            SliderRow("溢光", fx.particleBloom, 0f, 2.2f) { v -> onUpdate { it.copy(particleBloom = v) } }
            SliderRow("离散", fx.particleScatter, 0f, 1.8f) { v -> onUpdate { it.copy(particleScatter = v) } }
            SliderRow("背景压缩", fx.particleBgFade, 0f, 1.6f) { v -> onUpdate { it.copy(particleBgFade = v) } }
        }
    }
}

// ============ 折叠分区 ============

@Composable
private fun FoldSection(
    title: String,
    defaultOpen: Boolean = true,
    content: @Composable () -> Unit,
) {
    var open by remember { mutableStateOf(defaultOpen) }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x0DFFFFFF))
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { open = !open }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                title,
                color = MineradioColors.FcInk,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            Icon(
                if (open) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                null,
                tint = MineradioColors.FcMuted,
                modifier = Modifier.size(18.dp),
            )
        }
        AnimatedVisibility(
            visible = open,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            Column(
                Modifier.padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                content()
            }
        }
    }
}

// ============ 通用控件 ============

@Composable
private fun SwitchRow(label: String, on: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onChange(!on) }.padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = MineradioColors.FcInk2, fontSize = 12.sp, modifier = Modifier.weight(1f))
        Switch(checked = on, onCheckedChange = onChange)
    }
}

@Composable
private fun SliderRow(label: String, value: Float, min: Float, max: Float, onChange: (Float) -> Unit) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = MineradioColors.FcMuted, fontSize = 11.sp, modifier = Modifier.weight(1f))
            Text("${"%.2f".format(value)}", color = MineradioColors.Champagne, fontSize = 11.sp)
        }
        Slider(
            value = value.coerceIn(min, max),
            onValueChange = onChange,
            valueRange = min..max,
            colors = SliderDefaults.colors(
                thumbColor = MineradioColors.FcAccent,
                activeTrackColor = MineradioColors.FcAccent,
            ),
        )
    }
}

@Composable
private fun SegChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) MineradioColors.FcAccent else Color(0x1AFFFFFF))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(
            label,
            color = if (selected) MineradioColors.ChillInk else MineradioColors.FcInk2,
            fontSize = 11.sp,
        )
    }
}

@Composable
private fun ColorLabRow(label: String, color: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = MineradioColors.FcInk2, fontSize = 12.sp, modifier = Modifier.weight(1f))
        Box(
            Modifier
                .size(42.dp, 28.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(color)
                .border(1.dp, Color(0x29FFFFFF), RoundedCornerShape(8.dp)),
        )
    }
}

@Composable
private fun FontGrid(selected: String, onSelect: (String) -> Unit) {
    val fonts = listOf("default", "heiti", "songti", "cubsong", "shiyin", "kaisong", "serif", "gothic", "editorial")
    fonts.chunked(3).forEach { row ->
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            row.forEach { name ->
                Box(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selected == name) MineradioColors.FcAccent else Color(0x14FFFFFF))
                        .clickable { onSelect(name) }
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        name,
                        color = if (selected == name) MineradioColors.ChillInk else MineradioColors.FcInk2,
                        fontSize = 10.sp,
                    )
                }
            }
            if (row.size < 3) repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
        }
    }
}
