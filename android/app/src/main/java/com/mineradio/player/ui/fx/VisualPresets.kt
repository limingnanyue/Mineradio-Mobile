package com.mineradio.player.ui.fx

/**
 * 视觉预设 —— 与 index.html:20088-20096 的 presetMeta 7 项 1:1 对齐。
 * presetDisplayOrder = [0, 6, 5, 4, 2, 1, 3]（index.html:20106）。
 */
data class VisualPreset(
    val index: Int,
    val name: String,
    val desc: String,
)

object VisualPresets {
    val all: List<VisualPreset> = listOf(
        VisualPreset(0, "emily专辑封面", "封面粒子 · 快速入场"),
        VisualPreset(1, "滚筒", "隧道 · 沉浸感"),
        VisualPreset(2, "星球", "星球 · 雕塑感"),
        VisualPreset(3, "虚空", "无粒子 · 自定义背景"),
        VisualPreset(4, "唱片", "唱片 · 圆形封面"),
        VisualPreset(5, "星河", "壁纸粒子 · 音乐律动"),
        VisualPreset(6, "安魂", "骷髅·YUI7W"),
    )

    /** 桌面版展示顺序（index.html:20106）。 */
    val displayOrder: List<Int> = listOf(0, 6, 5, 4, 2, 1, 3)

    fun inDisplayOrder(): List<VisualPreset> = displayOrder.map { all[it] }
}
