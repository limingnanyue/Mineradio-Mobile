package com.mineradio.player.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.mineradio.player.data.stats.ListenSummary
import com.mineradio.player.data.stats.ListenRecord
import com.mineradio.player.ui.theme.MineradioColors

/**
 * 听歌画像弹层 —— 复刻桌面版 #profile-modal（index.html 渲染 homeListenSummary）。
 *
 * 内容：
 *  1. 顶部汇总卡片：总播放次数 / 总收听分钟 / 最常听歌手 / 最常听曲目
 *  2. 最近播放列表（最多 30 条，可滚动）
 */
@Composable
fun ListenProfileModal(
    summary: ListenSummary?,
    recent: List<ListenRecord>,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DiyOverlayPanel(
        title = "听歌画像",
        onClose = onDismiss,
        modifier = modifier,
    ) {
        Column(Modifier.heightIn(max = 520.dp)) {
            // 顶部汇总卡片
            SummaryCard(summary)
            Spacer(Modifier.height(14.dp))
            Text(
                "最近播放",
                color = MineradioColors.FcMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            if (recent.isEmpty()) {
                Text(
                    "还没有播放记录，听几首歌再来看看吧",
                    color = MineradioColors.FcMuted,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(vertical = 24.dp),
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(recent.take(30)) { r -> RecentRow(r) }
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(summary: ListenSummary?) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StatCell("总播放", "${summary?.totalPlays ?: 0}", "次", Modifier.weight(1f))
        StatCell("总时长", "${summary?.totalMinutes ?: 0}", "分钟", Modifier.weight(1f))
    }
    Spacer(Modifier.height(10.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        InfoCell(
            "最常听歌手",
            summary?.topArtist?.name ?: "—",
            "听过 ${summary?.topArtist?.plays ?: 0} 次",
            Modifier.weight(1f),
        )
        InfoCell(
            "最常听曲目",
            summary?.topSong?.name ?: "—",
            "听过 ${summary?.topSong?.plays ?: 0} 次",
            Modifier.weight(1f),
        )
    }
}

@Composable
private fun StatCell(label: String, value: String, unit: String, modifier: Modifier = Modifier) {
    Box(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0x0FFFFFFF))
            .padding(14.dp),
    ) {
        Column {
            Text(label, color = MineradioColors.FcMuted, fontSize = 10.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, color = MineradioColors.FcInk, fontSize = 24.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.width(3.dp))
                Text(unit, color = MineradioColors.FcMuted, fontSize = 11.sp, modifier = Modifier.padding(bottom = 3.dp))
            }
        }
    }
}

@Composable
private fun InfoCell(label: String, title: String, sub: String, modifier: Modifier = Modifier) {
    Box(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0x0FFFFFFF))
            .padding(14.dp),
    ) {
        Column {
            Text(label, color = MineradioColors.FcMuted, fontSize = 10.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(6.dp))
            Text(title, color = MineradioColors.FcInk, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(2.dp))
            Text(sub, color = MineradioColors.FcInk2, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun RecentRow(r: ListenRecord) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0x08FFFFFF))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (r.cover.isNotEmpty()) {
            AsyncImage(
                model = r.cover,
                contentDescription = r.name,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp)),
            )
            Spacer(Modifier.width(10.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(r.name, color = MineradioColors.FcInk, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${r.artist} · ${(r.listenMs / 60000L)}分钟", color = MineradioColors.FcMuted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
