package com.mineradio.player.data.repo

import com.mineradio.player.data.api.BackendConfig
import com.mineradio.player.data.api.MineradioApi
import com.mineradio.player.data.api.dto.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * 仓库层 —— 把 MineradioApi 的 48 路由按业务领域分组封装，供 ViewModel 调用。
 * 自动处理网易云 / QQ 两个源的切换；URL 代理统一由 BackendConfig 拼接。
 */
class MineradioRepository(
    private val backendConfig: BackendConfig,
    private val apiProvider: suspend () -> MineradioApi,
) {

    private suspend fun api(): MineradioApi = apiProvider()

    val backendUrlFlow: Flow<String> = backendConfig.backendUrlFlow

    suspend fun setBackend(url: String) = backendConfig.set(url)

    // ---- 应用 / 版本 ----
    suspend fun getAppVersion(): AppVersion = withContext(Dispatchers.IO) { api().getAppVersion() }
    suspend fun checkUpdate(): AppVersion = withContext(Dispatchers.IO) { api().getUpdateLatest() }

    // ---- 登录 ----
    suspend fun neteaseStatus(): LoginStatus = withContext(Dispatchers.IO) { api().getLoginStatus() }
    suspend fun qqStatus(): LoginStatus = withContext(Dispatchers.IO) { api().getQqLoginStatus() }
    suspend fun saveNeteaseCookie(cookie: String): LoginStatus =
        withContext(Dispatchers.IO) { api().postLoginCookie(CookieLoginRequest(cookie)) }
    suspend fun saveQqCookie(cookie: String): LoginStatus =
        withContext(Dispatchers.IO) { api().postQqLoginCookie(CookieLoginRequest(cookie)) }
    suspend fun neteaseLogout(): SimpleResult = withContext(Dispatchers.IO) { api().logout() }
    suspend fun qqLogout(): SimpleResult = withContext(Dispatchers.IO) { api().qqLogout() }

    suspend fun qrKey(): QrKey = withContext(Dispatchers.IO) { api().getQrKey() }
    suspend fun qrCreate(key: String): QrCreate = withContext(Dispatchers.IO) { api().getQrCreate(key) }
    suspend fun qrCheck(key: String): QrCheck = withContext(Dispatchers.IO) { api().getQrCheck(key) }

    // ---- 首页 / 发现 ----
    suspend fun discoverHome(): DiscoverHome = withContext(Dispatchers.IO) { api().getDiscoverHome() }
    suspend fun weatherRadio(lat: Double? = null, lon: Double? = null, city: String? = null): WeatherRadio =
        withContext(Dispatchers.IO) { api().getWeatherRadio(lat, lon, city) }
    suspend fun weatherIpLocation(): WeatherLocation =
        withContext(Dispatchers.IO) { api().getWeatherIpLocation() }

    // ---- 搜索 ----
    suspend fun search(keywords: String, source: String = "netease"): SearchResult =
        withContext(Dispatchers.IO) {
            if (source == "qq") api().qqSearch(keywords) else api().search(keywords)
        }

    // ---- 歌曲 URL（按源分流）----
    suspend fun songUrl(song: Song): SongUrl = withContext(Dispatchers.IO) {
        if (song.source == "qq" && !song.mid.isNullOrEmpty()) {
            api().getQqSongUrl(song.mid, song.mediaMid)
        } else {
            api().getSongUrl(song.id)
        }
    }

    /** 解析播放用的最终音频 URL，自动套后端流式代理。 */
    suspend fun resolvePlayableUrl(song: Song): String = withContext(Dispatchers.IO) {
        val res = songUrl(song)
        val raw = res.url?.takeIf { it.isNotBlank() } ?: return@withContext ""
        backendConfig.audioProxyUrl(raw, backendConfig.baseUrl())
    }

    // ---- 歌词 ----
    suspend fun lyric(song: Song): Lyric = withContext(Dispatchers.IO) {
        if (song.source == "qq" && !song.mid.isNullOrEmpty()) api().getQqLyric(song.mid)
        else api().getLyric(song.id)
    }

    // ---- 歌单 ----
    suspend fun userPlaylists(source: String = "netease"): List<Playlist> = withContext(Dispatchers.IO) {
        if (source == "qq") api().getQqUserPlaylists() else api().getUserPlaylists()
    }
    suspend fun playlistTracks(id: Long, source: String = "netease"): PlaylistDetail =
        withContext(Dispatchers.IO) {
            if (source == "qq") api().getQqPlaylistTracks(id) else api().getPlaylistTracks(id)
        }
    suspend fun createPlaylist(name: String): PlaylistCreateResult =
        withContext(Dispatchers.IO) { api().createPlaylist(name) }
    suspend fun addSongToPlaylist(pid: Long, songId: Long): PlaylistAddSongResult =
        withContext(Dispatchers.IO) { api().addSongToPlaylist(pid, songId.toString()) }

    // ---- 喜欢 ----
    suspend fun likeCheck(ids: List<Long>): LikeCheck =
        withContext(Dispatchers.IO) { api().getLikeCheck(ids.joinToString(",")) }
    suspend fun setLike(id: Long, like: Boolean): SimpleResult =
        withContext(Dispatchers.IO) { api().setLike(id, like) }

    // ---- 评论 / 歌手 ----
    suspend fun comments(song: Song, limit: Int = 20): List<Comment> = withContext(Dispatchers.IO) {
        if (song.source == "qq" && !song.mid.isNullOrEmpty()) api().getQqSongComments(song.id, song.mid, limit)
        else api().getSongComments(song.id, limit)
    }

    // ---- 播客 / DJ ----
    suspend fun podcastHot(limit: Int = 20): List<Podcast> = withContext(Dispatchers.IO) { api().getPodcastHot(limit) }
    suspend fun searchPodcast(keywords: String): List<Podcast> = withContext(Dispatchers.IO) { api().searchPodcast(keywords) }
    suspend fun podcastPrograms(id: Long, limit: Int = 50): List<PodcastProgram> =
        withContext(Dispatchers.IO) { api().getPodcastPrograms(id, limit) }
    suspend fun myPodcasts(): List<Podcast> = withContext(Dispatchers.IO) { api().getMyPodcasts() }
    suspend fun myPodcastItems(key: String): List<PodcastProgram> =
        withContext(Dispatchers.IO) { api().getMyPodcastItems(key) }
    suspend fun podcastDjBeatmap(url: String, duration: Long, intro: Long? = null): Beatmap =
        withContext(Dispatchers.IO) { api().getPodcastDjBeatmap(url, duration, intro) }

    // ---- 节奏缓存 ----
    suspend fun beatmapStatus(): BeatmapStatus = withContext(Dispatchers.IO) { api().getBeatmapCacheStatus() }
    suspend fun beatmap(key: String): Beatmap = withContext(Dispatchers.IO) { api().getBeatmapCache(key) }
    suspend fun saveBeatmap(b: Beatmap): SimpleResult = withContext(Dispatchers.IO) { api().postBeatmapCache(b) }

    /** 拼接封面代理 URL（供 Coil 加载）。 */
    suspend fun resolveCoverUrl(rawCover: String): String {
        if (rawCover.isEmpty()) return ""
        return backendConfig.coverProxyUrl(rawCover, backendConfig.baseUrl())
    }
}
