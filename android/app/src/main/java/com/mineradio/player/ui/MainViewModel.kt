package com.mineradio.player.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import com.mineradio.player.MineradioApp
import com.mineradio.player.data.api.dto.*
import com.mineradio.player.data.playback.PlayerController
import com.mineradio.player.data.repo.MineradioRepository
import com.mineradio.player.render.ShelfRenderer
import com.mineradio.player.ui.fx.FxArchives
import com.mineradio.player.ui.fx.FxArchiveSlot
import com.mineradio.player.ui.fx.FxArchiveSnapshot
import com.mineradio.player.ui.fx.FxState
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 全局 UI 状态。
 */
data class UiState(
    val backendUrl: String = "",
    val backendReachable: Boolean = false,
    val neteaseLogin: LoginStatus? = null,
    val qqLogin: LoginStatus? = null,
    val activeSource: String = "netease", // netease / qq
    val discover: DiscoverHome? = null,
    val weatherRadio: WeatherRadio? = null,
    val weatherLocation: WeatherLocation? = null,
    val searchResults: List<Song> = emptyList(),
    val searchKeywords: String = "",
    val searchLoading: Boolean = false,
    val playlists: List<Playlist> = emptyList(),
    val currentPlaylistTracks: List<Song> = emptyList(),
    val currentLyric: Lyric? = null,
    val lyricsLines: List<LyricLine> = emptyList(),
    val toast: String? = null,
    val showSettings: Boolean = false,
    val showLogin: Boolean = false,
    val showAccount: Boolean = false,
    // ---- 导航 ----
    val screen: Screen = Screen.PLAYER,
    // ---- 登录轮询 ----
    val loginProvider: String = "netease",
    val qrImgUrl: String? = null,
    val qrStatus: String = "",
    val qqCookieInput: String = "",
    // ---- 歌单详情 ----
    val selectedPlaylist: Playlist? = null,
    // ---- 评论 ----
    val commentsSong: Song? = null,
    val comments: List<Comment> = emptyList(),
    val showComments: Boolean = false,
    // ---- 喜欢 ----
    val likedSongMap: Map<String, Boolean> = emptyMap(),
    // ---- 播客 ----
    val hotPodcasts: List<Podcast> = emptyList(),
    val selectedPodcast: Podcast? = null,
    val podcastPrograms: List<PodcastProgram> = emptyList(),
    val showPodcast: Boolean = false,
    // ---- 3D 歌单架 ----
    val showShelf: Boolean = false,
    val shelfMode: ShelfRenderer.ShelfMode = ShelfRenderer.ShelfMode.SIDE,
    val shelfSelectedIndex: Int = 0,
    // ---- DIY / FX 状态 ----
    val fx: FxState = FxState(),
    val fxArchives: List<FxArchiveSlot> = FxArchives.defaultSlots(),
    val showDesktopLyricsDiy: Boolean = false,
    val showWallpaperDiy: Boolean = false,
    val showFxArchives: Boolean = false,
    val showCoverCrop: Boolean = false,
    val immersiveMode: Boolean = false,
    // ---- 底栏扩展状态（对应桌面版 #bottom-bar 各控件）----
    val showLyricsPanel: Boolean = true,        // .lyrics-toggle-btn，默认显示舞台歌词
    val showMiniQueue: Boolean = false,          // #mini-queue-btn 浮层
    val showCollect: Boolean = false,            // #collect-btn 收藏到歌单弹窗
    val controlsAutoHide: Boolean = false,       // #controls-hide-btn 自动隐藏
    val playbackQuality: String = "auto",        // 桌面版音质档位：auto/sq/hq/lossless/hires/master
    val collectPlaylists: List<Playlist> = emptyList(), // 收藏弹窗里的歌单列表
)

/** 顶层导航目标。 */
enum class Screen {
    PLAYER,         // 播放主页（歌词 + 控制条）
    HOME,           // 首页网格
    PLAYLIST_LIBRARY, // 我的歌单列表
    PLAYLIST_DETAIL,  // 歌单详情
}

/** 解析后的歌词行（时间戳 + 文本）。 */
data class LyricLine(val timeMs: Long, val text: String)

class MainViewModel(
    private val repo: MineradioRepository,
    private val player: PlayerController,
) : ViewModel() {

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private val handler = CoroutineExceptionHandler { _, e ->
        _state.update { it.copy(toast = "出错: ${e.message ?: e::class.simpleName}") }
    }

    /** 二维码登录轮询协程（2 秒一次，对应桌面版 pollQr）。 */
    private var qrPollJob: Job? = null

    val playback: StateFlow<PlayerController.PlaybackState> = player.state

    init {
        // 监听后端地址变化，地址变化后刷新登录态与首页
        viewModelScope.launch(handler) {
            repo.backendUrlFlow.collect { url ->
                _state.update { it.copy(backendUrl = url, backendReachable = url.isNotEmpty()) }
                refreshAll()
            }
        }
    }

    fun setBackendUrl(url: String) {
        viewModelScope.launch(handler) {
            repo.setBackend(url)
        }
    }

    fun toggleSettings() = _state.update { it.copy(showSettings = !it.showSettings) }
    fun setSource(source: String) {
        _state.update { it.copy(activeSource = source) }
        refreshUserPlaylists()
    }

    fun refreshAll() {
        refreshLoginStatus()
        refreshDiscover()
        refreshUserPlaylists()
        refreshWeather()
    }

    fun refreshLoginStatus() {
        viewModelScope.launch(handler) {
            val ne = runCatching { repo.neteaseStatus() }.getOrNull()
            val qq = runCatching { repo.qqStatus() }.getOrNull()
            _state.update { it.copy(neteaseLogin = ne, qqLogin = qq) }
        }
    }

    fun refreshDiscover() {
        viewModelScope.launch(handler) {
            val home = runCatching { repo.discoverHome() }.getOrNull()
            _state.update { it.copy(discover = home) }
        }
    }

    fun refreshWeather() {
        viewModelScope.launch(handler) {
            val loc = runCatching { repo.weatherIpLocation() }.getOrNull()
            _state.update { it.copy(weatherLocation = loc) }
            val radio = runCatching { repo.weatherRadio(loc?.latitude, loc?.longitude, loc?.name) }.getOrNull()
            _state.update { it.copy(weatherRadio = radio) }
        }
    }

    fun refreshUserPlaylists() {
        viewModelScope.launch(handler) {
            val src = _state.value.activeSource
            val ps = runCatching { repo.userPlaylists(src) }.getOrNull().orEmpty()
            _state.update { it.copy(playlists = ps) }
        }
    }

    fun search(keywords: String) {
        _state.update { it.copy(searchKeywords = keywords, searchLoading = true) }
        viewModelScope.launch(handler) {
            val src = _state.value.activeSource
            val res = runCatching { repo.search(keywords, src) }.getOrNull()
            _state.update { it.copy(searchResults = res?.songs.orEmpty(), searchLoading = false) }
        }
    }

    fun loadPlaylistTracks(id: Long) {
        viewModelScope.launch(handler) {
            val src = _state.value.activeSource
            val detail = runCatching { repo.playlistTracks(id, src) }.getOrNull()
            _state.update { it.copy(currentPlaylistTracks = detail?.tracks.orEmpty()) }
        }
    }

    /** 点击歌曲：解析可播放 URL → 加入播放队列 → 拉歌词。 */
    fun playSong(song: Song, queue: List<Song> = listOf(song)) {
        viewModelScope.launch(handler) {
            val idx = queue.indexOf(song).coerceAtLeast(0)
            // 先解析目标歌曲 URL（保证快速起播），其余队列懒解析
            val firstUrl = repo.resolvePlayableUrl(song)
            val urls = ArrayList<String>(queue.size)
            for (i in queue.indices) {
                if (i == idx) {
                    urls.add(firstUrl)
                } else {
                    val u = runCatching { repo.resolvePlayableUrl(queue[i]) }.getOrNull().orEmpty()
                    urls.add(u)
                }
            }
            player.playQueue(queue, urls, idx)
            loadLyric(song)
        }
    }

    fun loadLyric(song: Song) {
        viewModelScope.launch(handler) {
            val lrc = runCatching { repo.lyric(song) }.getOrNull()
            val lines = parseLyric(lrc?.lrc?.lyric)
            _state.update { it.copy(currentLyric = lrc, lyricsLines = lines) }
        }
    }

    // ============ 导航 ============

    fun navigateTo(screen: Screen) = _state.update { it.copy(screen = screen) }
    fun backToPlayer() = _state.update { it.copy(screen = Screen.PLAYER) }

    fun openPlaylistLibrary() {
        refreshUserPlaylists()
        _state.update { it.copy(screen = Screen.PLAYLIST_LIBRARY) }
    }

    fun openPlaylistDetail(playlist: Playlist) {
        _state.update { it.copy(selectedPlaylist = playlist, screen = Screen.PLAYLIST_DETAIL, currentPlaylistTracks = emptyList()) }
        viewModelScope.launch(handler) {
            val src = _state.value.activeSource
            val detail = runCatching { repo.playlistTracks(playlist.id, src) }.getOrNull()
            val tracks = detail?.tracks.orEmpty()
            _state.update { it.copy(currentPlaylistTracks = tracks) }
            // 曲目加载完成后批量检查喜欢态
            if (tracks.isNotEmpty()) checkLikes(tracks)
        }
    }

    fun playAllPlaylist() {
        val tracks = _state.value.currentPlaylistTracks
        if (tracks.isEmpty()) return
        playSong(tracks.first(), tracks)
    }

    // ============ 登录 ============

    fun toggleLogin() = _state.update { it.copy(showLogin = !it.showLogin) }
    fun toggleAccount() = _state.update { it.copy(showAccount = !it.showAccount) }

    fun setLoginProvider(provider: String) {
        qrPollJob?.cancel()
        _state.update { it.copy(loginProvider = provider, qrImgUrl = null, qrStatus = "") }
        if (provider == "netease" && _state.value.showLogin) startQrLogin()
    }

    fun startQrLogin() {
        qrPollJob?.cancel()
        qrPollJob = viewModelScope.launch(handler) {
            // 1. 请求 key
            val key = runCatching { repo.qrKey() }.getOrNull()?.key ?: run {
                _state.update { it.copy(qrStatus = "二维码生成失败") }
                return@launch
            }
            // 2. 用 key 生成二维码图片
            val create = runCatching { repo.qrCreate(key) }.getOrNull()
            _state.update { it.copy(qrImgUrl = create?.img, qrStatus = "请扫描二维码") }
            // 3. 每 2 秒轮询 check（对应桌面版 pollQr 间隔）
            while (true) {
                delay(2000)
                val check = runCatching { repo.qrCheck(key) }.getOrNull() ?: continue
                when (check.code) {
                    800 -> { _state.update { it.copy(qrStatus = "二维码已过期") }; return@launch }
                    801 -> _state.update { it.copy(qrStatus = "等待扫码确认…") }
                    802 -> _state.update { it.copy(qrStatus = "已扫码，请在手机确认") }
                    803 -> {
                        // 登录成功
                        _state.update { it.copy(qrStatus = "登录成功", showLogin = false) }
                        refreshLoginStatus()
                        refreshAll()
                        return@launch
                    }
                }
            }
        }
    }

    fun refreshQr() = startQrLogin()

    fun onQqCookieInput(cookie: String) = _state.update { it.copy(qqCookieInput = cookie) }

    fun submitQqCookie() {
        val cookie = _state.value.qqCookieInput
        if (cookie.isBlank()) {
            _state.update { it.copy(toast = "请粘贴 QQ cookie") }
            return
        }
        viewModelScope.launch(handler) {
            val res = runCatching { repo.saveQqCookie(cookie) }.getOrNull()
            if (res?.loggedIn == true) {
                _state.update { it.copy(showLogin = false, qqCookieInput = "", toast = "QQ 登录成功") }
                refreshLoginStatus()
                refreshAll()
            } else {
                _state.update { it.copy(toast = "QQ 登录失败：${res?.message ?: res?.error ?: "未知"}") }
            }
        }
    }

    fun logout() {
        viewModelScope.launch(handler) {
            val src = _state.value.activeSource
            if (src == "qq") runCatching { repo.qqLogout() } else runCatching { repo.neteaseLogout() }
            _state.update { it.copy(showAccount = false, toast = "已退出登录") }
            refreshLoginStatus()
            refreshAll()
        }
    }

    // ============ 评论 ============

    fun toggleComments(song: Song?) {
        val s = _state.value
        if (s.showComments && s.commentsSong?.id == song?.id) {
            _state.update { it.copy(showComments = false) }
            return
        }
        _state.update { it.copy(commentsSong = song, showComments = true, comments = emptyList()) }
        if (song != null) loadComments(song)
    }

    fun loadComments(song: Song) {
        viewModelScope.launch(handler) {
            // 单次拉取 limit=18，无分页（桌面版 loadSongComments 规则）
            val list = runCatching { repo.comments(song, limit = 18) }.getOrNull().orEmpty()
            _state.update { it.copy(comments = list) }
        }
    }

    fun dismissComments() = _state.update { it.copy(showComments = false) }

    // ============ 喜欢 ============

    /** 批量检查喜欢态，写入 likedSongMap（key = String(song.id)，与桌面版一致）。 */
    fun checkLikes(songs: List<Song>) {
        if (songs.isEmpty()) return
        viewModelScope.launch(handler) {
            val ids = songs.map { it.id }
            val check = runCatching { repo.likeCheck(ids) }.getOrNull() ?: return@launch
            _state.update { it.copy(likedSongMap = it.likedSongMap + check.likes) }
        }
    }

    /** 切换喜欢：乐观更新本地 map → 调 setLike → 失败回滚。 */
    fun toggleLike(song: Song) {
        val key = song.id.toString()
        val current = _state.value.likedSongMap[key] ?: false
        val next = !current
        // 乐观更新
        _state.update { it.copy(likedSongMap = it.likedSongMap + (key to next)) }
        viewModelScope.launch(handler) {
            val res = runCatching { repo.setLike(song.id, next) }.getOrNull()
            if (res?.ok != true && res?.code != 200) {
                // 回滚
                _state.update { it.copy(likedSongMap = it.likedSongMap + (key to current), toast = "操作失败") }
            }
        }
    }

    fun isLiked(song: Song): Boolean = _state.value.likedSongMap[song.id.toString()] ?: false

    // ============ 播客 ============

    fun togglePodcast() {
        if (_state.value.showPodcast) {
            _state.update { it.copy(showPodcast = false) }
        } else {
            _state.update { it.copy(showPodcast = true, selectedPodcast = null, podcastPrograms = emptyList()) }
            loadPodcastHot()
        }
    }

    fun loadPodcastHot() {
        viewModelScope.launch(handler) {
            val list = runCatching { repo.podcastHot(limit = 18) }.getOrNull().orEmpty()
            _state.update { it.copy(hotPodcasts = list) }
        }
    }

    fun selectPodcast(podcast: Podcast) {
        _state.update { it.copy(selectedPodcast = podcast, podcastPrograms = emptyList()) }
        viewModelScope.launch(handler) {
            val progs = runCatching { repo.podcastPrograms(podcast.id) }.getOrNull().orEmpty()
            _state.update { it.copy(podcastPrograms = progs) }
        }
    }

    fun backToPodcastList() = _state.update { it.copy(selectedPodcast = null, podcastPrograms = emptyList()) }

    fun playPodcastProgram(program: PodcastProgram) {
        val song = program.mainSong ?: return
        playSong(song, listOf(song))
    }

    fun dismissPodcast() = _state.update { it.copy(showPodcast = false) }

    // ============ 3D 歌单架 ============

    fun toggleShelf() = _state.update { it.copy(showShelf = !it.showShelf) }
    fun setShelfMode(mode: ShelfRenderer.ShelfMode) = _state.update { it.copy(shelfMode = mode) }
    fun setShelfSelected(index: Int) = _state.update { it.copy(shelfSelectedIndex = index) }

    /** 在 3D 架选中歌单后打开详情。 */
    fun openShelfSelected() {
        val idx = _state.value.shelfSelectedIndex
        val pl = _state.value.playlists.getOrNull(idx) ?: return
        _state.update { it.copy(showShelf = false) }
        openPlaylistDetail(pl)
    }

    // ============ 天气电台快捷入口 ============

    fun playWeatherSong(index: Int) {
        val songs = _state.value.weatherRadio?.songs.orEmpty()
        val song = songs.getOrNull(index) ?: return
        playSong(song, songs)
    }

    fun playPrivateRadio() {
        val songs = _state.value.discover?.privateRadio.orEmpty()
        if (songs.isEmpty()) return
        playSong(songs.first(), songs)
    }

    fun playDailyRecommend() {
        val songs = _state.value.discover?.dailyRecommend.orEmpty()
        if (songs.isEmpty()) return
        playSong(songs.first(), songs)
    }

    fun playRecentVoice() {
        val songs = _state.value.discover?.recentVoice.orEmpty()
        if (songs.isEmpty()) return
        playSong(songs.first(), songs)
    }

    fun playPause() = player.playPause()
    fun seekTo(ms: Long) = player.seekTo(ms)
    fun skipNext() = player.skipNext()
    fun skipPrev() = player.skipPrev()

    /**
     * 循环播放模式 —— 对应桌面版 cyclePlayMode()。
     * 顺序：顺序(REPEAT_OFF) → 列表循环(REPEAT_ALL) → 单曲循环(REPEAT_ONE) → 随机(shuffle=true) → 顺序。
     */
    fun cyclePlayMode() {
        val pb = player.state.value
        when {
            // 顺序播放 → 列表循环
            !pb.shuffle && pb.repeatMode == Player.REPEAT_MODE_OFF -> player.setRepeatMode(Player.REPEAT_MODE_ALL).also {
                _state.update { it.copy(toast = "列表循环") }
            }
            // 列表循环 → 单曲循环
            !pb.shuffle && pb.repeatMode == Player.REPEAT_MODE_ALL -> player.setRepeatMode(Player.REPEAT_MODE_ONE).also {
                _state.update { it.copy(toast = "单曲循环") }
            }
            // 单曲循环 → 随机
            !pb.shuffle && pb.repeatMode == Player.REPEAT_MODE_ONE -> {
                player.setRepeatMode(Player.REPEAT_MODE_OFF)
                player.setShuffle(true)
                _state.update { it.copy(toast = "随机播放") }
            }
            // 随机 → 顺序
            else -> {
                player.setShuffle(false)
                _state.update { it.copy(toast = "顺序播放") }
            }
        }
    }

    /** 切换舞台歌词显示 —— 对应桌面版 toggleLyricsPanel()。 */
    fun toggleLyricsPanel() = _state.update { it.copy(showLyricsPanel = !it.showLyricsPanel) }

    /** 切换迷你队列浮层 —— 对应桌面版 toggleMiniQueue()。 */
    fun toggleMiniQueue() = _state.update { it.copy(showMiniQueue = !it.showMiniQueue) }

    /** 切换控制条自动隐藏 —— 对应桌面版 toggleControlsAutoHide()。 */
    fun toggleControlsAutoHide() = _state.update { it.copy(controlsAutoHide = !it.controlsAutoHide) }

    /** 切换音质档位 —— 对应桌面版 #quality-control。 */
    fun setQuality(quality: String) = _state.update { it.copy(playbackQuality = quality) }

    /** 打开「收藏到歌单」弹窗 —— 对应桌面版 openCollectModalForCurrent()。 */
    fun openCollectModalForCurrent() {
        if (playback.value.current == null) {
            _state.update { it.copy(toast = "没有正在播放的曲目") }
            return
        }
        refreshUserPlaylists()
        _state.update { it.copy(showCollect = true, collectPlaylists = it.playlists) }
    }

    fun toggleCollect() = _state.update { it.copy(showCollect = !it.showCollect) }

    /** 把当前播放曲目加入指定歌单 —— 对应桌面版 collectCurrentToPlaylist(id)。 */
    fun collectCurrentToPlaylist(playlistId: Long) {
        val song = playback.value.current ?: return
        viewModelScope.launch(handler) {
            val res = runCatching { repo.addSongToPlaylist(playlistId, song.id) }.getOrNull()
            if (res?.code == 200) {
                _state.update { it.copy(showCollect = false, toast = "已收藏到歌单") }
            } else {
                _state.update { it.copy(toast = "收藏失败：${res?.message ?: "未知"}") }
            }
        }
    }

    fun dismissToast() = _state.update { it.copy(toast = null) }

    // ============ DIY / FX 控制 ============

    /** 切换 DIY 模式（diyPlayerMode），对应桌面版 toggleDiyMode/applyDiyMode。 */
    fun toggleDiyMode() {
        _state.update {
            val next = !it.fx.diyMode
            it.copy(
                fx = it.fx.copy(diyMode = next),
                toast = if (next) "DIY 模式已开启" else "DIY 模式已关闭",
            )
        }
    }

    /** 切换沉浸模式，对应桌面版 setImmersiveMode。 */
    fun toggleImmersive() {
        _state.update {
            val next = !it.immersiveMode
            it.copy(
                immersiveMode = next,
                // 进入沉浸时关闭所有 DIY 浮层（closeImmersiveInterference 等价）
                showDesktopLyricsDiy = if (next) false else it.showDesktopLyricsDiy,
                showWallpaperDiy = if (next) false else it.showWallpaperDiy,
                showFxArchives = if (next) false else it.showFxArchives,
                showCoverCrop = if (next) false else it.showCoverCrop,
                toast = if (next) "已进入全沉浸式" else "已退出全沉浸式",
            )
        }
    }

    fun toggleDesktopLyricsDiy() = _state.update { it.copy(showDesktopLyricsDiy = !it.showDesktopLyricsDiy) }
    fun toggleWallpaperDiy() = _state.update { it.copy(showWallpaperDiy = !it.showWallpaperDiy) }
    fun toggleFxArchives() = _state.update { it.copy(showFxArchives = !it.showFxArchives) }
    fun toggleCoverCrop() = _state.update { it.copy(showCoverCrop = !it.showCoverCrop) }

    /** 更新 FX 状态的通用入口。 */
    fun updateFx(transform: (FxState) -> FxState) {
        _state.update { it.copy(fx = transform(it.fx)) }
    }

    /** 保存当前 FX 配置到指定槽位（glass-saved-button 行为）。 */
    fun saveFxArchive(index: Int) {
        _state.update { s ->
            val snap = FxArchiveSnapshot(
                preset = s.fx.preset,
                desktopLyrics = s.fx.desktopLyrics,
                desktopLyricsSize = s.fx.desktopLyricsSize,
                desktopLyricsOpacity = s.fx.desktopLyricsOpacity,
                desktopLyricsY = s.fx.desktopLyricsY,
                desktopLyricsClickThrough = s.fx.desktopLyricsClickThrough,
                desktopLyricsCinema = s.fx.desktopLyricsCinema,
                desktopLyricsHighlight = s.fx.desktopLyricsHighlight,
                desktopLyricsFps = s.fx.desktopLyricsFps,
                lyricColorMode = s.fx.lyricColorMode,
                lyricColor = s.fx.lyricColor.value.toLong(),
                lyricHighlightColor = s.fx.lyricHighlightColor.value.toLong(),
                lyricGlowColor = s.fx.lyricGlowColor.value.toLong(),
                visualTintColor = s.fx.visualTintColor.value.toLong(),
                lyricGlowParticles = s.fx.lyricGlowParticles,
                wallpaperMode = s.fx.wallpaperMode,
                wallpaperOpacity = s.fx.wallpaperOpacity,
                lyricFont = s.fx.lyricFont,
                lyricLetterSpacing = s.fx.lyricLetterSpacing,
                lyricLineHeight = s.fx.lyricLineHeight,
                lyricWeight = s.fx.lyricWeight,
                lyricScale = s.fx.lyricScale,
            )
            val archives = s.fxArchives.map {
                if (it.index == index) it.copy(savedAt = System.currentTimeMillis(), snapshot = snap) else it
            }
            s.copy(fxArchives = archives, toast = "已保存到 ${archives[index].name}")
        }
    }

    /** 从槽位加载 FX 配置。 */
    fun loadFxArchive(index: Int) {
        _state.update { s ->
            val slot = s.fxArchives.getOrNull(index) ?: return@update s
            val snap = slot.snapshot ?: return@update s
            val fx = s.fx.copy(
                preset = snap.preset,
                desktopLyrics = snap.desktopLyrics,
                desktopLyricsSize = snap.desktopLyricsSize,
                desktopLyricsOpacity = snap.desktopLyricsOpacity,
                desktopLyricsY = snap.desktopLyricsY,
                desktopLyricsClickThrough = snap.desktopLyricsClickThrough,
                desktopLyricsCinema = snap.desktopLyricsCinema,
                desktopLyricsHighlight = snap.desktopLyricsHighlight,
                desktopLyricsFps = snap.desktopLyricsFps,
                lyricColorMode = snap.lyricColorMode,
                lyricColor = Color(snap.lyricColor),
                lyricHighlightColor = Color(snap.lyricHighlightColor),
                lyricGlowColor = Color(snap.lyricGlowColor),
                visualTintColor = Color(snap.visualTintColor),
                lyricGlowParticles = snap.lyricGlowParticles,
                wallpaperMode = snap.wallpaperMode,
                wallpaperOpacity = snap.wallpaperOpacity,
                lyricFont = snap.lyricFont,
                lyricLetterSpacing = snap.lyricLetterSpacing,
                lyricLineHeight = snap.lyricLineHeight,
                lyricWeight = snap.lyricWeight,
                lyricScale = snap.lyricScale,
            )
            s.copy(fx = fx, toast = "已加载 ${slot.name}")
        }
    }

    /** 把 [mm:ss.xx] 文本解析为带时间戳的歌词行。 */
    private fun parseLyric(raw: String?): List<LyricLine> {
        if (raw.isNullOrBlank()) return emptyList()
        val regex = Regex("""\[(\d+):(\d+)(?:[.:](\d+))?]""")
        val lines = mutableListOf<LyricLine>()
        for (line in raw.split("\n")) {
            val matches = regex.findAll(line).toList()
            if (matches.isEmpty()) continue
            val text = line.substring(matches.last().range.last + 1).trim()
            if (text.isEmpty()) continue
            for (m in matches) {
                val min = m.groupValues[1].toLong()
                val sec = m.groupValues[2].toLong()
                val ms = m.groupValues.getOrNull(3)?.take(3)?.padEnd(3, '0')?.toLongOrNull() ?: 0L
                val t = min * 60_000 + sec * 1000 + ms
                lines.add(LyricLine(t, text))
            }
        }
        return lines.sortedBy { it.timeMs }
    }
}

class MainViewModelFactory(private val app: MineradioApp) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return MainViewModel(app.repository, app.playerController) as T
    }
}
