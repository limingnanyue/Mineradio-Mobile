package com.mineradio.player.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Mineradio 色牌 —— 与桌面版 public/index.html 中 :root CSS 变量 1:1 对齐。
 * 来源：index.html:24-27 的 --fc-* / --chill-* / --champagne / --home-accent / --visual-tint。
 * 不做任何视觉改型，仅把十六进制搬进原生 Color。
 */
object MineradioColors {
    // 基础纸面 / 墨色
    val FcBg = Color(0xFF08090B)         // --fc-bg
    val FcPaper = Color(0xFF0E1014)      // --fc-paper
    val FcInk = Color(0xFFE8ECEF)        // --fc-ink
    val FcInk2 = Color(0xFFD2D7DC)       // --fc-ink-2
    val FcMuted = Color(0xFF8A9099)      // --fc-muted
    val FcHair = Color(0xFF1A1D22)       // --fc-hair
    val FcHair2 = Color(0xFF262A31)      // --fc-hair-2
    val FcAccent = Color(0xFF00F5D4)     // --fc-accent (主强调青)
    val FcAccentHov = Color(0xFF00E0BE)  // --fc-accent-hov
    val FcAccentRgb = intArrayOf(0, 245, 212)
    val FcBlue = Color(0xFF2442FF)       // --fc-blue
    val FcWarm = Color(0xFFF8F4EE)       // --fc-warm

    // Chill（壁纸 / 银河星河氛围）
    val ChillInk = Color(0xFF030608)     // --chill-ink
    val ChillDeep = Color(0xFF061116)    // --chill-deep
    val ChillCyan = Color(0xFF8FE9FF)    // --chill-cyan
    val ChillBlue = Color(0xFF73A7FF)    // --chill-blue
    val ChillMint = Color(0xFF9CFFDF)    // --chill-mint
    val ChillSoft = Color(0x66D6F8FF.toInt()) // rgba(214,248,255,.9) — 用于文字描边

    // Champagne（首页 / 高光金）
    val Champagne = Color(0xFFF4D28A)    // --champagne
    val ChampagneDeep = Color(0xFF9A6F2C)// --champagne-deep

    // 首页 / 视觉图标
    val HomeAccent = Color(0xFF00F5D4)   // --home-accent
    val VisualTint = Color(0xFF7FD8FF)   // --visual-icon-color

    // 来源色（音乐平台标识）
    val SourceNetease = Color(0xFFD95B67)
    val SourceQq = Color(0xFF00F5D4)
    val SourceLocal = Color(0xFF9DB8CF)

    // 玻璃面板底色（取 --glass-bg 中间停色）
    val GlassDark = Color(0xFF181B1E)
    val GlassDarker = Color(0xFF080C0E)

    // 危险 / 关闭按钮
    val Danger = Color(0xFFFF5367)

    // 壁纸 / 粒子引擎默认配色（wallpaper.html state.colors）
    val WallpaperBg = Color(0xFF050608)
    val ParticlePrimary = Color(0xFFD6F8FF)
    val ParticleSecondary = Color(0xFF9CFFDF)
    val ParticleHighlight = Color(0xFFFFF0B8)
    val ParticleGlow = Color(0xFF9CFFDF)
}
