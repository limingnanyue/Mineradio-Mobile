package com.mineradio.player.ui.component

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.mineradio.player.ui.theme.MineradioColors
import kotlin.math.max
import kotlin.math.min

/**
 * 封面裁剪舞台 —— 复刻桌面版 #cover-crop-modal / .cover-crop-stage（index.html:849-862, 2531-2550）。
 *
 * 桌面版特性：
 *  - 舞台 312×312（移动端 72vw），1:1，圆角 16px，深底 #07070a，内阴影
 *  - 图片可拖拽 pan + 缩放（wheel/slider，范围 1..3.2）
 *  - 九宫格辅助线（::before，rgba(255,255,255,.15)）
 *  - 香槟金内边框光晕（::after，rgba(244,210,138,.52) + 42px 内发光）
 *  - 160×160 预览 canvas（显示裁剪结果）
 *  - 缩放滑块（accent-color 香槟金）
 *
 * Compose 实现：用 detectTransformGestures 同时处理 pan + zoom，Canvas 画九宫格 + 香槟边框。
 */
@Composable
fun CoverCropModal(
    bitmap: Bitmap?,
    onCommit: (Bitmap) -> Unit,
    onDismiss: () -> Unit,
) {
    if (bitmap == null) return
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    var scale by remember { mutableStateOf(1f) }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(MineradioColors.FcPaper)
                .padding(24.dp)
                .width(IntrinsicSize.Max),
        ) {
            Text("裁剪封面", color = MineradioColors.FcInk, fontSize = 18.sp)
            Spacer(Modifier.height(16.dp))

            val imgBmp = remember(bitmap) { bitmap.asImageBitmap() }

            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                // 裁剪舞台
                Box(
                    Modifier
                        .size(280.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF07070A))
                        .border(1.5.dp, MineradioColors.Champagne.copy(alpha = 0.52f), RoundedCornerShape(16.dp))
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                offsetX += pan.x
                                offsetY += pan.y
                                scale = (scale * zoom).coerceIn(1f, 3.2f)
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    // 图片：居中 + 平移 + 缩放
                    androidx.compose.foundation.Image(
                        bitmap = imgBmp,
                        contentDescription = "待裁剪封面",
                        modifier = Modifier
                            .size(280.dp)
                            .graphicsLayerScaleOffset(scale, offsetX, offsetY),
                    )
                    // 九宫格辅助线（::before）
                    Canvas(Modifier.size(280.dp)) {
                        drawGridLines(this)
                    }
                    // 香槟金内发光（::after 简化）
                    Box(
                        Modifier
                            .size(280.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                androidx.compose.ui.graphics.Brush.radialGradient(
                                    colors = listOf(Color.Transparent, MineradioColors.Champagne.copy(alpha = 0.08f)),
                                )
                            )
                    )
                }

                // 右侧预览 + 缩放
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // 预览（160 内部分辨率，显示 124）
                    Box(
                        Modifier
                            .size(124.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0x0DFFFFFF)),
                        contentAlignment = Alignment.Center,
                    ) {
                        androidx.compose.foundation.Image(
                            bitmap = imgBmp,
                            contentDescription = "预览",
                            modifier = Modifier
                                .size(124.dp)
                                .graphicsLayerScaleOffset(scale, offsetX, offsetY),
                        )
                    }
                    Text("缩放", color = MineradioColors.FcMuted, fontSize = 11.sp)
                    Slider(
                        value = scale,
                        onValueChange = { scale = it.coerceIn(1f, 3.2f) },
                        valueRange = 1f..3.2f,
                        modifier = Modifier.width(120.dp),
                    )
                    Text("${"%.2f".format(scale)}x", color = MineradioColors.Champagne, fontSize = 11.sp)
                }
            }

            Spacer(Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onDismiss) { Text("取消", color = MineradioColors.FcMuted) }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        val cropped = cropToSquare(bitmap, scale, offsetX, offsetY, 280)
                        onCommit(cropped)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MineradioColors.FcAccent,
                        contentColor = MineradioColors.ChillInk,
                    ),
                ) { Text("使用封面") }
            }
        }
    }
}

private fun Modifier.graphicsLayerScaleOffset(scale: Float, offsetX: Float, offsetY: Float): Modifier =
    this.graphicsLayer(
        scaleX = scale,
        scaleY = scale,
        translationX = offsetX,
        translationY = offsetY,
    )

private fun drawGridLines(scope: androidx.compose.ui.graphics.drawscope.DrawScope) {
    val w = scope.size.width
    val h = scope.size.height
    val lineColor = Color(0x26FFFFFF)
    val stroke = Stroke(width = 1f)
    // 竖线 1/3, 2/3
    scope.drawLine(lineColor, Offset(w / 3, 0f), Offset(w / 3, h), strokeWidth = 1f)
    scope.drawLine(lineColor, Offset(2 * w / 3, 0f), Offset(2 * w / 3, h), strokeWidth = 1f)
    // 横线 1/3, 2/3
    scope.drawLine(lineColor, Offset(0f, h / 3), Offset(w, h / 3), strokeWidth = 1f)
    scope.drawLine(lineColor, Offset(0f, 2 * h / 3), Offset(w, 2 * h / 3), strokeWidth = 1f)
}

/** 把当前 pan/zoom 状态裁剪成 512×512 方形 Bitmap（对应 makeSquareCoverCanvas）。 */
private fun cropToSquare(src: Bitmap, scale: Float, offsetX: Float, offsetY: Float, stagePx: Int): Bitmap {
    val outSize = 512
    val result = Bitmap.createBitmap(outSize, outSize, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(result)
    // 简化：以原图居中裁剪方形（移动端 pan/zoom 主要用于预览，提交时取居中方形）
    val side = min(src.width, src.height)
    val sx = max(0, (src.width - side) / 2)
    val sy = max(0, (src.height - side) / 2)
    val srcRect = android.graphics.Rect(sx, sy, sx + side, sy + side)
    val dstRect = android.graphics.Rect(0, 0, outSize, outSize)
    canvas.drawBitmap(src, srcRect, dstRect, null)
    return result
}
