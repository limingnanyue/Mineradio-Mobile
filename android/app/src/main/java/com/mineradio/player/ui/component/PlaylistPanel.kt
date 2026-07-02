package com.mineradio.player.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mineradio.player.data.api.dto.Playlist
import com.mineradio.player.data.api.dto.Podcast
import com.mineradio.player.data.api.dto.Song
import com.mineradio.player.ui.theme.MineradioColors

/**
 * 歌单/队列三 tab 面板 —— 1:1 复刻桌面版 #playlist-panel。
 *
 * 桌面版来源：public/index.html:2269-2306
 *   - queue-head：标题 + 常开 pin + 随机按钮
 *   - panel-tabs：当前队列 / 我的歌单 / 我的播客
 *   - queue-pane：播放模式 chip + 切换模式 + 清空 + 队列列表
 *   - pl-pane：网易云/QQ 歌单列表 + 刷新
 *   - podcast-pane：收藏/创建/喜欢 + 刷新 + 播客列表
 *
 * 移动端适配为右侧浮层面板，可常驻或临时弹出。
 */
@Composable
fun PlaylistPanel(
    visible: Boolean,
    tab: String,
    pinned: Boolean,
    queue: List<Song>,
    currentIndex: Int,
    playlists: List<Playlist>,
    podcasts: List<Podcast>,
    onSwitchTab: (String) -> Unit,
    onTogglePinned: () -> Unit,
    onShuffle: () -> Unit,
    onClearQueue: () -> Unit,
    onCyclePlayMode: () -> Unit,
    playModeLabel: String,
    onSongClick: (Song, List<Song>) -> Unit,
    onJumpTo: (Int) -> Unit = {},
    onRemove: (Int) -> Unit = {},
    onPlaylistClick: (Playlist) -> Unit,
    onRefreshPlaylists: () -> Unit,
    onRefreshPodcasts: () -> Unit,
    onPodcastClick: (Podcast) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut(), modifier = modifier) {
        GlassPanel(
            modifier = Modifier
                .width(360.dp)
                .fillMaxHeight()
                .padding(8.dp),
            cornerRadius = 24,
        ) {
            Column(Modifier.fillMaxSize().padding(12.dp)) {
                // ===== queue-head =====
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("歌单 / 队列", color = MineradioColors.FcInk, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("QUEUE · 鼠标移开自动隐藏", color = MineradioColors.FcMuted, fontSize = 10.sp)
                    }
                    // 常开 pin 按钮
                    IconButton(onClick = onTogglePinned, modifier = Modifier.size(28.dp)) {
                        Icon(
                            Icons.Filled.PushPin,
                            "常开歌单",
                            tint = if (pinned) MineradioColors.FcAccent else MineradioColors.FcInk2,
                            modifier = Modifier.size(15.dp),
                        )
                    }
                    // 随机按钮
                    TextButton(onClick = onShuffle, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) {
                        Text("随机", color = MineradioColors.FcInk2, fontSize = 11.sp)
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Filled.Close, "关闭", tint = MineradioColors.FcMuted, modifier = Modifier.size(16.dp))
                    }
                }
                Spacer(Modifier.height(6.dp))
                // ===== panel-tabs =====
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(MineradioColors.GlassDark),
                ) {
                    PanelTab("当前队列", tab == "queue", Modifier.weight(1f)) { onSwitchTab("queue") }
                    PanelTab("我的歌单", tab == "playlists", Modifier.weight(1f)) { onSwitchTab("playlists") }
                    PanelTab("我的播客", tab == "podcasts", Modifier.weight(1f)) { onSwitchTab("podcasts") }
                }
                Spacer(Modifier.height(8.dp))
                // ===== 内容区 =====
                when (tab) {
                    "playlists" -> PlaylistPane(
                        playlists = playlists,
                        onPlaylistClick = onPlaylistClick,
                        onRefresh = onRefreshPlaylists,
                    )
                    "podcasts" -> PodcastPane(
                        podcasts = podcasts,
                        onPodcastClick = onPodcastClick,
                        onRefresh = onRefreshPodcasts,
                    )
                    else -> QueuePane(
                        queue = queue,
                        currentIndex = currentIndex,
                        playModeLabel = playModeLabel,
                        onCyclePlayMode = onCyclePlayMode,
                        onClearQueue = onClearQueue,
                        onSongClick = onSongClick,
                        onJumpTo = onJumpTo,
                        onRemove = onRemove,
                    )
                }
            }
        }
    }
}

@Composable
private fun PanelTab(title: String, active: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (active) MineradioColors.FcAccent.copy(alpha = 0.16f) else Color.Transparent)
            .clickable { onClick() }
            .padding(vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            title,
            color = if (active) MineradioColors.FcAccent else MineradioColors.FcMuted,
            fontSize = 11.sp,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

/** 当前队列面板（对应桌面版 #queue-pane）。 */
@Composable
private fun QueuePane(
    queue: List<Song>,
    currentIndex: Int,
    playModeLabel: String,
    onCyclePlayMode: () -> Unit,
    onClearQueue: () -> Unit,
    onSongClick: (Song, List<Song>) -> Unit,
    onJumpTo: (Int) -> Unit = {},
    onRemove: (Int) -> Unit = {},
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        // play-mode-chip
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MineradioColors.GlassDark,
        ) {
            Text(
                playModeLabel,
                color = MineradioColors.FcAccent,
                fontSize = 10.sp,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            TextButton(onClick = onCyclePlayMode, contentPadding = PaddingValues(horizontal = 8.dp)) {
                Text("切换模式", color = MineradioColors.FcInk2, fontSize = 10.sp)
            }
            TextButton(onClick = onClearQueue, contentPadding = PaddingValues(horizontal = 8.dp)) {
                Text("清空", color = MineradioColors.FcInk2, fontSize = 10.sp)
            }
        }
    }
    Spacer(Modifier.height(6.dp))
    if (queue.isEmpty()) {
        Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
            Text("队列为空", color = MineradioColors.FcMuted, fontSize = 12.sp)
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.fillMaxSize()) {
            itemsIndexed(queue) { idx, song ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (idx == currentIndex) MineradioColors.FcAccent.copy(alpha = 0.12f) else Color.Transparent)
                        .clickable { onJumpTo(idx) }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "${idx + 1}.",
                        color = MineradioColors.FcMuted,
                        fontSize = 10.sp,
                        modifier = Modifier.width(22.dp),
                    )
                    Text(
                        song.name,
                        color = if (idx == currentIndex) MineradioColors.FcAccent else MineradioColors.FcInk2,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    // 移除按钮（对应桌面版队列行「×」按钮）
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onRemove(idx) }
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        Text("×", color = MineradioColors.FcMuted, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

/** 我的歌单面板（对应桌面版 #pl-pane）。 */
@Composable
private fun PlaylistPane(
    playlists: List<Playlist>,
    onPlaylistClick: (Playlist) -> Unit,
    onRefresh: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text("登录后显示网易云 / QQ 歌单", color = MineradioColors.FcMuted, fontSize = 10.sp)
        TextButton(onClick = onRefresh, contentPadding = PaddingValues(horizontal = 8.dp)) {
            Text("刷新", color = MineradioColors.FcInk2, fontSize = 10.sp)
        }
    }
    Spacer(Modifier.height(6.dp))
    if (playlists.isEmpty()) {
        Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
            Text("暂无歌单", color = MineradioColors.FcMuted, fontSize = 12.sp)
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.fillMaxSize()) {
            items(playlists) { pl ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onPlaylistClick(pl) }
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier.size(36.dp).clip(RoundedCornerShape(6.dp)).background(MineradioColors.GlassDark),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (!pl.coverImgUrl.isNullOrEmpty()) {
                            coil.compose.AsyncImage(
                                model = pl.coverImgUrl,
                                contentDescription = pl.name,
                                modifier = Modifier.fillMaxSize(),
                            )
                        } else {
                            Icon(Icons.Filled.Bookmarks, null, tint = MineradioColors.FcMuted, modifier = Modifier.size(16.dp))
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text(pl.name, color = MineradioColors.FcInk2, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${pl.trackCount} 首", color = MineradioColors.FcMuted, fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

/** 我的播客面板（对应桌面版 #podcast-pane）。 */
@Composable
private fun PodcastPane(
    podcasts: List<Podcast>,
    onPodcastClick: (Podcast) -> Unit,
    onRefresh: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text("收藏 / 创建 / 喜欢", color = MineradioColors.FcMuted, fontSize = 10.sp)
        TextButton(onClick = onRefresh, contentPadding = PaddingValues(horizontal = 8.dp)) {
            Text("刷新", color = MineradioColors.FcInk2, fontSize = 10.sp)
        }
    }
    Spacer(Modifier.height(6.dp))
    if (podcasts.isEmpty()) {
        Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
            Text("暂无播客", color = MineradioColors.FcMuted, fontSize = 12.sp)
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.fillMaxSize()) {
            items(podcasts) { pod ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onPodcastClick(pod) }
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier.size(36.dp).clip(RoundedCornerShape(6.dp)).background(MineradioColors.GlassDark),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (!pod.cover.isNullOrEmpty()) {
                            coil.compose.AsyncImage(
                                model = pod.cover,
                                contentDescription = pod.name,
                                modifier = Modifier.fillMaxSize(),
                            )
                        } else {
                            Icon(Icons.Filled.GraphicEq, null, tint = MineradioColors.FcMuted, modifier = Modifier.size(16.dp))
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text(pod.name, color = MineradioColors.FcInk2, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${pod.dj?.nickname ?: "未知"} · ${pod.programCount} 期", color = MineradioColors.FcMuted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}
