package com.mineradio.player.ui.fx

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * FX 配置状态 —— 对应桌面版全局 `fx` 对象中与 DIY / 视觉 / 歌词相关的字段。
 * 字段默认值与 index.html:3239-3247 的 fxDefaults 对齐。
 *
 * 这是 DIY 功能的核心数据模型：颜色、尺寸、透明度、位置、锁定、预设、壁纸、沉浸等。
 */
@Immutable
data class FxState(
    // ---- 视觉预设 ----
    val preset: Int = 0,                    // 0..6

    // ---- 桌面歌词 DIY（对应 fx.desktopLyrics*）----
    val desktopLyrics: Boolean = false,
    val desktopLyricsSize: Float = 1.0f,    // 0.72..1.55
    val desktopLyricsOpacity: Float = 0.92f,// 0.28..1
    val desktopLyricsY: Float = 0.76f,      // 0.08..0.92
    val desktopLyricsClickThrough: Boolean = false,  // 锁定
    val desktopLyricsCinema: Boolean = true,
    val desktopLyricsHighlight: Boolean = false,
    val desktopLyricsFps: Int = 60,         // 24/30/60/120/0(无上限)

    // ---- 歌词颜色 DIY（对应 fx.lyricColorMode / lyricColor / lyricHighlightColor / lyricGlowColor / visualTintColor）----
    val lyricColorMode: String = "auto",    // auto / custom
    val lyricColor: Color = Color(0xFFA9B8C8),          // 雾蓝
    val lyricHighlightColor: Color = Color(0xFFFFF0B8), // 暖奶油
    val lyricGlowColor: Color = Color(0xFF9DB8CF),      // 银蓝
    val visualTintColor: Color = Color(0xFF9DB8CF),     // 银蓝（次级色）
    val lyricGlowParticles: Boolean = false,

    // ---- 壁纸模式 DIY（对应 fx.wallpaperMode / wallpaperOpacity）----
    // 桌面版 DEVELOPMENT_LOCKED_FX.wallpaperMode = true，这里保留字段与 UI（显示「开发中」徽标），
    // 但 enabled 强制 false，与桌面版行为一致。
    val wallpaperMode: Boolean = false,
    val wallpaperOpacity: Float = 1.0f,     // 0.35..1
    val wallpaperDevLocked: Boolean = true,

    // ---- DIY / Simple 模式（diyPlayerMode）----
    val diyMode: Boolean = false,

    // ---- 沉浸模式 ----
    val immersiveMode: Boolean = false,

    // ---- 歌词字体 DIY（对应 fx.lyricFont / lyricLetterSpacing / lyricLineHeight / lyricWeight）----
    // 桌面版 9 种字体名：default / heiti / songti / cubsong / shiyin / kaisong / serif / gothic / editorial
    val lyricFont: String = "default",
    val lyricLetterSpacing: Float = 0f,     // -0.04..0.18
    val lyricLineHeight: Float = 1.18f,     // 0.9..1.8
    val lyricWeight: Int = 400,             // 100..900
    val lyricScale: Float = 1.0f,           // 0.35..1.65

    // ---- 粒子星河高级参数（对应桌面版 #fx-advanced fold）----
    val particleSize: Float = 1.0f,         // fx.point  0.4..2.4  粒子尺寸
    val particleSpeed: Float = 1.0f,        // fx.speed  0.2..2.6  流速
    val particleTwist: Float = 1.0f,        // fx.twist  0.0..2.4  扭曲（轨道波动幅度）
    val particleColor: Float = 1.0f,        // fx.color  0.0..1.8  色彩张力
    val particleBloom: Float = 1.0f,        // fx.bloom  0.0..2.2  溢光（高光阈值/强度）
    val particleScatter: Float = 1.0f,      // fx.scatter 0.0..1.8  离散（半径环离散度）
    val particleBgFade: Float = 1.0f,       // fx.bgfade  0.0..1.6  背景压缩（aura 强度）

    // ---- 3D 歌单架参数（对应桌面版 #fx-stage-fold）----
    val shelfSize: Float = 1.0f,            // fx.shelfsize   0.5..1.7
    val shelfX: Float = 0f,                 // fx.shelfx      -1.2..1.2
    val shelfY: Float = 0f,                 // fx.shelfy      -1.0..1.0
    val shelfZ: Float = 0f,                 // fx.shelfz      -1.5..1.5
    val shelfAngle: Float = 0f,             // fx.shelfangle  -30..30 度
    val shelfOpacity: Float = 1.0f,         // fx.shelfopacity 0.2..1
    val shelfBgAlpha: Float = 0.0f,         // fx.shelfbgalpha 0..0.6 架子背景板透明度
    val shelfAccent: Color = Color(0xFFF4D28A), // fx.shelfaccent 香槟金
    val shelfShowPodcasts: Boolean = false, // #t-shelfShowPodcasts
    val shelfMergeCollections: Boolean = false, // #t-shelfMergeCollections
    val shelfCameraMode: Int = 0,           // #shelf-camera-seg 0=动态 / 1=静态
    val shelfPresenceMode: Int = 0,         // #shelf-presence-seg 0=自动隐藏 / 1=常驻

    // ---- 摄像头交互（对应桌面版 #cam-seg）----
    val cameraInteraction: Int = 0,         // 0=关闭 / 1=手势触碰
) {
    /** 桌面歌词配色（desktopOverlayColors 等价实现）。 */
    fun overlayColors(): OverlayColors {
        val primary = if (lyricColorMode == "custom") lyricColor else LyricColorPresets.DefaultPrimary
        val secondary = if (lyricColorMode == "custom") visualTintColor else LyricColorPresets.DefaultSecondary
        val highlight = if (lyricColorMode == "custom") lyricHighlightColor else LyricColorPresets.DefaultHighlight
        val glow = if (lyricColorMode == "custom") {
            // glow 未被用户单独修改（仍等于默认银蓝）时退回到次级色，保持配色协调
            if (lyricGlowColor != Color(0xFF9DB8CF)) lyricGlowColor else visualTintColor
        } else LyricColorPresets.DefaultGlow
        return OverlayColors(primary, secondary, highlight, glow)
    }

    /** 壁纸是否真正启用（受 dev-lock 约束，与桌面版一致）。 */
    val wallpaperEffectivelyEnabled: Boolean
        get() = wallpaperMode && !wallpaperDevLocked

    /** 桌面歌词是否真正启用。 */
    val desktopLyricsEffectivelyEnabled: Boolean
        get() = desktopLyrics
}

@Immutable
data class OverlayColors(
    val primary: Color,
    val secondary: Color,
    val highlight: Color,
    val glow: Color,
)
