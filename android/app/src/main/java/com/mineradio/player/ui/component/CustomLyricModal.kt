package com.mineradio.player.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mineradio.player.ui.theme.MineradioColors

/**
 * 自定义歌词 LRC 编辑器 —— 复刻桌面版 #custom-lyric-modal。
 *
 * 用户可粘贴 LRC 时间轴文本（`[mm:ss.xx]歌词`）或纯文本歌词，
 * 保存后由 MainViewModel.parseLyric() 解析并应用到 LyricStage。
 */
@Composable
fun CustomLyricModal(
    text: String,
    onTextChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DiyOverlayPanel(
        title = "自定义歌词",
        onClose = onDismiss,
        modifier = modifier,
    ) {
        Column {
            // 提示
            Text(
                "粘贴 LRC 时间轴歌词（[mm:ss.xx]行内容）或纯文本，保存后立即应用",
                color = MineradioColors.FcMuted,
                fontSize = 11.sp,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            // 文本编辑区
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp),
                placeholder = {
                    Text(
                        "[00:12.34]夜空中最亮的星\n[00:18.56]能否听清\n[00:24.10]那仰望的人 心底的孤独和叹息",
                        color = MineradioColors.FcMuted,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                    )
                },
                textStyle = androidx.compose.ui.text.TextStyle(
                    color = MineradioColors.FcInk,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 18.sp,
                ),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = MineradioColors.FcAccent,
                    unfocusedBorderColor = MineradioColors.GlassDark,
                    cursorColor = MineradioColors.FcAccent,
                ),
                shape = RoundedCornerShape(12.dp),
            )
            Spacer(Modifier.height(12.dp))
            // 操作行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = { onTextChange("") }) {
                    Icon(Icons.Filled.Refresh, "清空", tint = MineradioColors.FcMuted, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("清空", color = MineradioColors.FcMuted, fontSize = 12.sp)
                }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = onSave,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MineradioColors.FcAccent,
                        contentColor = MineradioColors.ChillInk,
                    ),
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Icon(Icons.Filled.Check, "保存", modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("保存并应用", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
