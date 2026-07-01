package com.mineradio.player.ui.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mineradio.player.ui.theme.MineradioColors
import kotlin.math.abs
import kotlin.math.sin

/**
 * 本地节奏分析 —— 复刻桌面版 #local-beat-modal（index.html:2569-2592）。
 *
 * 桌面版双模式：
 *  - MR 分析：日常电影视角（默认）
 *  - DJ 分析：长混音/强节奏
 *
 * 选模式后点「开始分析」显示滚动波形，完成后给出估算 BPM。
 * 真实 BPM 估算需 MediaExtractor + 自相关，此处先落地 UI 与交互。
 */
@Composable
fun LocalBeatModal(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var mode by remember { mutableStateOf("mr") }       // mr / dj
    var analyzing by remember { mutableStateOf(false) }
    var done by remember { mutableStateOf(false) }
    var bpm by remember { mutableStateOf(0) }

    val transition = rememberInfiniteTransition(label = "beat")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (mode == "dj") 600 else 900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "phase",
    )

    DiyOverlayPanel(
        title = "本地节奏分析",
        onClose = onDismiss,
        modifier = modifier,
    ) {
        Column {
            // 曲目信息
            Text(
                "选择一种电影视角分析方式",
                color = MineradioColors.FcMuted,
                fontSize = 11.sp,
                modifier = Modifier.padding(bottom = 12.dp),
            )

            // MR / DJ 模式 tab
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                BeatModeTab(
                    title = "MR 分析",
                    sub = "日常电影视角",
                    selected = mode == "mr",
                    onClick = { mode = "mr" },
                    modifier = Modifier.weight(1f),
                )
                BeatModeTab(
                    title = "DJ 分析",
                    sub = "长混音/强节奏",
                    selected = mode == "dj",
                    onClick = { mode = "dj" },
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(16.dp))

            // 波形 / 节拍可视化区
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF07070A)),
                contentAlignment = Alignment.Center,
            ) {
                if (analyzing || done) {
                    Canvas(Modifier.fillMaxSize().padding(12.dp)) {
                        val w = size.width
                        val h = size.height
                        val mid = h / 2
                        val bars = if (mode == "dj") 64 else 48
                        val gap = w / bars
                        for (i in 0 until bars) {
                            val amp = (sin((i + phase * bars) * 0.45) +
                                    0.6 * sin((i + phase * bars) * 0.21)) * 0.5
                            val barH = (abs(amp) * (h * 0.42)).coerceAtLeast(2f)
                            val x = i * gap + gap / 4
                            val color = if (done) {
                                MineradioColors.FcAccent
                            } else {
                                MineradioColors.FcAccent.copy(alpha = 0.5f + 0.5f * abs(amp))
                            }
                            drawLine(
                                color = color,
                                start = Offset(x, mid - barH),
                                end = Offset(x, mid + barH),
                                strokeWidth = gap / 2.4f,
                                cap = StrokeCap.Round,
                            )
                        }
                        drawLine(
                            color = Color(0x33FFFFFF),
                            start = Offset(0f, mid),
                            end = Offset(w, mid),
                            strokeWidth = 1f,
                        )
                    }
                } else {
                    Icon(
                        Icons.Filled.GraphicEq, "等待分析",
                        tint = MineradioColors.FcMuted,
                        modifier = Modifier.size(48.dp),
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // BPM 结果
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("估算 BPM", color = MineradioColors.FcMuted, fontSize = 12.sp)
                Text(
                    if (done) "$bpm" else "—",
                    color = MineradioColors.Champagne,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                )
            }

            Spacer(Modifier.height(16.dp))

            // 分析按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(
                    onClick = {
                        if (done) {
                            analyzing = false
                            done = false
                            bpm = 0
                        } else {
                            analyzing = true
                            done = false
                        }
                    },
                    enabled = !analyzing,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MineradioColors.FcAccent,
                        contentColor = MineradioColors.ChillInk,
                    ),
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Icon(
                        if (done) Icons.Filled.Refresh else Icons.Filled.PlayArrow,
                        null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        when {
                            analyzing -> "分析中…"
                            done -> "重新分析"
                            else -> "开始分析"
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun BeatModeTab(
    title: String,
    sub: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) MineradioColors.FcAccent.copy(alpha = 0.14f) else Color(0x0DFFFFFF))
            .clickable(onClick = onClick)
            .padding(12.dp),
    ) {
        Text(
            title,
            color = if (selected) MineradioColors.FcAccent else MineradioColors.FcInk2,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(2.dp))
        Text(sub, color = MineradioColors.FcMuted, fontSize = 10.sp)
    }
}
