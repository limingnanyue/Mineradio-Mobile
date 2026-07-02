package com.mineradio.player.data.api.dto

import com.google.gson.annotations.SerializedName

/**
 * Mineradio 后端 DTO —— 与 server.js 的 JSON 响应结构对齐。
 * 仅包含 UI 实际消费的字段；多余字段用 transient 忽略，避免破坏。
 */

// ---- 通用 ----
data class AppVersion(
    val name: String? = null,
    val productName: String? = null,
    val version: String? = null,
    val update: UpdateInfo? = null,
)

data class UpdateInfo(
    val provider: String? = null,
    val configured: Boolean = false,
    val owner: String? = null,
    val repo: String? = null,
    val preview: Boolean = false,
)

// ---- 登录态 ----
data class LoginStatus(
    val provider: String? = null,
    val loggedIn: Boolean = false,
    val userId: Long? = null,
    val nickname: String? = null,
    val avatarUrl: String? = null,
    val message: String? = null,
    val error: String? = null,
    val playbackKeyReady: Boolean? = null,
    val saved: Boolean? = null,
    // VIP 信息（对应桌面版 data.vipType / vipLevel / isVip / isSvip / vipLabel）
    val vipType: Int? = null,
    val vipLevel: Int? = null,
    val isVip: Boolean? = null,
    val isSvip: Boolean? = null,
    val vipLabel: String? = null,
) {
    /** 派生：是否为 VIP 用户（任一 VIP 标志为真即视为 VIP）。 */
    val isVipUser: Boolean get() = isVip == true || isSvip == true || (vipType != null && vipType > 0)
    /** 派生：VIP 展示文案（优先 vipLabel，其次 SVIP/VIP，无则空串）。 */
    val displayVipLabel: String
        get() = when {
            vipLabel?.isNotEmpty() == true -> vipLabel!!
            isSvip == true -> "SVIP"
            isVip == true -> "VIP"
            vipLevel != null && vipLevel > 0 -> "VIP $vipLevel"
            else -> ""
        }
}

data class CookieLoginRequest(val cookie: String)
data class QrKey(val key: String? = null, val url: String? = null)
data class QrCreate(val img: String? = null, val qrurl: String? = null, val url: String? = null)
data class QrCheck(val code: Int = 0, val message: String? = null, val cookie: String? = null, val nickname: String? = null)

// ---- 歌曲 ----
data class Song(
    val id: Long,
    val name: String = "",
    val artists: List<Artist>? = null,
    val artist: String? = null,
    val album: String? = null,
    val albumName: String? = null,
    val cover: String? = null,
    val coverUrl: String? = null,
    val duration: Long = 0,
    val source: String = "netease",
    val mid: String? = null,
    val mediaMid: String? = null,
    val fee: Int = 0,
    val freeTrialInfo: FreeTrialInfo? = null,
) {
    val displayArtist: String
        get() = artist ?: artists?.joinToString(" / ") { it.name } ?: ""
    val displayAlbum: String
        get() = album ?: albumName ?: ""
    val displayCover: String
        get() = cover ?: coverUrl ?: ""
}

data class Artist(
    val id: Long = 0,
    val name: String = "",
    val mid: String? = null,
    val img1v1Url: String? = null,
    val picUrl: String? = null,
)

data class FreeTrialInfo(val start: Long = 0, val end: Long = 0)

/**
 * 解析后的可播放结果 —— 把 resolvePlayableUrl 的纯字符串升级为结构体，
 * 携带试听 / VIP / 音质信息，供 TrialBanner / SourceFallbackNotice / QualityPill 消费。
 */
data class ResolvedPlayable(
    val url: String = "",
    val isTrial: Boolean = false,
    val vipLevel: Int? = null,
    val br: Long = 0,
    val level: String? = null,
    val qualityLabel: String = "未知",
    val freeTrialInfo: FreeTrialInfo? = null,
)

data class SongUrl(
    val id: Long? = null,
    val url: String? = null,
    val br: Long = 0,
    val size: Long = 0,
    val type: String? = null,
    val source: String? = null,
    val message: String? = null,
    // 试听/VIP/音质信息（对应桌面版 data.trial / data.vipLevel / data.level / data.freeTrialInfo）
    val trial: Boolean? = null,
    val vipLevel: Int? = null,
    val level: String? = null,
    val freeTrialInfo: FreeTrialInfo? = null,
) {
    /** 派生：是否为试听片段（trial=true 或 freeTrialInfo 非空）。 */
    val isTrial: Boolean get() = trial == true || freeTrialInfo != null
    /** 派生：实际音质档位文案。 */
    val qualityLabel: String
        get() = when {
            br >= 999000L -> "Hi-Res"
            br >= 320000L -> "无损"
            br >= 192000L -> "HQ"
            br > 0L -> "标准"
            else -> "未知"
        }
}

// ---- 搜索 ----
data class SearchResult(
    val songs: List<Song> = emptyList(),
    val songCount: Int = 0,
    val artists: List<Artist> = emptyList(),
    val artistCount: Int = 0,
    val albums: List<Any> = emptyList(),
)

// ---- 歌单 ----
data class Playlist(
    val id: Long,
    val name: String = "",
    val coverImgUrl: String? = null,
    val picUrl: String? = null,
    val description: String? = null,
    val trackCount: Int = 0,
    val playCount: Long = 0,
    val creator: PlaylistCreator? = null,
    val source: String = "netease",
)

data class PlaylistCreator(
    val userId: Long = 0,
    val nickname: String = "",
    val avatarUrl: String? = null,
)

data class PlaylistDetail(
    val id: Long = 0,
    val name: String = "",
    val coverImgUrl: String? = null,
    val tracks: List<Song> = emptyList(),
    val trackIds: List<Long> = emptyList(),
)

// ---- 歌词 ----
data class Lyric(
    val lrc: LyricContent? = null,
    val tlyric: LyricContent? = null,
    val romalrc: LyricContent? = null,
    val yrc: LyricContent? = null,
)

data class LyricContent(val lyric: String? = null)

// ---- 评论 ----
data class Comment(
    val commentId: Long = 0,
    val content: String = "",
    val time: Long = 0,
    val likedCount: Int = 0,
    val user: CommentUser? = null,
)

data class CommentUser(
    val userId: Long = 0,
    val nickname: String = "",
    val avatarUrl: String? = null,
)

// ---- 首页 / 发现 ----
data class DiscoverHome(
    val weatherRadio: List<Song> = emptyList(),
    val dailyRecommend: List<Song> = emptyList(),
    val privateRadio: List<Song> = emptyList(),
    val recentVoice: List<Song> = emptyList(),
    val myPlaylists: List<Playlist> = emptyList(),
    val portrait: Any? = null,
)

// ---- 天气电台 ----
data class WeatherRadio(
    val location: WeatherLocation? = null,
    val mood: String? = null,
    val songs: List<Song> = emptyList(),
)

data class WeatherLocation(
    val name: String = "",
    val country: String? = null,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val timezone: String? = null,
)

// ---- 播客 / DJ ----
data class Podcast(
    val id: Long,
    val name: String = "",
    val cover: String? = null,
    val desc: String? = null,
    val programCount: Int = 0,
    val dj: DjUser? = null,
)

data class DjUser(
    val userId: Long = 0,
    val nickname: String = "",
    val avatarUrl: String? = null,
)

data class PodcastProgram(
    val id: Long,
    val name: String = "",
    val cover: String? = null,
    val duration: Long = 0,
    val createTime: Long = 0,
    val description: String? = null,
    val mainSong: Song? = null,
    val url: String? = null,
)

// ---- 节奏分析（beatmap）----
data class BeatmapStatus(val hasCache: Boolean = false, val key: String? = null)
data class Beatmap(val bpm: Float = 0f, val beats: List<Float> = emptyList(), val intro: Float = 0f, val source: String? = null)

// ---- 歌手详情（对应桌面版 /api/artist/detail 与 /api/qq/artist/detail）----
data class ArtistDetail(
    val artist: ArtistInfo = ArtistInfo(),
    val songs: List<Song> = emptyList(),
    val error: String? = null,
)

data class ArtistInfo(
    val id: Long = 0,
    val name: String = "",
    val mid: String? = null,
    val avatar: String? = null,
    val img1v1Url: String? = null,
    val picUrl: String? = null,
    val albumSize: Int = 0,
    val musicSize: Int = 0,
    val briefDesc: String? = null,
)

// ---- 喜欢列表 ----
data class LikeList(val ids: List<Long> = emptyList(), val likedIds: Set<Long> = emptySet())
data class LikeCheck(val code: Int = 0, val likes: Map<String, Boolean> = emptyMap())

// ---- 歌单操作 ----
data class PlaylistCreateResult(val id: Long = 0, val code: Int = 0, val message: String? = null)
data class PlaylistAddSongResult(val code: Int = 0, val message: String? = null)

// ---- 简单响应 ----
data class SimpleResult(val ok: Boolean = false, val code: Int = 0, val message: String? = null, val error: String? = null)
