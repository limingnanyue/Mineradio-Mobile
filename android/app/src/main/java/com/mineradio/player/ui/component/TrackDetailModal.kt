package com.mineradio.player.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.mineradio.player.data.api.dto.ArtistDetail
import com.mineradio.player.data.api.dto.Comment
import com.mineradio.player.data.api.dto.Song
import com.mineradio.player.ui.theme.MineradioColors

/**
 * 歌曲/歌手详情 —— 复刻桌面版 #track-detail-modal（index.html:16463-16620 openTrackDetailModal）。
 *
 * 双视图：
 *  - song   : 歌曲封面 + 标题/艺人/专辑/来源 + 网易云/QQ 评论列表
 *  - artist : 歌手头像 + 热门歌曲列表（点击播放）
 *
 * 数据由 MainViewModel.openTrackDetail() 异步加载，loading 期间显示加载态。
 */
@Composable
fun TrackDetailModal(
    type: String,
    song: Song?,
    artist: ArtistDetail?,
    comments: List<Comment>,
    loading: Boolean,
    onSongClick: (Song, List<Song>) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DiyOverlayPanel(
        title = if (type == "artist") "歌手详情" else "歌曲详情",
        onClose = onDismiss,
        modifier = modifier,
    ) {
        if (song == null) {
            Text("没有可显示的曲目", color = MineradioColors.FcMuted, fontSize = 12.sp)
            return@DiyOverlayPanel
        }
        Column(
            Modifier
                .fillMaxWidth()
                .heightIn(max = 460.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // ---- 顶部 hero ----
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(72.dp).clip(if (type == "artist") CircleShape else RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    val cover = if (type == "artist") {
                        artist?.artist?.avatar ?: artist?.artist?.picUrl ?: artist?.artist?.img1v1Url
                    } else {
                        song.displayCover
                    }
                    if (!cover.isNullOrEmpty()) {
                        AsyncImage(model = cover, contentDescription = song.name, modifier = Modifier.fillMaxSize())
                    } else {
                        Icon(Icons.Filled.MusicNote, null, tint = MineradioColors.FcMuted, modifier = Modifier.size(28.dp))
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        if (type == "artist") (artist?.artist?.name ?: song.displayArtist) else song.name,
                        color = MineradioColors.FcInk,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        if (type == "artist") "来自当前播放 · ${song.name}"
                        else "${song.displayArtist}  ·  ${song.displayAlbum.ifEmpty { "未知专辑" }}",
                        color = MineradioColors.FcMuted,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            // ---- 详情字段网格 ----
            if (type == "artist") {
                DetailGrid(listOf(
                    "当前歌曲" to song.name,
                    "关联歌手" to (artist?.artist?.name ?: song.displayArtist),
                    "所属专辑" to song.displayAlbum.ifEmpty { "未知" },
                    "来源" to if (song.source == "qq") "QQ 音乐" else "网易云",
                ))
            } else {
                DetailGrid(listOf(
                    "歌曲名" to song.name,
                    "歌手" to song.displayArtist.ifEmpty { "未知" },
                    "专辑" to song.displayAlbum.ifEmpty { "未知" },
                    "来源" to if (song.source == "qq") "QQ 音乐" else "网易云",
                ))
            }

            // ---- 内容区 ----
            if (loading) {
                Text("加载中…", color = MineradioColors.FcMuted, fontSize = 12.sp, modifier = Modifier.padding(8.dp))
            } else if (type == "artist") {
                // 热门歌曲
                Text("热门歌曲", color = MineradioColors.FcAccent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                val hotSongs = artist?.songs.orEmpty()
                if (hotSongs.isEmpty()) {
                    Text(
                        artist?.error ?: "暂无热门歌曲",
                        color = MineradioColors.FcMuted,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(8.dp),
                    )
                } else {
                    hotSongs.forEach { s ->
                        SongRow(
                            song = s,
                            onClick = { onSongClick(s, hotSongs) },
                            modifier = Modifier.clip(RoundedCornerShape(8.dp)),
                        )
                    }
                }
            } else {
                // 评论列表
                Text("评论（${comments.size}）", color = MineradioColors.FcAccent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                if (comments.isEmpty()) {
                    Text("暂无评论", color = MineradioColors.FcMuted, fontSize = 11.sp, modifier = Modifier.padding(8.dp))
                } else {
                    comments.forEach { c ->
                        CommentRow(c)
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailGrid(rows: List<Pair<String, String>>) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(androidx.compose.ui.graphics.Color(0x0DFFFFFF))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        rows.forEach { (k, v) ->
            Row {
                Text(k, color = MineradioColors.FcMuted, fontSize = 11.sp, modifier = Modifier.width(64.dp))
                Text(v, color = MineradioColors.FcInk2, fontSize = 12.sp, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun CommentRow(c: Comment) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                c.user?.nickname ?: "匿名用户",
                color = MineradioColors.FcAccent,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                c.content,
                color = MineradioColors.FcInk2,
                fontSize = 12.sp,
                lineHeight = 18.sp,
            )
            if (c.likedCount > 0) {
                Text("♥ ${c.likedCount}", color = MineradioColors.FcMuted, fontSize = 10.sp)
            }
        }
    }
}
