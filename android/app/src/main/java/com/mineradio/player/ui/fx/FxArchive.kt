package com.mineradio.player.ui.fx

import androidx.compose.runtime.Immutable

/**
 * FX 存档快照 —— 对应桌面版 captureFxArchiveSnapshot() 的对象结构
 * （index.html:20147-20197）。保存的是当前视觉/FX 配置，不是图片。
 *
 * 用户点「保存」按钮（glass-saved-button）时，把当前 FxState 拍快照存入槽位。
 */
@Immutable
data class FxArchiveSnapshot(
    val preset: Int = 0,
    val desktopLyrics: Boolean = false,
    val desktopLyricsSize: Float = 1.0f,
    val desktopLyricsOpacity: Float = 0.92f,
    val desktopLyricsY: Float = 0.76f,
    val desktopLyricsClickThrough: Boolean = false,
    val desktopLyricsCinema: Boolean = true,
    val desktopLyricsHighlight: Boolean = false,
    val desktopLyricsFps: Int = 60,
    val lyricColorMode: String = "auto",
    val lyricColor: Long = 0xFFA9B8C8,
    val lyricHighlightColor: Long = 0xFFFFF0B8,
    val lyricGlowColor: Long = 0xFF9DB8CF,
    val visualTintColor: Long = 0xFF9DB8CF,
    val lyricGlowParticles: Boolean = false,
    val wallpaperMode: Boolean = false,
    val wallpaperOpacity: Float = 1.0f,
    // 自定义背景
    val customBgType: String = "none",
    val customBgColor: Long = 0xFF05060A,
    val customBgUri: String = "",
    val lyricFont: String = "default",
    val lyricLetterSpacing: Float = 0f,
    val lyricLineHeight: Float = 1.18f,
    val lyricWeight: Int = 400,
    val lyricScale: Float = 1.0f,
    // 粒子星河高级参数
    val particleSize: Float = 1.0f,
    val particleSpeed: Float = 1.0f,
    val particleTwist: Float = 1.0f,
    val particleColor: Float = 1.0f,
    val particleBloom: Float = 1.0f,
    val particleScatter: Float = 1.0f,
    val particleBgFade: Float = 1.0f,
    // 3D 歌单架参数
    val shelfSize: Float = 1.0f,
    val shelfX: Float = 0f,
    val shelfY: Float = 0f,
    val shelfZ: Float = 0f,
    val shelfAngle: Float = 0f,
    val shelfOpacity: Float = 1.0f,
    val shelfBgAlpha: Float = 0.0f,
    val shelfAccent: Long = 0xFFF4D28A,
    val shelfShowPodcasts: Boolean = false,
    val shelfMergeCollections: Boolean = false,
    val shelfCameraMode: Int = 0,
    val shelfPresenceMode: Int = 0,
    val cameraInteraction: Int = 0,
)

/**
 * FX 存档槽位 —— 对应桌面版 userFxArchives[i] = { name, savedAt, snapshot }。
 * has-save 时边框变青色高亮（与 .user-archive-slot.has-save 一致）。
 */
@Immutable
data class FxArchiveSlot(
    val index: Int,
    val name: String,
    val savedAt: Long = 0L,        // 0 = 未保存
    val snapshot: FxArchiveSnapshot? = null,
) {
    val hasSave: Boolean get() = snapshot != null && savedAt > 0
}

object FxArchives {
    /** 默认 4 个槽位（与桌面版默认一致）。 */
    fun defaultSlots(count: Int = 4): List<FxArchiveSlot> = (0 until count).map {
        FxArchiveSlot(index = it, name = "存档 ${it + 1}")
    }

    fun defaultNames(): List<String> = listOf("晨曦", "星河", "深海", "暮光")
}
