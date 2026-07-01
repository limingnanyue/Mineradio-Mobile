package com.mineradio.player.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mineradio.player.ui.LyricLine
import com.mineradio.player.ui.fx.FxState
import com.mineradio.player.ui.theme.MineradioColors

/**
 * 歌词舞台 —— 复刻桌面版 #stage-lyrics：当前行高亮居中，上下行半透明渐隐。
 * 自动随播放进度滚动到当前行。
 *
 * DIY 配置驱动（对应桌面版 fx.lyricColorMode / lyricColor / lyricHighlightColor / lyricGlowColor /
 * lyricScale / lyricLetterSpacing / lyricLineHeight / lyricWeight / lyricFont）：
 *  - auto 模式：用默认雾蓝/暖奶油/银蓝配色；
 *  - custom 模式：用 FxState 自定义配色；
 *  - scale / letterSpacing / lineHeight / weight 实时影响渲染。
 */
@Composable
fun LyricStage(
    lines: List<LyricLine>,
    positionMs: Long,
    fx: FxState = FxState(),
    modifier: Modifier = Modifier,
) {
    if (lines.isEmpty()) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "暂无歌词",
                color = MineradioColors.FcMuted,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        return
    }

    // 找到当前行索引
    var currentIdx by remember(lines, positionMs) {
        mutableStateOf(findCurrentLine(lines, positionMs))
    }
    LaunchedEffect(positionMs, lines) {
        currentIdx = findCurrentLine(lines, positionMs)
    }

    // DIY 配色（auto / custom）
    val colors = fx.overlayColors()
    val primaryColor = colors.primary
    val highlightColor = colors.highlight

    // DIY 字体属性
    val activeSize = (22f * fx.lyricScale).sp
    val inactiveSize = (16f * fx.lyricScale).sp
    val activeWeight = FontWeight(fx.lyricWeight.coerceIn(100, 900))
    val inactiveWeight = FontWeight((fx.lyricWeight.coerceIn(100, 900) - 100).coerceAtLeast(100))
    val letterSpacing = fx.lyricLetterSpacing.sp
    val lineHeight = fx.lyricLineHeight
    val fontFamily = fontFamilyFor(fx.lyricFont)

    val listState = rememberLazyListState()
    LaunchedEffect(currentIdx) {
        if (currentIdx >= 0) {
            // 居中当前行
            val viewportCenter = listState.layoutInfo.viewportSize.height / 2
            listState.animateScrollToItem(currentIdx, scrollOffset = -viewportCenter / 3)
        }
    }

    Box(modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 120.dp, horizontal = 32.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            itemsIndexed(lines) { idx, line ->
                val isActive = idx == currentIdx
                Text(
                    text = line.text,
                    color = if (isActive) highlightColor else primaryColor,
                    fontSize = if (isActive) activeSize else inactiveSize,
                    fontWeight = if (isActive) activeWeight else inactiveWeight,
                    fontFamily = fontFamily,
                    letterSpacing = letterSpacing,
                    lineHeight = ((if (isActive) activeSize.value else inactiveSize.value) * lineHeight * 1.2f).sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.alpha(if (isActive) 1f else 0.45f),
                )
            }
        }
        // 上下渐隐遮罩（模拟桌面版歌词淡入淡出）
        Box(
            Modifier
                .fillMaxWidth()
                .height(120.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(MineradioColors.FcBg, Color.Transparent)
                    )
                )
        )
        Box(
            Modifier
                .fillMaxWidth()
                .height(120.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, MineradioColors.FcBg)
                    )
                )
        )
    }
}

/** 桌面版 9 种字体名映射到 Compose FontFamily。 */
private fun fontFamilyFor(name: String): FontFamily = when (name) {
    "heiti" -> FontFamily.SansSerif
    "songti", "cubsong", "shiyin", "kaisong", "serif" -> FontFamily.Serif
    "gothic", "editorial" -> FontFamily.SansSerif
    else -> FontFamily.Default
}

private fun findCurrentLine(lines: List<LyricLine>, positionMs: Long): Int {
    if (lines.isEmpty()) return -1
    for (i in lines.indices.reversed()) {
        if (lines[i].timeMs <= positionMs) return i
    }
    return 0
}
