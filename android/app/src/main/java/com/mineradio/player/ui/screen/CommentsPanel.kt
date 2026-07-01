package com.mineradio.player.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Comment
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.mineradio.player.data.api.dto.Comment
import com.mineradio.player.data.api.dto.Song
import com.mineradio.player.ui.component.GlassPanel
import com.mineradio.player.ui.theme.MineradioColors

/**
 * 评论面板 —— 复刻桌面版 #song-comments（index.html:20910-20960）。
 *
 * 桌面版规则：单次拉取 limit=18，无分页（loadSongComments 仅一次 fetch，
 * 不绑定 scroll 监听做翻页）。这是与歌单/搜索列表的关键差异点。
 *
 * 单条结构：头像 + 昵称 + 内容 + 时间戳 + 点赞数。
 */
@Composable
fun CommentsPanel(
    song: Song?,
    comments: List<Comment>,
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
                Icon(Icons.Filled.Comment, null, tint = MineradioColors.Champagne, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text("歌曲评论", color = MineradioColors.FcInk, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    song?.let {
                        Text(it.name, color = MineradioColors.FcMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                Text("${comments.size} 条", color = MineradioColors.FcMuted, fontSize = 11.sp)
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, "关闭", tint = MineradioColors.FcInk2)
                }
            }
            Box(Modifier.height(1.dp).fillMaxWidth().background(MineradioColors.GlassDark))
            // 评论列表
            if (comments.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.Comment, null, tint = MineradioColors.FcMuted.copy(alpha = 0.4f), modifier = Modifier.size(40.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("暂无评论", color = MineradioColors.FcMuted, fontSize = 13.sp)
                    }
                }
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    items(comments) { c ->
                        CommentRow(c)
                    }
                }
            }
        }
    }
}

@Composable
private fun CommentRow(c: Comment) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        // 头像
        Box(
            Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(50))
                .background(MineradioColors.GlassDark),
            contentAlignment = Alignment.Center,
        ) {
            val avatar = c.user?.avatarUrl
            if (!avatar.isNullOrEmpty()) {
                AsyncImage(model = avatar, contentDescription = c.user?.nickname, modifier = Modifier.fillMaxSize())
            } else {
                Text(c.user?.nickname?.firstOrNull()?.toString() ?: "?", color = MineradioColors.FcMuted, fontSize = 12.sp)
            }
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    c.user?.nickname ?: "匿名",
                    color = MineradioColors.FcInk2,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Spacer(Modifier.width(8.dp))
                Text(formatTime(c.time), color = MineradioColors.FcMuted.copy(alpha = 0.6f), fontSize = 10.sp)
            }
            Spacer(Modifier.height(4.dp))
            Text(
                c.content,
                color = MineradioColors.FcInk,
                fontSize = 13.sp,
            )
            if (c.likedCount > 0) {
                Spacer(Modifier.height(4.dp))
                Text("♥ ${c.likedCount}", color = MineradioColors.Champagne.copy(alpha = 0.8f), fontSize = 10.sp)
            }
        }
    }
}

private fun formatTime(ts: Long): String {
    if (ts <= 0) return ""
    // server.js 返回毫秒时间戳
    val now = System.currentTimeMillis()
    val diff = now - ts
    return when {
        diff < 60_000 -> "刚刚"
        diff < 3_600_000 -> "${diff / 60_000} 分钟前"
        diff < 86_400_000 -> "${diff / 3_600_000} 小时前"
        diff < 30L * 86_400_000 -> "${diff / 86_400_000} 天前"
        else -> {
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            sdf.format(java.util.Date(ts))
        }
    }
}
