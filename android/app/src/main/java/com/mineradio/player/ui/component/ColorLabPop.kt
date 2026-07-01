package com.mineradio.player.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mineradio.player.ui.theme.MineradioColors

/**
 * 色彩实验室 —— 复刻桌面版 #color-lab-pop。
 *
 * 提供 HSV 滑块 + 预设色板，选色后回调应用到 FxState 指定字段。
 * 目标字段由 colorLabTarget 决定（lyric/highlight/glow/tint/shelfAccent）。
 */
@Composable
fun ColorLabPop(
    initialColor: Color,
    target: String,
    onPick: (Color) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var hsv by remember(initialColor) {
        val arr = FloatArray(3)
        android.graphics.Color.RGBToHSV(
            (initialColor.red * 255).toInt(),
            (initialColor.green * 255).toInt(),
            (initialColor.blue * 255).toInt(),
            arr,
        )
        mutableStateOf(Triple(arr[0], arr[1], arr[2]))
    }
    val currentColor = remember(hsv) {
        Color(android.graphics.Color.HSVToColor(floatArrayOf(hsv.first, hsv.second, hsv.third)))
    }

    val targetLabel = when (target) {
        "lyric" -> "歌词主色"
        "highlight" -> "高亮色"
        "glow" -> "溢光色"
        "tint" -> "次级色"
        "shelfAccent" -> "歌单架描边"
        else -> "颜色"
    }

    DiyOverlayPanel(
        title = "色彩实验室 · $targetLabel",
        onClose = onDismiss,
        modifier = modifier,
    ) {
        Column {
            // 当前色预览
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(currentColor),
                contentAlignment = Alignment.Center,
            ) {
                val hex = String.format("#%06X", currentColor.toArgb() and 0xFFFFFF)
                Text(hex, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(12.dp))
            // 色相
            Text("色相 H", color = MineradioColors.FcMuted, fontSize = 11.sp)
            Slider(
                value = hsv.first,
                onValueChange = { hsv = Triple(it, hsv.second, hsv.third) },
                valueRange = 0f..360f,
                colors = SliderDefaults.colors(
                    thumbColor = MineradioColors.FcAccent,
                    activeTrackColor = MineradioColors.FcAccent,
                ),
            )
            Spacer(Modifier.height(4.dp))
            // 饱和度
            Text("饱和度 S", color = MineradioColors.FcMuted, fontSize = 11.sp)
            Slider(
                value = hsv.second,
                onValueChange = { hsv = Triple(hsv.first, it, hsv.third) },
                valueRange = 0f..1f,
                colors = SliderDefaults.colors(
                    thumbColor = MineradioColors.FcAccent,
                    activeTrackColor = MineradioColors.FcAccent,
                ),
            )
            Spacer(Modifier.height(4.dp))
            // 明度
            Text("明度 V", color = MineradioColors.FcMuted, fontSize = 11.sp)
            Slider(
                value = hsv.third,
                onValueChange = { hsv = Triple(hsv.first, hsv.second, it) },
                valueRange = 0f..1f,
                colors = SliderDefaults.colors(
                    thumbColor = MineradioColors.FcAccent,
                    activeTrackColor = MineradioColors.FcAccent,
                ),
            )
            Spacer(Modifier.height(12.dp))
            // 预设色板
            Text("预设", color = MineradioColors.FcMuted, fontSize = 11.sp)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    Color(0xFFA9B8C8), Color(0xFFFFF0B8), Color(0xFF9DB8CF),
                    Color(0xFF9CFFDF), Color(0xFFD6F8FF), Color(0xFFF4D28A),
                    Color(0xFFD95B67), Color(0xFF73A7FF),
                ).forEach { c ->
                    Box(
                        Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(c)
                            .clickable {
                                val arr = FloatArray(3)
                                android.graphics.Color.RGBToHSV(
                                    (c.red * 255).toInt(), (c.green * 255).toInt(), (c.blue * 255).toInt(), arr
                                )
                                hsv = Triple(arr[0], arr[1], arr[2])
                            }
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            // 应用按钮
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Button(
                    onClick = { onPick(currentColor); onDismiss() },
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
