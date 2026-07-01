package com.mineradio.player.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.mineradio.player.data.api.dto.Song
import com.mineradio.player.ui.theme.MineradioColors

/**
 * 歌曲行 —— 复刻桌面版搜索结果/歌单曲目列表的单行样式。
 */
@Composable
fun SongRow(
    song: Song,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 封面
        Box(
            Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MineradioColors.GlassDark),
            contentAlignment = Alignment.Center,
        ) {
            if (song.displayCover.isNotEmpty()) {
                AsyncImage(
                    model = song.displayCover,
                    contentDescription = song.name,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Icon(Icons.Filled.MusicNote, null, tint = MineradioColors.FcMuted, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                song.name,
                color = MineradioColors.FcInk,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                "${song.displayArtist}  ·  ${song.displayAlbum}",
                color = MineradioColors.FcMuted,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (trailing != null) {
            Spacer(Modifier.width(8.dp))
            trailing()
        }
    }
}

/** 歌曲列表（搜索结果 / 歌单曲目）。 */
@Composable
fun SongList(
    songs: List<Song>,
    onSongClick: (Song) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier) {
        items(songs) { song ->
            SongRow(song = song, onClick = { onSongClick(song) })
        }
    }
}

/**
 * 带喜欢按钮的歌曲列表 —— 每行 trailing 显示 LikeButton。
 * 对应桌面版搜索结果/队列面板里每首歌右侧的红心。
 *
 * @param onCheckLikes 列表首次显示时批量检查喜欢态（避免逐个请求）
 */
@Composable
fun LikeableSongList(
    songs: List<Song>,
    likedMap: Map<String, Boolean>,
    onSongClick: (Song) -> Unit,
    onToggleLike: (Song) -> Unit,
    onCheckLikes: (List<Song>) -> Unit,
    modifier: Modifier = Modifier,
) {
    // 首次有歌曲时触发批量喜欢检查
    androidx.compose.runtime.LaunchedEffect(songs) {
        if (songs.isNotEmpty()) onCheckLikes(songs)
    }
    LazyColumn(modifier = modifier) {
        items(songs) { song ->
            val liked = likedMap[song.id.toString()] ?: false
            SongRow(
                song = song,
                onClick = { onSongClick(song) },
                trailing = {
                    LikeButton(liked = liked, onClick = { onToggleLike(song) })
                },
            )
        }
    }
}
