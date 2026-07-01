package com.mineradio.player.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Mineradio 永远是暗场主题（桌面版 #000 背景），不走系统 light/dark 切换。
 */
private val MineradioColorScheme = darkColorScheme(
    primary = MineradioColors.FcAccent,
    onPrimary = MineradioColors.ChillInk,
    primaryContainer = MineradioColors.FcAccentHov,
    onPrimaryContainer = MineradioColors.FcInk,
    secondary = MineradioColors.Champagne,
    onSecondary = MineradioColors.ChillInk,
    tertiary = MineradioColors.ChillCyan,
    onTertiary = MineradioColors.ChillInk,
    background = MineradioColors.FcBg,
    onBackground = MineradioColors.FcInk,
    surface = MineradioColors.FcPaper,
    onSurface = MineradioColors.FcInk,
    surfaceVariant = MineradioColors.GlassDark,
    onSurfaceVariant = MineradioColors.FcInk2,
    outline = MineradioColors.FcHair2,
    outlineVariant = MineradioColors.FcHair,
    error = MineradioColors.Danger,
    onError = MineradioColors.FcInk,
)

@Composable
fun MineradioTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // 沉浸式全屏 + 透明状态栏，让 OpenGL 粒子场铺满
            WindowCompat.setDecorFitsSystemWindows(window, false)
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = false
            controller.isAppearanceLightNavigationBars = false
            controller.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            controller.show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
        }
    }
    MaterialTheme(
        colorScheme = MineradioColorScheme,
        typography = MineradioTypography,
        content = content,
    )
}
