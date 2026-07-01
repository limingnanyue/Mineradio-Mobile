package com.mineradio.player.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mineradio.player.ui.theme.MineradioColors

/**
 * 搜索框 —— 复刻桌面版 #search-area 的胶囊形玻璃搜索框 + #search-mode-tabs 模式切换。
 *
 * 模式 tab（对应桌面版 4 个）：
 *  - all     : 全部（跟随当前 activeSource）
 *  - netease : 网易云
 *  - qq      : QQ 音乐
 *  - podcast : 播客
 */
@Composable
fun SearchBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "搜索歌曲、歌手、专辑",
    searchMode: String = "all",
    onModeChange: (String) -> Unit = {},
    history: List<String> = emptyList(),
    onHistoryClick: (String) -> Unit = {},
    onClearHistory: () -> Unit = {},
) {
    Column(modifier) {
        // 模式 tab 行（对应桌面版 #search-mode-tabs）
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            listOf(
                "all" to "All",
                "netease" to "NE",
                "qq" to "QQ",
                "podcast" to "Podcast",
            ).forEach { (mode, label) ->
                val active = searchMode == mode
                Box(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (active) MineradioColors.FcAccent else MineradioColors.GlassDark)
                        .clickable { onModeChange(mode) }
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                ) {
                    Text(
                        label,
                        color = if (active) MineradioColors.ChillInk else MineradioColors.FcInk2,
                        fontSize = 11.sp,
                        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                    )
                }
            }
        }
        // 搜索框
        GlassPanel(
            modifier = Modifier.height(44.dp),
            cornerRadius = 22,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Search, "搜索", tint = MineradioColors.FcAccent)
                Spacer(Modifier.width(10.dp))
                Box(Modifier.weight(1f)) {
                    if (value.isEmpty()) {
                        Text(
                            placeholder,
                            color = MineradioColors.FcMuted,
                            fontSize = 14.sp,
                        )
                    }
                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        singleLine = true,
                        textStyle = TextStyle(
                            color = MineradioColors.FcInk,
                            fontSize = 14.sp,
                        ),
                        cursorBrush = SolidColor(MineradioColors.FcAccent),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (value.isNotEmpty()) {
                    Spacer(Modifier.width(8.dp))
                    Box(
                        Modifier
                            .size(20.dp)
                            .clip(RoundedCornerShape(50))
                            .clickable { onValueChange("") },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.Close, "清除", tint = MineradioColors.FcMuted, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
        // 搜索历史 chip 区（对应桌面版 .search-history-chip，仅在输入为空且有历史时显示）
        if (value.isEmpty() && history.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("历史", color = MineradioColors.FcMuted, fontSize = 10.sp)
                Spacer(Modifier.width(8.dp))
                LazyRow(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(history) { item ->
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(50))
                                .background(MineradioColors.GlassDark)
                                .clickable { onHistoryClick(item) }
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                        ) {
                            Text(
                                item,
                                color = MineradioColors.FcInk2,
                                fontSize = 11.sp,
                                maxLines = 1,
                            )
                        }
                    }
                }
                Spacer(Modifier.width(8.dp))
                Box(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable { onClearHistory() }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Text("清空", color = MineradioColors.FcMuted, fontSize = 10.sp)
                }
            }
        }
    }
}
