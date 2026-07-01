package com.mineradio.player.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.mineradio.player.data.api.dto.Podcast
import com.mineradio.player.data.api.dto.PodcastProgram
import com.mineradio.player.ui.component.GlassPanel
import com.mineradio.player.ui.theme.MineradioColors

/**
 * 播客面板 —— 复刻桌面版 #podcast-panel（index.html:20800-20860 loadPodcastHot/openPodcastPrograms）。
 *
 * 双视图：
 *  1. 热门播客网格（默认，loadPodcastHot limit=18）
 *  2. 节目列表（选中播客后，openPodcastPrograms 流程）
 *
 * 顶部带返回 / 关闭按钮。
 */
@Composable
fun PodcastPanel(
    hotPodcasts: List<Podcast>,
    selectedPodcast: Podcast?,
    programs: List<PodcastProgram>,
    onPodcastClick: (Podcast) -> Unit,
    onProgramClick: (PodcastProgram) -> Unit,
    onBack: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GlassPanel(modifier = modifier, cornerRadius = 28) {
        Column(Modifier.fillMaxSize()) {
            // 顶部标题栏
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (selectedPodcast != null) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回", tint = MineradioColors.FcInk2)
                    }
                } else {
                    Icon(Icons.Filled.GraphicEq, null, tint = MineradioColors.Champagne, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        selectedPodcast?.name ?: "播客 / DJ",
                        color = MineradioColors.FcInk,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        if (selectedPodcast != null) "${programs.size} 期节目"
                        else "${hotPodcasts.size} 个热门播客",
                        color = MineradioColors.FcMuted,
                        fontSize = 11.sp,
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, "关闭", tint = MineradioColors.FcInk2)
                }
            }
            Box(Modifier.height(1.dp).fillMaxWidth().background(MineradioColors.GlassDark))
            // 内容区
            if (selectedPodcast != null) {
                // 节目列表
                if (programs.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("加载节目中…", color = MineradioColors.FcMuted, fontSize = 13.sp)
                    }
                } else {
                    LazyColumn(Modifier.fillMaxSize()) {
                        items(programs) { p ->
                            ProgramRow(p, onClick = { onProgramClick(p) })
                        }
                    }
                }
            } else {
                // 热门播客网格（2 列）
                if (hotPodcasts.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("加载播客…", color = MineradioColors.FcMuted, fontSize = 13.sp)
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(hotPodcasts) { pc ->
                            PodcastCard(pc, onClick = { onPodcastClick(pc) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PodcastCard(pc: Podcast, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xA6101216))
            .border(1.dp, Color(0x14FFFFFF), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(10.dp),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(96.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MineradioColors.GlassDark),
            contentAlignment = Alignment.Center,
        ) {
            if (!pc.cover.isNullOrEmpty()) {
                AsyncImage(model = pc.cover, contentDescription = pc.name, modifier = Modifier.fillMaxSize())
            } else {
                Icon(Icons.Filled.GraphicEq, null, tint = MineradioColors.FcMuted, modifier = Modifier.size(28.dp))
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(pc.name, color = MineradioColors.FcInk, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(2.dp))
        Text(
            "${pc.dj?.nickname ?: "未知"} · ${pc.programCount} 期",
            color = MineradioColors.FcMuted,
            fontSize = 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ProgramRow(p: PodcastProgram, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MineradioColors.GlassDark),
            contentAlignment = Alignment.Center,
        ) {
            if (!p.cover.isNullOrEmpty()) {
                AsyncImage(model = p.cover, contentDescription = p.name, modifier = Modifier.fillMaxSize())
            } else {
                Icon(Icons.Filled.PlayArrow, null, tint = MineradioColors.FcMuted, modifier = Modifier.size(18.dp))
            }
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(p.name, color = MineradioColors.FcInk, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(2.dp))
            Text(
                "${formatDuration(p.duration)} · ${p.description?.take(20) ?: ""}",
                color = MineradioColors.FcMuted,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun formatDuration(ms: Long): String {
    if (ms <= 0) return "--:--"
    val s = ms / 1000
    return "%02d:%02d".format(s / 60, s % 60)
}
