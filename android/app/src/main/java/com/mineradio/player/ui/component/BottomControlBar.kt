package com.mineradio.player.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import com.mineradio.player.data.api.dto.Playlist
import com.mineradio.player.data.api.dto.Song
import com.mineradio.player.data.playback.PlayerController
import com.mineradio.player.ui.theme.MineradioColors
import java.util.Locale

/**
 * 底部控制条 —— 1:1 复刻桌面版 #bottom-bar 三段式结构（actions / transport / modes）。
 *
 * 桌面版来源：public/index.html:2403-2457
 *   actions 段：封面 / 标题 / 艺人 / 音质 pill / 喜欢 / 收藏
 *   transport 段：播放模式 / 上一曲 / 播放暂停 / 下一曲 / 迷你队列
 *   modes 段：歌词开关 / 音量(移动端省略) / 控制条自动隐藏 / 全沉浸式 / 全屏(移动端省略) / 时间显示
 *
 * 移动端适配：
 *  - 音量由系统音量键控制，省略 #volume-control；
 *  - 全屏等价于沉浸模式，已在 TopBar 与 DIY 浮区暴露，底栏不重复；
 *  - 其余控件全部保留，与桌面版一致。
 */
@Composable
fun BottomControlBar(
    state: PlayerController.PlaybackState,
    liked: Boolean,
    quality: String,
    showLyricsPanel: Boolean,
    controlsAutoHide: Boolean,
    immersiveMode: Boolean,
    onPlayPause: () -> Unit,
    onSkipPrev: () -> Unit,
    onSkipNext: () -> Unit,
    onSeek: (Long) -> Unit,
    onToggleLike: () -> Unit,
    onCollect: () -> Unit,
    onCyclePlayMode: () -> Unit,
    onToggleMiniQueue: () -> Unit,
    onToggleLyricsPanel: () -> Unit,
    onToggleControlsAutoHide: () -> Unit,
    onToggleImmersive: () -> Unit,
    onQualityChange: (String) -> Unit,
    onSongClick: (Song, List<Song>) -> Unit,
    queue: List<Song>,
    showMiniQueue: Boolean,
    collectPlaylists: List<Playlist>,
    showCollect: Boolean,
    onCollectToPlaylist: (Long) -> Unit,
    onDismissCollect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier) {
        // 迷你队列浮层（点 #mini-queue-btn 弹出，桌面版 #mini-queue-popover）
        AnimatedVisibility(
            visible = showMiniQueue,
            enter = fadeIn(), exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 96.dp),
        ) {
            MiniQueuePopover(
                queue = queue,
                currentIndex = state.queueIndex,
                onSongClick = { song -> onSongClick(song, queue) },
            )
        }
        // 收藏到歌单弹窗（桌面版 #collect-modal）
        AnimatedVisibility(
            visible = showCollect,
            enter = fadeIn(), exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center).padding(24.dp),
        ) {
            CollectToPlaylistModal(
                playlists = collectPlaylists,
                onPick = onCollectToPlaylist,
                onDismiss = onDismissCollect,
            )
        }

        GlassPanel(
            modifier = Modifier
                .fillMaxWidth()
                .height(88.dp),
            cornerRadius = 28,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // ===== actions 段 =====
                CoverThumb(state.current, Modifier.size(52.dp).clip(RoundedCornerShape(10.dp)))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = state.current?.name ?: "未在播放",
                        color = MineradioColors.FcInk,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = state.current?.displayArtist ?: "",
                        color = MineradioColors.FcMuted,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                // 音质 pill（#quality-control）
                QualityPill(quality = quality, onChange = onQualityChange)
                Spacer(Modifier.width(6.dp))
                // 喜欢（#heart-btn）
                IconButton(onClick = onToggleLike, modifier = Modifier.size(40.dp)) {
                    Icon(
                        if (liked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        "喜欢",
                        tint = if (liked) MineradioColors.SourceNetease else MineradioColors.FcInk2,
                    )
                }
                // 收藏到歌单（#collect-btn）
                IconButton(onClick = onCollect, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Filled.PlaylistAdd, "收藏到歌单", tint = MineradioColors.FcInk2)
                }

                Spacer(Modifier.width(12.dp))

                // ===== transport 段 =====
                // 播放模式（#play-mode-btn）
                IconButton(onClick = onCyclePlayMode, modifier = Modifier.size(40.dp)) {
                    Icon(
                        playModeIcon(state.repeatMode, state.shuffle),
                        "播放模式",
                        tint = if (state.repeatMode != Player.REPEAT_MODE_OFF || state.shuffle)
                            MineradioColors.FcAccent else MineradioColors.FcInk2,
                    )
                }
                IconButton(onClick = onSkipPrev, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Filled.SkipPrevious, "上一曲", tint = MineradioColors.FcInk2)
                }
                FloatingActionButton(
                    onClick = onPlayPause,
                    modifier = Modifier.size(52.dp),
                    shape = CircleShape,
                    containerColor = MineradioColors.FcAccent,
                    contentColor = MineradioColors.ChillInk,
                ) {
                    Icon(
                        if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        "播放/暂停",
                        modifier = Modifier.size(26.dp),
                    )
                }
                IconButton(onClick = onSkipNext, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Filled.SkipNext, "下一曲", tint = MineradioColors.FcInk2)
                }
                // 迷你队列（#mini-queue-btn）
                IconButton(onClick = onToggleMiniQueue, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Filled.QueueMusic, "当前队列", tint = MineradioColors.FcInk2)
                }

                Spacer(Modifier.width(12.dp))

                // ===== modes 段 =====
                // 歌词开关（.lyrics-toggle-btn「词」字）
                IconButton(onClick = onToggleLyricsPanel, modifier = Modifier.size(40.dp)) {
                    Icon(
                        Icons.Filled.Subtitles,
                        "歌词开关",
                        tint = if (showLyricsPanel) MineradioColors.FcAccent else MineradioColors.FcInk2,
                    )
                }
                // 控制条自动隐藏（#controls-hide-btn）
                IconButton(onClick = onToggleControlsAutoHide, modifier = Modifier.size(40.dp)) {
                    Icon(
                        if (controlsAutoHide) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        "控制条自动隐藏",
                        tint = if (controlsAutoHide) MineradioColors.FcAccent else MineradioColors.FcInk2,
                    )
                }
                // 全沉浸式（#immersive-btn）
                IconButton(onClick = onToggleImmersive, modifier = Modifier.size(40.dp)) {
                    Icon(
                        Icons.Filled.Fullscreen,
                        "全沉浸式",
                        tint = if (immersiveMode) MineradioColors.FcAccent else MineradioColors.FcInk2,
                    )
                }
                // 时间显示（#time-display）
                Text(
                    text = "${formatTime(state.positionMs)} / ${formatTime(state.durationMs)}",
                    color = MineradioColors.FcMuted,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }

            // 可拖拽进度条（桌面版 #progress-bar 含 fill + thumb）
            DraggableProgressBar(
                positionMs = state.positionMs,
                durationMs = state.durationMs,
                onSeek = onSeek,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .align(Alignment.BottomCenter),
            )
        }
    }
}

/** 播放模式图标：顺序 / 列表循环 / 单曲循环 / 随机。 */
private fun playModeIcon(repeatMode: Int, shuffle: Boolean): ImageVector = when {
    shuffle -> Icons.Filled.Shuffle
    repeatMode == Player.REPEAT_MODE_ONE -> Icons.Filled.RepeatOne
    repeatMode == Player.REPEAT_MODE_ALL -> Icons.Filled.Repeat
    else -> Icons.Filled.Repeat
}

/** 音质 pill（对应桌面版 #quality-control 弹出档位）。 */
@Composable
private fun QualityPill(quality: String, onChange: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val label = qualityLabel(quality)
    Box {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MineradioColors.GlassDark,
            modifier = Modifier.clickable { expanded = true },
        ) {
            Row(
                Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(label, color = MineradioColors.FcAccent, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Icon(Icons.Filled.ArrowDropDown, null, tint = MineradioColors.FcInk2, modifier = Modifier.size(14.dp))
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            listOf(
                "master" to "超清母带",
                "hires" to "高清臻音",
                "lossless" to "无损 SQ",
                "hq" to "极高 HQ",
                "sq" to "标准",
                "auto" to "自动",
            ).forEach { (k, v) ->
                DropdownMenuItem(
                    text = { Text(v, color = if (quality == k) MineradioColors.FcAccent else MineradioColors.FcInk) },
                    onClick = { onChange(k); expanded = false },
                )
            }
        }
    }
}

private fun qualityLabel(quality: String): String = when (quality) {
    "master" -> "母带"
    "hires" -> "臻音"
    "lossless" -> "SQ"
    "hq" -> "HQ"
    "sq" -> "标准"
    else -> "自动"
}

/** 可拖拽进度条（桌面版 #progress-fill + #progress-thumb）。 */
@Composable
private fun DraggableProgressBar(
    positionMs: Long,
    durationMs: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var dragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableStateOf(0f) }
    val progress = if (durationMs > 0) {
        (if (dragging) dragValue else positionMs.toFloat() / durationMs).coerceIn(0f, 1f)
    } else 0f

    Box(modifier.height(3.dp)) {
        // 槽
        Box(Modifier.fillMaxSize().background(MineradioColors.GlassDarker.copy(alpha = 0.6f)))
        // 填充
        Box(
            Modifier
                .fillMaxWidth(progress)
                .fillMaxHeight()
                .background(MineradioColors.FcAccent),
        )
        // 手势层（加大触摸热区到 20dp 高）
        Box(
            Modifier
                .fillMaxWidth()
                .height(20.dp)
                .align(Alignment.Center)
                .pointerInput(durationMs) {
                    androidx.compose.foundation.gestures.detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            if (durationMs > 0) {
                                dragging = true
                                dragValue = (offset.x / size.width).coerceIn(0f, 1f)
                            }
                        },
                        onDragEnd = {
                            if (dragging && durationMs > 0) {
                                onSeek((dragValue * durationMs).toLong())
                            }
                            dragging = false
                        },
                        onDragCancel = { dragging = false },
                        onHorizontalDrag = { change, _ ->
                            if (durationMs > 0) {
                                dragValue = (change.position.x / size.width).coerceIn(0f, 1f)
                            }
                            change.consume()
                        },
                    )
                },
        )
    }
}

/** 迷你队列浮层（桌面版 #mini-queue-popover）。 */
@Composable
private fun MiniQueuePopover(
    queue: List<Song>,
    currentIndex: Int,
    onSongClick: (Song) -> Unit,
) {
    GlassPanel(
        modifier = Modifier
            .width(360.dp)
            .heightIn(max = 360.dp),
        cornerRadius = 20,
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(
                "当前队列（${queue.size}）",
                color = MineradioColors.FcInk,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            LazyColumn(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                items(queue) { song ->
                    val idx = queue.indexOf(song)
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (idx == currentIndex) MineradioColors.FcAccent.copy(alpha = 0.12f) else Color.Transparent)
                            .clickable { onSongClick(song) }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "${idx + 1}.",
                            color = MineradioColors.FcMuted,
                            fontSize = 11.sp,
                            modifier = Modifier.width(24.dp),
                        )
                        Text(
                            song.name,
                            color = if (idx == currentIndex) MineradioColors.FcAccent else MineradioColors.FcInk2,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

/** 收藏到歌单弹窗（桌面版 #collect-modal）。 */
@Composable
private fun CollectToPlaylistModal(
    playlists: List<Playlist>,
    onPick: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    GlassPanel(
        modifier = Modifier
            .width(360.dp)
            .heightIn(max = 420.dp),
        cornerRadius = 24,
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("收藏到歌单", color = MineradioColors.FcInk, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Filled.Close, "关闭", tint = MineradioColors.FcInk2)
                }
            }
            Spacer(Modifier.height(8.dp))
            if (playlists.isEmpty()) {
                Text("没有可用歌单", color = MineradioColors.FcMuted, fontSize = 12.sp, modifier = Modifier.padding(16.dp))
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    items(playlists) { pl ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onPick(pl.id) }
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Filled.Bookmarks, null, tint = MineradioColors.FcMuted, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                pl.name,
                                color = MineradioColors.FcInk2,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CoverThumb(song: Song?, modifier: Modifier = Modifier) {
    Box(
        modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MineradioColors.GlassDark),
        contentAlignment = Alignment.Center,
    ) {
        if (song != null && song.displayCover.isNotEmpty()) {
            coil.compose.AsyncImage(
                model = song.displayCover,
                contentDescription = song.name,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Icon(Icons.Filled.MusicNote, null, tint = MineradioColors.FcMuted)
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSec = (ms / 1000).coerceAtLeast(0)
    val m = totalSec / 60
    val s = totalSec % 60
    return String.format(Locale.US, "%d:%02d", m, s)
}
