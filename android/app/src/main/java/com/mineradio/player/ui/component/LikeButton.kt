package com.mineradio.player.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mineradio.player.ui.theme.MineradioColors

/**
 * 喜欢按钮 —— 复刻桌面版 .like-btn / #song-like（index.html:20970-21010）。
 *
 * 桌面版行为：
 *  - likedSongMap 以 String(song.id) 为 key
 *  - 点击触发 setLike(songId, !liked) → 乐观更新本地 map
 *  - 已喜欢：实心红心 + 缩放弹动；未喜欢：描边轮廓
 *
 * 用作 SongRow 的 trailing slot。
 */
@Composable
fun LikeButton(
    liked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scale by animateFloatAsState(
        targetValue = if (liked) 1.12f else 1f,
        animationSpec = tween(durationMillis = 220),
        label = "like-scale",
    )
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .size(36.dp)
            .clip(RoundedCornerShape(50))
            .clickable(
                interactionSource = interaction,
                indication = null,
            ) { onClick() }
            .padding(6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (liked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
            contentDescription = if (liked) "取消喜欢" else "喜欢",
            tint = if (liked) MineradioColors.Danger else MineradioColors.FcMuted,
            modifier = Modifier
                .size(20.dp)
                .scale(scale),
        )
    }
}

/**
 * 收藏到歌单按钮 —— 复刻桌面版搜索结果行的 collect 入口（Bookmarks 图标）。
 * 点击后由 ViewModel 打开 #collect 弹窗，目标为该行对应的曲目。
 */
@Composable
fun CollectButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .size(34.dp)
            .clip(RoundedCornerShape(50))
            .clickable(
                interactionSource = interaction,
                indication = null,
            ) { onClick() }
            .padding(6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = androidx.compose.material.icons.Icons.Filled.PlaylistAdd,
            contentDescription = "收藏到歌单",
            tint = MineradioColors.FcMuted,
            modifier = Modifier.size(18.dp),
        )
    }
}
