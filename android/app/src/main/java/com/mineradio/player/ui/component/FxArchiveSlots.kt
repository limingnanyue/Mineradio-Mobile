package com.mineradio.player.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mineradio.player.ui.fx.FxArchiveSlot
import com.mineradio.player.ui.theme.MineradioColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * FX 存档槽位网格 —— 复刻桌面版 .user-archive-grid / .user-archive-slot
 * （index.html:1100-1106, renderUserFxArchives 20431）。
 *
 * 每个槽位显示存档名 + 保存时间，「保存」按钮把当前 FX 配置拍快照存入（glass-saved-button 纹理）。
 * has-save 时边框变青色高亮（.has-save：border rgba(fc-accent,.24), bg rgba(fc-accent,.040)）。
 */
@Composable
fun FxArchiveSlots(
    slots: List<FxArchiveSlot>,
    onSave: (Int) -> Unit,
    onLoad: (Int) -> Unit,
    onExport: (Int) -> Unit = {},
    onImport: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val df = remember { SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()) }
    Column(modifier = modifier.fillMaxWidth().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("FX 存档", color = MineradioColors.FcMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            // 导入按钮（对应桌面版 importUserFxArchiveFromDialog）
            Box(
                Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onImport() }
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Text("导入", color = MineradioColors.FcAccent, fontSize = 10.sp)
            }
        }
        Spacer(Modifier.height(10.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(slots) { slot ->
                FxArchiveCard(
                    slot, df,
                    onSave = { onSave(slot.index) },
                    onLoad = { onLoad(slot.index) },
                    onExport = { onExport(slot.index) },
                )
            }
        }
    }
}

@Composable
private fun FxArchiveCard(
    slot: FxArchiveSlot,
    df: SimpleDateFormat,
    onSave: () -> Unit,
    onLoad: () -> Unit,
    onExport: () -> Unit = {},
) {
    val accent = slot.hasSave
    Box(
        Modifier
            .width(160.dp)
            .height(86.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (accent) MineradioColors.FcAccent.copy(alpha = 0.040f) else Color(0x07FFFFFF))
            .border(
                width = 1.dp,
                color = if (accent) MineradioColors.FcAccent.copy(alpha = 0.24f) else Color(0x12FFFFFF),
                shape = RoundedCornerShape(12.dp),
            )
            .padding(10.dp),
    ) {
        Column {
            Text(slot.name, color = MineradioColors.FcInk, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text(
                if (slot.hasSave) "已保存 ${df.format(Date(slot.savedAt))}" else "空存档",
                color = MineradioColors.FcMuted,
                fontSize = 10.sp,
            )
            Spacer(Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                // 保存按钮（glass-saved-button 纹理）
                GlassSavedButton(
                    onClick = onSave,
                    cornerRadius = 8,
                    modifier = Modifier.height(28.dp),
                ) {
                    Text("保存", color = MineradioColors.FcInk, fontSize = 11.sp)
                }
                if (slot.hasSave) {
                    GlassSavedButton(
                        onClick = onLoad,
                        cornerRadius = 8,
                        modifier = Modifier.height(28.dp),
                    ) {
                        Text("加载", color = MineradioColors.FcAccent, fontSize = 11.sp)
                    }
                    // 导出按钮（对应桌面版 exportUserFxArchive）
                    GlassSavedButton(
                        onClick = onExport,
                        cornerRadius = 8,
                        modifier = Modifier.height(28.dp),
                    ) {
                        Text("导出", color = MineradioColors.FcMuted, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}
