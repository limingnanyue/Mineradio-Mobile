package com.mineradio.player.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.WbSunny
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
import com.mineradio.player.data.api.dto.Song
import com.mineradio.player.data.api.dto.WeatherLocation
import com.mineradio.player.data.api.dto.WeatherRadio
import com.mineradio.player.ui.component.GlassPanel
import com.mineradio.player.ui.theme.MineradioColors

/**
 * 天气电台 —— 复刻桌面版 homeWeatherRadioState / #weather-radio（index.html:20120-20180）。
 *
 * 结构：
 *  - 顶部：定位胶囊（城市 + mood） + 关闭按钮
 *  - 中部：天气歌曲列表（点击播放，整张列表作为队列）
 *
 * mood 来自 server.js 推断（晴/阴/雨…），作为「心情」标签。
 */
@Composable
fun WeatherRadioPanel(
    radio: WeatherRadio?,
    location: WeatherLocation?,
    onSongClick: (Int) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GlassPanel(modifier = modifier, cornerRadius = 28) {
        Column(Modifier.fillMaxSize()) {
            // 顶部 banner
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0x331E2A36), Color.Transparent),
                        )
                    )
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.WbSunny, null, tint = MineradioColors.Champagne, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("天气电台", color = MineradioColors.FcInk, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    val city = location?.name?.takeIf { it.isNotBlank() } ?: radio?.location?.name ?: "未知地点"
                    val mood = radio?.mood ?: "默认"
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.LocationOn, null, tint = MineradioColors.FcMuted, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(2.dp))
                        Text(city, color = MineradioColors.FcMuted, fontSize = 11.sp)
                        Spacer(Modifier.width(8.dp))
                        MoodPill(mood)
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, "关闭", tint = MineradioColors.FcInk2)
                }
            }
            // 歌曲列表
            val songs = radio?.songs.orEmpty()
            if (songs.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("正在为你挑选歌曲…", color = MineradioColors.FcMuted, fontSize = 13.sp)
                }
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    items(songs.withIndex().toList()) { (i, song) ->
                        WeatherSongRow(song = song, index = i, onClick = { onSongClick(i) })
                    }
                }
            }
        }
    }
}

@Composable
private fun MoodPill(mood: String) {
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(MineradioColors.Champagne.copy(alpha = 0.18f))
            .border(1.dp, MineradioColors.Champagne.copy(alpha = 0.4f), RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 2.dp),
    ) {
        Text(mood, color = MineradioColors.Champagne, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun WeatherSongRow(song: Song, index: Int, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "${index + 1}",
            color = MineradioColors.FcMuted,
            fontSize = 12.sp,
            modifier = Modifier.width(28.dp),
        )
        Box(
            Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MineradioColors.GlassDark),
            contentAlignment = Alignment.Center,
        ) {
            if (song.displayCover.isNotEmpty()) {
                AsyncImage(model = song.displayCover, contentDescription = song.name, modifier = Modifier.fillMaxSize())
            } else {
                Icon(Icons.Filled.PlayArrow, null, tint = MineradioColors.FcMuted, modifier = Modifier.size(18.dp))
            }
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(song.name, color = MineradioColors.FcInk, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(2.dp))
            Text(song.displayArtist, color = MineradioColors.FcMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
