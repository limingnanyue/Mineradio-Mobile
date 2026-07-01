package com.mineradio.player.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mineradio.player.ui.theme.MineradioColors

/**
 * 后端设置面板 —— 让用户填自建后端地址。
 * 对应移动端方案中「应用内设置入口」。
 */
@Composable
fun SettingsPanel(
    currentBackend: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var input by remember(currentBackend) { mutableStateOf(currentBackend) }
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MineradioColors.FcBg.copy(alpha = 0.75f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        GlassPanel(
            modifier = Modifier
                .padding(24.dp)
                .width(IntrinsicSize.Min)
                .clickable(enabled = false) {},
            cornerRadius = 24,
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 24.dp, vertical = 22.dp)
                    .widthIn(min = 360.dp),
            ) {
                Text("Mineradio 设置", color = MineradioColors.FcInk, fontSize = 18.sp)
                Spacer(Modifier.height(16.dp))
                Text("后端地址", color = MineradioColors.FcMuted, fontSize = 12.sp)
                Spacer(Modifier.height(6.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MineradioColors.GlassDark)
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    BasicTextField(
                        value = input,
                        onValueChange = { input = it },
                        singleLine = true,
                        textStyle = TextStyle(color = MineradioColors.FcInk, fontSize = 14.sp),
                        cursorBrush = SolidColor(MineradioColors.FcAccent),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "填写自建后端（如 https://mineradio.example.com），不要带末尾斜杠。留空则使用本机内置服务。",
                    color = MineradioColors.FcMuted,
                    fontSize = 11.sp,
                )
                Spacer(Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onDismiss) { Text("取消", color = MineradioColors.FcMuted) }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { onSave(input.trim()) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MineradioColors.FcAccent,
                            contentColor = MineradioColors.ChillInk,
                        ),
                    ) { Text("保存并刷新") }
                }
            }
        }
    }
}
