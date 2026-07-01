package com.mineradio.player.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.NavigateNext
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mineradio.player.ui.theme.MineradioColors

/**
 * 视觉引导 —— 复刻桌面版 #visual-guide（index.html:879-902, 20130-20160）。
 *
 * 桌面版逻辑：首次进入时弹出全屏遮罩，分步介绍 DIY / 3D 架 / 色彩实验室 / 自定义歌词 等入口，
 * 用户点「下一步」推进，「跳过」或「我知道了」结束并写入 visualGuideSeen 标记。
 *
 * 这里用分步卡片实现（移动端横屏）：中央玻璃卡片显示当前步骤的标题/说明/示意图标，
 * 底部进度点 + 下一步/跳过按钮。
 */
@Composable
fun VisualGuide(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var step by remember { mutableStateOf(0) }
    val steps = remember { GuideSteps.list }
    val current = steps[step.coerceIn(0, steps.lastIndex)]
    val isLast = step >= steps.lastIndex

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MineradioColors.FcBg.copy(alpha = 0.82f))
            .clickable(enabled = false) {},
        contentAlignment = Alignment.Center,
    ) {
        GlassPanel(
            modifier = Modifier
                .padding(32.dp)
                .widthIn(max = 460.dp),
            cornerRadius = 24,
        ) {
            Column(Modifier.padding(24.dp)) {
                // 顶部进度
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    steps.forEachIndexed { i, _ ->
                        Box(
                            Modifier
                                .padding(horizontal = 3.dp)
                                .size(if (i == step) 8.dp else 6.dp)
                                .clip(CircleShape)
                                .background(
                                    if (i == step) MineradioColors.FcAccent
                                    else MineradioColors.FcMuted.copy(alpha = 0.4f)
                                )
                        )
                    }
                }
                Spacer(Modifier.height(20.dp))
                // 示意图标圆
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .align(Alignment.CenterHorizontally)
                        .clip(CircleShape)
                        .background(MineradioColors.FcAccent.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        current.icon, current.title,
                        tint = MineradioColors.FcAccent,
                        modifier = Modifier.size(32.dp),
                    )
                }
                Spacer(Modifier.height(16.dp))
                // 标题
                Text(
                    current.title,
                    color = MineradioColors.FcInk,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
                Spacer(Modifier.height(8.dp))
                // 说明
                Text(
                    current.desc,
                    color = MineradioColors.FcMuted,
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
                Spacer(Modifier.height(24.dp))
                // 操作行
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("跳过", color = MineradioColors.FcMuted, fontSize = 12.sp)
                    }
                    Button(
                        onClick = {
                            if (isLast) onDismiss()
                            else step++
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MineradioColors.FcAccent,
                            contentColor = MineradioColors.ChillInk,
                        ),
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Text(
                            if (isLast) "我知道了" else "下一步",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            if (isLast) Icons.Filled.CheckCircle else Icons.Filled.NavigateNext,
                            null,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
        }
    }
}

/** 引导步骤数据。 */
private data class GuideStep(
    val icon: ImageVector,
    val title: String,
    val desc: String,
)

private object GuideSteps {
    val list: List<GuideStep> = listOf(
        GuideStep(
            icon = Icons.Filled.Tune,
            title = "DIY 视觉控制台",
            desc = "点击右上角 DIY 按钮打开控制台，可调节粒子星河、歌词字体、3D 歌单架等全部视觉参数。",
        ),
        GuideStep(
            icon = Icons.Filled.ViewCarousel,
            title = "3D 歌单架",
            desc = "顶栏的歌单架图标可进入 3D 浏览模式，OFF/SIDE/STAGE 三种摆放切换，支持缩放与旋转。",
        ),
        GuideStep(
            icon = Icons.Filled.Palette,
            title = "色彩实验室",
            desc = "在 DIY 控制台内可对歌词主色、高亮色、溢光色、歌单架描边等任意调色，HSV 滑块 + 预设色板。",
        ),
        GuideStep(
            icon = Icons.Filled.Edit,
            title = "自定义歌词",
            desc = "支持粘贴 LRC 时间轴歌词或纯文本，保存后立即应用到歌词舞台。",
        ),
        GuideStep(
            icon = Icons.Filled.Fullscreen,
            title = "沉浸模式",
            desc = "顶栏全屏图标进入沉浸模式，隐藏所有控件，仅保留粒子星河与歌词舞台。",
        ),
    )
}
