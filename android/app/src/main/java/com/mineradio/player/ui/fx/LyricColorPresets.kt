package com.mineradio.player.ui.fx

import androidx.compose.ui.graphics.Color

/**
 * 歌词颜色预设 —— 与 index.html:20107-20126 的 lyricColorPresets 18 项 1:1 对齐。
 * 顺序、名称、色值完全一致。
 */
data class LyricColorPreset(val name: String, val color: Color)

object LyricColorPresets {
    val all: List<LyricColorPreset> = listOf(
        LyricColorPreset("雾蓝", Color(0xFFA9B8C8)),
        LyricColorPreset("银蓝", Color(0xFF9DB8CF)),
        LyricColorPreset("冰川", Color(0xFF7EC8D8)),
        LyricColorPreset("青绿", Color(0xFF66D2B5)),
        LyricColorPreset("松针", Color(0xFF7FA894)),
        LyricColorPreset("月白", Color(0xFFD7D2C4)),
        LyricColorPreset("岩金", Color(0xFFC3AE7C)),
        LyricColorPreset("琥珀", Color(0xFFD9A45F)),
        LyricColorPreset("暮粉", Color(0xFFC78AA4)),
        LyricColorPreset("烟紫", Color(0xFF9B83D3)),
        LyricColorPreset("电紫", Color(0xFF8D70FF)),
        LyricColorPreset("靛蓝", Color(0xFF5E78D8)),
        LyricColorPreset("海蓝", Color(0xFF3C9FE0)),
        LyricColorPreset("霓青", Color(0xFF28C5C3)),
        LyricColorPreset("夜绿", Color(0xFF245C49)),
        LyricColorPreset("墨黑", Color(0xFF111318)),
        LyricColorPreset("酒红", Color(0xFF6D1F35)),
        LyricColorPreset("玫红", Color(0xFFD76A8D)),
    )

    /** 桌面歌词默认配色（desktopOverlayColors 默认值）。 */
    val DefaultPrimary = Color(0xFFD6F8FF)     // 冰青
    val DefaultSecondary = Color(0xFF9CFFDF)   // 薄荷
    val DefaultHighlight = Color(0xFFFFF0B8)   // 暖奶油
    val DefaultGlow = Color(0xFF9CFFDF)        // 薄荷
}
