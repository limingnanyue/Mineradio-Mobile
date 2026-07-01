package com.mineradio.player.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.mineradio.player.data.api.dto.Playlist
import com.mineradio.player.data.api.dto.Song
import com.mineradio.player.ui.component.GlassPanel
import com.mineradio.player.ui.component.SongRow
import com.mineradio.player.ui.theme.MineradioColors

/**
 * 歌单详情 —— 复刻桌面版 .pl-inline-detail / .pl-detail（index.html:1319-1338, 19472-19512）。
 *
 * 结构：
 *  - 顶部 sticky header：左侧封面 + 右侧（歌单名 / 创建者 / 曲目数 / 播放全部按钮）
 *  - 下方曲目 LazyColumn（SongRow）
 *
 * 桌面版规则：歌单详情内不对单曲做喜欢/收藏（那些在队列面板里）。
 */
@Composable
fun PlaylistDetail(
    playlist: Playlist?,
    tracks: List<Song>,
    onPlayAll: () -> Unit,
    onSongClick: (Song) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GlassPanel(modifier = modifier, cornerRadius = 28) {
        Box(Modifier.fillMaxSize()) {
            LazyColumn(Modifier.fillMaxSize()) {
                // 顶部 header（sticky）
                item {
                    PlaylistHeader(
                        playlist = playlist,
                        trackCount = tracks.size,
                        onPlayAll = onPlayAll,
                        onBack = onBack,
                    )
                }
                // 曲目列表
                items(tracks) { song ->
                    SongRow(
                        song = song,
                        onClick = { onSongClick(song) },
                    )
                }
                if (tracks.isEmpty()) {
                    item {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .padding(40.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("加载中…", color = MineradioColors.FcMuted, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaylistHeader(
    playlist: Playlist?,
    trackCount: Int,
    onPlayAll: () -> Unit,
    onBack: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0x66050608), Color.Transparent),
                )
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回", tint = MineradioColors.FcInk2)
        }
        Spacer(Modifier.width(8.dp))
        // 封面
        Box(
            Modifier
                .size(96.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MineradioColors.GlassDark),
            contentAlignment = Alignment.Center,
        ) {
            val cover = playlist?.coverImgUrl ?: playlist?.picUrl
            if (!cover.isNullOrEmpty()) {
                AsyncImage(model = cover, contentDescription = playlist?.name, modifier = Modifier.fillMaxSize())
            } else {
                Icon(Icons.AutoMirrored.Filled.PlaylistPlay, null, tint = MineradioColors.FcMuted, modifier = Modifier.size(34.dp))
            }
        }
        Spacer(Modifier.width(16.dp))
        // 元信息
        Column(Modifier.weight(1f)) {
            Text(
                playlist?.name ?: "歌单",
                color = MineradioColors.FcInk,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "${playlist?.creator?.nickname ?: "未知"} · $trackCount 首",
                color = MineradioColors.FcMuted,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            playlist?.description?.takeIf { it.isNotBlank() }?.let {
                Spacer(Modifier.height(2.dp))
                Text(it, color = MineradioColors.FcMuted.copy(alpha = 0.7f), fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Spacer(Modifier.width(12.dp))
        Button(
            onClick = onPlayAll,
            colors = ButtonDefaults.buttonColors(
                containerColor = MineradioColors.FcAccent,
                contentColor = MineradioColors.ChillInk,
            ),
        ) { Text("播放全部", fontSize = 12.sp) }
    }
}

/** 歌单列表 —— 入口网格（我的歌单）。 */
@Composable
fun PlaylistLibrary(
    playlists: List<Playlist>,
    onPlaylistClick: (Playlist) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GlassPanel(modifier = modifier, cornerRadius = 28) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回", tint = MineradioColors.FcInk2)
                }
                Spacer(Modifier.width(8.dp))
                Text("我的歌单", color = MineradioColors.FcInk, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                Text("${playlists.size} 个", color = MineradioColors.FcMuted, fontSize = 11.sp)
            }
            Spacer(Modifier.height(8.dp))
            LazyColumn(Modifier.fillMaxSize()) {
                items(playlists) { pl ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onPlaylistClick(pl) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MineradioColors.GlassDark),
                        ) {
                            val cover = pl.coverImgUrl ?: pl.picUrl
                            if (!cover.isNullOrEmpty()) {
                                AsyncImage(model = cover, contentDescription = pl.name, modifier = Modifier.fillMaxSize())
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(pl.name, color = MineradioColors.FcInk, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${pl.trackCount} 首 · ${pl.creator?.nickname ?: ""}", color = MineradioColors.FcMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}
