package com.mineradio.player.data.api

import com.mineradio.player.data.api.dto.*
import retrofit2.http.*

/**
 * Mineradio 后端 Retrofit 接口 —— 完整覆盖 server.js 的 48 条 HTTP 路由。
 *
 * 路径与 server.js 路由器 1:1 对齐，方法（GET/POST）按 server.js 的 req.method 判定。
 * 所有路径为相对路径（/api/...），baseUrl 由 BackendConfig 动态注入。
 */
interface MineradioApi {

    // ===== 应用 / 版本 =====
    @GET("api/app/version")
    suspend fun getAppVersion(): AppVersion

    // ===== 自更新（移动端只读检查，不下载安装包）=====
    @GET("api/update/latest")
    suspend fun getUpdateLatest(@Query("t") t: Long = System.currentTimeMillis()): AppVersion

    // ===== 节奏缓存 =====
    @GET("api/beatmap/cache/status")
    suspend fun getBeatmapCacheStatus(@Query("t") t: Long = System.currentTimeMillis()): BeatmapStatus

    @GET("api/beatmap/cache")
    suspend fun getBeatmapCache(@Query("key") key: String, @Query("t") t: Long = System.currentTimeMillis()): Beatmap

    @POST("api/beatmap/cache")
    suspend fun postBeatmapCache(@Body body: Beatmap): SimpleResult

    // ===== 首页 / 发现 =====
    @GET("api/discover/home")
    suspend fun getDiscoverHome(@Query("t") t: Long = System.currentTimeMillis()): DiscoverHome

    @GET("api/weather/radio")
    suspend fun getWeatherRadio(
        @Query("lat") lat: Double?,
        @Query("lon") lon: Double?,
        @Query("city") city: String?,
        @Query("t") t: Long = System.currentTimeMillis(),
    ): WeatherRadio

    @GET("api/weather/ip-location")
    suspend fun getWeatherIpLocation(@Query("t") t: Long = System.currentTimeMillis()): WeatherLocation

    // ===== 搜索 =====
    @GET("api/search")
    suspend fun search(@Query("keywords") keywords: String, @Query("limit") limit: Int = 30): SearchResult

    @GET("api/qq/search")
    suspend fun qqSearch(@Query("keywords") keywords: String, @Query("limit") limit: Int = 30): SearchResult

    // ===== 歌曲 URL =====
    @GET("api/song/url")
    suspend fun getSongUrl(@Query("id") id: Long, @Query("br") br: Long = 320000, @Query("level") level: String? = null): SongUrl

    @GET("api/qq/song/url")
    suspend fun getQqSongUrl(
        @Query("mid") mid: String,
        @Query("mediaMid") mediaMid: String? = null,
    ): SongUrl

    // ===== 歌词 =====
    @GET("api/lyric")
    suspend fun getLyric(@Query("id") id: Long): Lyric

    @GET("api/qq/lyric")
    suspend fun getQqLyric(@Query("mid") mid: String, @Query("id") id: Long? = null): Lyric

    // ===== 网易云登录 =====
    @GET("api/login/status")
    suspend fun getLoginStatus(): LoginStatus

    @POST("api/login/cookie")
    suspend fun postLoginCookie(@Body body: CookieLoginRequest): LoginStatus

    @GET("api/logout")
    suspend fun logout(): SimpleResult

    @GET("api/login/qr/key")
    suspend fun getQrKey(): QrKey

    @GET("api/login/qr/create")
    suspend fun getQrCreate(@Query("key") key: String): QrCreate

    @GET("api/login/qr/check")
    suspend fun getQrCheck(@Query("key") key: String): QrCheck

    // ===== QQ 登录 =====
    @GET("api/qq/login/status")
    suspend fun getQqLoginStatus(): LoginStatus

    @POST("api/qq/login/cookie")
    suspend fun postQqLoginCookie(@Body body: CookieLoginRequest): LoginStatus

    @GET("api/qq/logout")
    suspend fun qqLogout(): SimpleResult

    // ===== 用户歌单 =====
    @GET("api/user/playlists")
    suspend fun getUserPlaylists(): List<Playlist>

    @GET("api/qq/user/playlists")
    suspend fun getQqUserPlaylists(): List<Playlist>

    @GET("api/playlist/tracks")
    suspend fun getPlaylistTracks(@Query("id") id: Long, @Query("limit") limit: Int = 300): PlaylistDetail

    @GET("api/qq/playlist/tracks")
    suspend fun getQqPlaylistTracks(@Query("id") id: Long): PlaylistDetail

    @FormUrlEncoded
    @POST("api/playlist/create")
    suspend fun createPlaylist(@Field("name") name: String): PlaylistCreateResult

    @GET("api/playlist/create")
    suspend fun createPlaylistGet(@Query("name") name: String): PlaylistCreateResult

    @FormUrlEncoded
    @POST("api/playlist/add-song")
    suspend fun addSongToPlaylist(
        @Field("pid") pid: Long,
        @Field("ids") ids: String,
    ): PlaylistAddSongResult

    @GET("api/playlist/add-song")
    suspend fun addSongToPlaylistGet(
        @Query("pid") pid: Long,
        @Query("ids") ids: String,
    ): PlaylistAddSongResult

    // ===== 喜欢 =====
    @GET("api/song/like/check")
    suspend fun getLikeCheck(@Query("ids") ids: String): LikeCheck

    @GET("api/song/like")
    suspend fun setLike(@Query("id") id: Long, @Query("like") like: Boolean): SimpleResult

    @FormUrlEncoded
    @POST("api/song/like")
    suspend fun setLikePost(@Field("id") id: Long, @Field("like") like: Boolean): SimpleResult

    // ===== 评论 / 歌手 =====
    @GET("api/song/comments")
    suspend fun getSongComments(@Query("id") id: Long, @Query("limit") limit: Int = 20): List<Comment>

    @GET("api/qq/song/comments")
    suspend fun getQqSongComments(@Query("id") id: Long, @Query("mid") mid: String, @Query("limit") limit: Int = 20): List<Comment>

    @GET("api/artist/detail")
    suspend fun getArtistDetail(@Query("id") id: Long, @Query("limit") limit: Int = 36): ArtistDetail

    @GET("api/qq/artist/detail")
    suspend fun getQqArtistDetail(@Query("mid") mid: String, @Query("limit") limit: Int = 36): ArtistDetail

    // ===== 播客 / DJ =====
    @GET("api/podcast/hot")
    suspend fun getPodcastHot(@Query("limit") limit: Int = 20): List<Podcast>

    @GET("api/podcast/search")
    suspend fun searchPodcast(@Query("keywords") keywords: String): List<Podcast>

    @GET("api/podcast/detail")
    suspend fun getPodcastDetail(@Query("id") id: Long): Any

    @GET("api/podcast/programs")
    suspend fun getPodcastPrograms(@Query("id") id: Long, @Query("limit") limit: Int = 50): List<PodcastProgram>

    @GET("api/podcast/my")
    suspend fun getMyPodcasts(): List<Podcast>

    @GET("api/podcast/my/items")
    suspend fun getMyPodcastItems(@Query("key") key: String): List<PodcastProgram>

    @GET("api/podcast/dj-beatmap")
    suspend fun getPodcastDjBeatmap(
        @Query("url") url: String,
        @Query("duration") duration: Long,
        @Query("intro") intro: Long? = null,
    ): Beatmap
}
