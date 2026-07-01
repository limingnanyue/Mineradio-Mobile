package com.mineradio.player.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * 字体层级 —— 对齐桌面版 --font-sans / --font-mono 用法。
 * 中文走系统默认（PingFang/Heiti/Noto Sans SC 在设备上原生可用），
 * 不引入外部字体文件，避免增加包体（用户接受大内存，但字体无必要内置）。
 */
object MineradioType {
    val Sans = FontFamily.Default
    val Mono = FontFamily.Monospace
}

val MineradioTypography = Typography(
    displayLarge = TextStyle(fontFamily = MineradioType.Sans, fontWeight = FontWeight.Black, fontSize = 40.sp, letterSpacing = 0.5.sp),
    displayMedium = TextStyle(fontFamily = MineradioType.Sans, fontWeight = FontWeight.Black, fontSize = 32.sp),
    headlineLarge = TextStyle(fontFamily = MineradioType.Sans, fontWeight = FontWeight.Bold, fontSize = 24.sp),
    headlineMedium = TextStyle(fontFamily = MineradioType.Sans, fontWeight = FontWeight.Bold, fontSize = 20.sp),
    titleLarge = TextStyle(fontFamily = MineradioType.Sans, fontWeight = FontWeight.SemiBold, fontSize = 18.sp),
    titleMedium = TextStyle(fontFamily = MineradioType.Sans, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, letterSpacing = 0.3.sp),
    titleSmall = TextStyle(fontFamily = MineradioType.Sans, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, letterSpacing = 0.6.sp),
    bodyLarge = TextStyle(fontFamily = MineradioType.Sans, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontFamily = MineradioType.Sans, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 19.sp),
    bodySmall = TextStyle(fontFamily = MineradioType.Sans, fontWeight = FontWeight.Normal, fontSize = 11.sp, lineHeight = 15.sp),
    labelLarge = TextStyle(fontFamily = MineradioType.Sans, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, letterSpacing = 0.8.sp),
    labelMedium = TextStyle(fontFamily = MineradioType.Sans, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, letterSpacing = 1.0.sp, textTransform = null),
    labelSmall = TextStyle(fontFamily = MineradioType.Sans, fontWeight = FontWeight.SemiBold, fontSize = 10.sp, letterSpacing = 1.0.sp),
)
