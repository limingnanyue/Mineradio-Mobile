package com.mineradio.player.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.mineradio.player.data.api.dto.DiscoverHome
import com.mineradio.player.data.api.dto.Playlist
import com.mineradio.player.data.api.dto.Song
import com.mineradio.player.data.api.dto.WeatherRadio
import com.mineradio.player.data.stats.ListenSummary
import com.mineradio.player.data.stats.ListenRecord
import com.mineradio.player.ui.theme.MineradioColors

/**
 * 首页网格 —— 复刻桌面版 #empty-home / .home-grid（index.html:1934-1991, 289-313）。
 *
 * 6 张卡片（2 列网格）：
 *  1. 我的歌单（Library）→ 打开歌单列表
 *  2. 每日推荐（Daily）→ 播放
 *  3. 私人电台（Song）→ 播放
 *  4. 继续听（Continue）→ 播放最近
 *  5. 听歌画像（Profile）→ 打开画像（显示 topArtist / 总分钟数）
 *  6. 常听歌手（Song）→ 播放
 *
 * 下方 home-rail：最多 5 个 tile（天气歌曲/最近/画像/歌单/播客）。
 */
@Composable
fun HomeGrid(
    discover: DiscoverHome?,
    weatherRadio: WeatherRadio?,
    listenSummary: ListenSummary?,
    onLibraryClick: () -> Unit,
    onDailyClick: () -> Unit,
    onPrivateRadioClick: () -> Unit,
    onContinueClick: () -> Unit,
    onProfileClick: () -> Unit,
    onWeatherSongClick: (Int) -> Unit,
    onPlaylistTileClick: (Playlist) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize().padding(16.dp)) {
        // 6 卡片网格（2 列）
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            HomeCard("LIBRARY", "我的歌单", "${discover?.myPlaylists?.size ?: 0} 个歌单", null, onLibraryClick, Modifier.weight(1f))
            HomeCard("DAILY", "每日推荐", "${discover?.dailyRecommend?.size ?: 0} 首", discover?.dailyRecommend?.firstOrNull()?.displayCover, onDailyClick, Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            HomeCard("SONG", "私人电台", "为你推荐", discover?.privateRadio?.firstOrNull()?.displayCover, onPrivateRadioClick, Modifier.weight(1f))
            HomeCard("MIX", "继续听", "最近播放", discover?.recentVoice?.firstOrNull()?.displayCover, onContinueClick, Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            // 听歌画像卡片：显示 topArtist 名字 + 总播放分钟数（无数据时显示引导文案）
            val profileTitle = listenSummary?.topArtist?.name ?: "听歌画像"
            val profileSub = if (listenSummary != null && listenSummary.totalPlays > 0) {
                "听过 ${listenSummary.totalPlays} 次 · ${listenSummary.totalMinutes} 分钟"
            } else {
                "你的音乐足迹"
            }
            val profileCover = listenSummary?.topSong?.cover?.ifEmpty { null }
                ?: listenSummary?.recent?.cover?.ifEmpty { null }
            HomeCard("LOCAL", profileTitle, profileSub, profileCover, onProfileClick, Modifier.weight(1f))
            // 常听歌手卡片：路由到听歌画像弹层（弹层内含 topArtist 列表），不再误指向每日推荐
            val topArtistCover = listenSummary?.topSong?.cover?.ifEmpty { null }
            HomeCard("MIX", "常听歌手", "发现更多", topArtistCover, onProfileClick, Modifier.weight(1f))
        }

        // 天气 meta 胶囊
        weatherRadio?.location?.name?.let { city ->
            val w = weatherRadio
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                WeatherPill(city)
                w.mood?.let { WeatherPill(it) }
                WeatherPill("${w.songs.size} 首推荐")
            }
        }

        // home-rail tiles —— 按 kind 分发到对应回调（对应桌面版 home-rail tile 点击行为）
        val tiles = buildHomeTiles(discover, weatherRadio, listenSummary)
        if (tiles.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Text("为你推荐", color = MineradioColors.FcMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(tiles) { tile ->
                    HomeTileCard(tile, onClick = {
                        when (tile.kind) {
                            "weatherSong" -> onWeatherSongClick(tile.index)
                            "recent" -> onContinueClick()
                            "profile" -> onProfileClick()
                            "playlist" -> discover?.myPlaylists?.getOrNull(tile.index)?.let(onPlaylistTileClick)
                            else -> onDailyClick()
                        }
                    })
                }
            }
        }
    }
}

@Composable
private fun HomeCard(
    label: String,
    title: String,
    sub: String,
    coverUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .height(152.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.linearGradient(
                    listOf(Color(0xA812151A), Color(0xC208090D)),
                )
            )
            .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(22.dp))
            .clickable(onClick = onClick)
            .padding(17.dp),
    ) {
        Column {
            Text(label, color = MineradioColors.FcAccent, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.4.sp)
            Spacer(Modifier.height(6.dp))
            Text(title, color = MineradioColors.FcInk, fontSize = 19.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp))
            Text(sub, color = MineradioColors.FcInk2.copy(alpha = 0.55f), fontSize = 11.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        // 右下封面
        if (!coverUrl.isNullOrEmpty()) {
            AsyncImage(
                model = coverUrl,
                contentDescription = title,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(108.dp)
                    .clip(RoundedCornerShape(24.dp)),
            )
        }
    }
}

@Composable
private fun WeatherPill(text: String) {
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(Color(0x1AFFFFFF))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(text, color = MineradioColors.FcInk2, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

data class HomeTile(
    val kind: String,        // weatherSong / recent / song / playlist / podcast / profile
    val title: String,
    val sub: String,
    val cover: String?,
    val index: Int,
)

private fun buildHomeTiles(
    discover: DiscoverHome?,
    weather: WeatherRadio?,
    summary: ListenSummary?,
): List<HomeTile> {
    val tiles = mutableListOf<HomeTile>()
    weather?.songs?.take(3)?.forEachIndexed { i, s ->
        tiles.add(HomeTile("weatherSong", s.name, s.displayArtist, s.displayCover, i))
    }
    discover?.recentVoice?.take(2)?.forEachIndexed { i, s ->
        tiles.add(HomeTile("recent", s.name, s.displayArtist, s.displayCover, i))
    }
    // 听歌画像 tile：常听歌手或最热曲目（对应桌面版 home-rail 的 profile tile）
    summary?.topSong?.let { ts ->
        tiles.add(HomeTile("profile", ts.name, "听过 ${ts.plays} 次", ts.cover.ifEmpty { null }, 0))
    }
    discover?.myPlaylists?.take(2)?.forEachIndexed { i, p ->
        tiles.add(HomeTile("playlist", p.name, "${p.trackCount} 首", p.coverImgUrl ?: p.picUrl, i))
    }
    return tiles.take(5)
}

@Composable
private fun HomeTileCard(tile: HomeTile, onClick: () -> Unit) {
    Box(
        Modifier
            .width(160.dp)
            .height(166.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xA6101216))
            .border(1.dp, Color(0x14FFFFFF), RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(10.dp),
    ) {
        Column {
            if (!tile.cover.isNullOrEmpty()) {
                AsyncImage(
                    model = tile.cover,
                    contentDescription = tile.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(92.dp)
                        .clip(RoundedCornerShape(12.dp)),
                )
            } else {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(92.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MineradioColors.GlassDark),
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(tile.title, color = MineradioColors.FcInk, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(2.dp))
            Text(tile.sub, color = MineradioColors.FcMuted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
