package com.mineradio.player.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mineradio.player.ui.theme.MineradioColors
import kotlin.math.roundToInt

/**
 * 封面取色弹层 —— 复刻桌面版 .cover-color-pop（index.html:1137-1151）。
 *
 * 功能：
 *  1. 显示当前曲目封面，点击任意像素取色（对应桌面版 crosshair cursor + loupe）
 *  2. 自动提取封面主色板（最多 6 个量化色，对应桌面版 .cover-color-swatches 5 列）
 *  3. 当前选中色预览条 + 十六进制值
 *  4. 应用按钮回调到 FxState 指定字段
 *
 * 取色算法：把封面缩放到 48x48，按 4bit/通道量化分桶，取频次最高且饱和度≥0.25 的 6 个色。
 */
@Composable
fun CoverColorPop(
    bitmap: android.graphics.Bitmap?,
    target: String,
    onPick: (Color) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // 自动提取的主色板（封面加载后计算一次）
    var palette by remember(bitmap) { mutableStateOf<List<Color>>(emptyList()) }
    var picked by remember { mutableStateOf<Color?>(null) }

    LaunchedEffect(bitmap) {
        palette = if (bitmap != null) extractPalette(bitmap) else emptyList()
        picked = palette.firstOrNull()
    }

    val targetLabel = when (target) {
        "lyric" -> "歌词主色"
        "highlight" -> "高亮色"
        "glow" -> "溢光色"
        "tint" -> "次级色"
        "shelfAccent" -> "歌单架描边"
        else -> "封面取色"
    }

    DiyOverlayPanel(
        title = "封面取色 · $targetLabel",
        onClose = onDismiss,
        modifier = modifier,
    ) {
        Column {
            if (bitmap == null) {
                Text("封面未加载", color = MineradioColors.FcMuted, fontSize = 13.sp, modifier = Modifier.padding(24.dp))
                return@Column
            }
            // 封面预览 + 点击取色（对应桌面版 .cover-color-art，crosshair）
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(Color(0x1AFFFFFF)),
                contentAlignment = Alignment.Center,
            ) {
                val imageBmp = remember(bitmap) { bitmap.asImageBitmap() }
                Canvas(
                    Modifier
                        .fillMaxSize()
                        .pointerInput(bitmap) {
                            detectTapGestures { offset ->
                                val x = (offset.x / size.width * bitmap.width).roundToInt()
                                    .coerceIn(0, bitmap.width - 1)
                                val y = (offset.y / size.height * bitmap.height).roundToInt()
                                    .coerceIn(0, bitmap.height - 1)
                                picked = Color(bitmap.getPixel(x, y))
                            }
                        },
                ) {
                    val dstW = size.width.roundToInt()
                    val dstH = size.height.roundToInt()
                    drawImage(
                        image = imageBmp,
                        dstOffset = IntOffset(0, 0),
                        dstSize = IntSize(dstW, dstH),
                    )
                }
                // 提示
                Text(
                    "点击封面任意位置取色",
                    color = MineradioColors.FcMuted,
                    fontSize = 10.sp,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(8.dp),
                )
            }
            Spacer(Modifier.height(10.dp))
            // 当前选中色预览（对应桌面版 .cover-color-preview）
            val cur = picked ?: Color(0xFF9DB8CF)
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(cur),
                contentAlignment = Alignment.Center,
            ) {
                val hex = String.format("#%06X", cur.toArgb() and 0xFFFFFF)
                Text(hex, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(10.dp))
            Text("封面主色板", color = MineradioColors.FcMuted, fontSize = 11.sp)
            Spacer(Modifier.height(6.dp))
            // 主色板（对应桌面版 .cover-color-swatches 5 列）
            if (palette.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    palette.take(6).forEach { c ->
                        val selected = picked == c
                        Box(
                            Modifier
                                .weight(1f)
                                .height(26.dp)
                                .clip(RoundedCornerShape(7.dp))
                                .background(c)
                                .clickable { picked = c }
                                .then(
                                    if (selected) Modifier.padding(2.dp) else Modifier
                                ),
                        )
                    }
                }
            } else {
                Text("未能提取主色", color = MineradioColors.FcMuted, fontSize = 11.sp)
            }
            Spacer(Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Button(
                    onClick = { picked?.let { onPick(it); onDismiss() } },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MineradioColors.FcAccent,
                        contentColor = MineradioColors.ChillInk,
                    ),
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Text("应用", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

/**
 * 从 Bitmap 提取主色板 —— 缩放到 48x48，按 4bit/通道量化分桶，取频次最高的 6 个 vibrant 色。
 * 对应桌面版 extractCoverPalette 的简化实现（不依赖 androidx.palette 库）。
 */
private fun extractPalette(src: android.graphics.Bitmap): List<Color> {
    return runCatching {
        val scaled = android.graphics.Bitmap.createScaledBitmap(src, 48, 48, true)
        val buckets = HashMap<Int, Int>() // quantized color -> count
        for (y in 0 until scaled.height) {
            for (x in 0 until scaled.width) {
                val p = scaled.getPixel(x, y)
                // 4bit/通道量化：每通道右移 4 位再左移 4 位
                val r = (p shr 16 and 0xFF) and 0xF0
                val g = (p shr 8 and 0xFF) and 0xF0
                val b = (p and 0xFF) and 0xF0
                val key = (r shl 16) or (g shl 8) or b
                buckets[key] = (buckets[key] ?: 0) + 1
            }
        }
        // 转为 HSV 过滤过低饱和度（灰白）的桶，按频次降序取前 6
        buckets.entries
            .map { e ->
                val r = (e.key shr 16 and 0xFF) / 255f
                val g = (e.key shr 8 and 0xFF) / 255f
                val b = (e.key and 0xFF) / 255f
                val hsv = FloatArray(3)
                android.graphics.Color.RGBToHSV(
                    (r * 255).toInt(), (g * 255).toInt(), (b * 255).toInt(), hsv
                )
                Triple(e.key, e.value, hsv[1]) // key, count, saturation
            }
            .filter { it.third >= 0.18f } // 饱和度阈值
            .sortedByDescending { it.second }
            .take(6)
            .map { Color(it.first or 0xFF000000.toInt()) }
    }.getOrDefault(emptyList())
}
