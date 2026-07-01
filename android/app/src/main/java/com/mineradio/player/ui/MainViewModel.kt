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
    // 搜索模式（对应桌面版 #search-mode-tabs：all/netease/qq/podcast）
    val searchMode: String = "all",
    val discover: DiscoverHome? = null,
    val weatherRadio: WeatherRadio? = null,
    val weatherLocation: WeatherLocation? = null,
    val searchResults: List<Song> = emptyList(),
    val podcastSearchResults: List<Podcast> = emptyList(),
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
    val showFxPanel: Boolean = false,            // #fx-panel 视觉控制台总面板
    val showCoverCrop: Boolean = false,
    // 新增弹窗（对应桌面版各 modal）
    val showCustomLyric: Boolean = false,        // #custom-lyric-modal 自定义歌词 LRC 编辑器
    val customLyricText: String = "",             // 当前编辑的 LRC 文本
    val showColorLab: Boolean = false,            // #color-lab-pop 色彩实验室
    val colorLabTarget: String = "lyric",         // 色彩实验室编辑目标：lyric/highlight/glow/tint/shelfAccent
    // 封面取色弹层（对应桌面版 .cover-color-pop）
    val showCoverColor: Boolean = false,          // 封面取色弹层开关
    val coverColorTarget: String = "lyric",       // 取色应用目标：lyric/highlight/glow/tint/shelfAccent
    val showVisualGuide: Boolean = false,         // #visual-guide 视觉引导
    val visualGuideSeen: Boolean = false,         // 是否已看过引导
    val showLocalBeat: Boolean = false,           // #local-beat-modal 本地节奏分析
    val showUpdateModal: Boolean = false,         // #update-modal 更新面板
    val updateAvailable: Boolean = false,         // #update-entry 角标
    val updateVersion: String = "",               // 新版本号
    // ---- 歌曲/歌手详情（对应桌面版 #track-detail-modal）----
    val showTrackDetail: Boolean = false,
    val trackDetailType: String = "song",         // song / artist
    val trackDetailSong: Song? = null,
    val trackDetailArtist: ArtistDetail? = null,
    val trackDetailLoading: Boolean = false,
    val trackDetailComments: List<Comment> = emptyList(),
    val showSplash: Boolean = true,               // #splash 启动遮罩
    val immersiveMode: Boolean = false,
    // ---- 底栏扩展状态（对应桌面版 #bottom-bar 各控件）----
    val showLyricsPanel: Boolean = true,        // .lyrics-toggle-btn，默认显示舞台歌词
    val showMiniQueue: Boolean = false,          // #mini-queue-btn 浮层
    val showCollect: Boolean = false,            // #collect-btn 收藏到歌单弹窗
    val collectTargetSong: Song? = null,         // 收藏弹窗的目标曲目（默认为当前播放，搜索结果行可指定）
    val controlsAutoHide: Boolean = false,       // #controls-hide-btn 自动隐藏
    val playbackQuality: String = "auto",        // 桌面版音质档位：auto/sq/hq/lossless/hires/master
    val collectPlaylists: List<Playlist> = emptyList(), // 收藏弹窗里的歌单列表
    // ---- 歌单/队列三 tab 面板（对应桌面版 #playlist-panel 三 tab：queue/playlists/podcasts）----
    val showPlaylistPanel: Boolean = false,      // #playlist-panel 浮层开关
    val playlistPanelTab: String = "queue",      // 当前 tab：queue/playlists/podcasts
    val playlistPanelPinned: Boolean = false,    // #playlist-pin-btn 常开
    // ---- 试听片段提示（对应桌面版 #trial-banner）----
    val showTrialBanner: Boolean = false,        // 试听片段横幅
    val trialText: String = "仅播放试听片段",     // 试听提示文案
    val trialLoggedIn: Boolean = false,          // 是否已登录（控制登录按钮显隐）
    // ---- 节奏/AI 深度状态角标（对应桌面版 #beat-chip / #ai-depth-chip）----
    val beatChipText: String? = null,            // null=隐藏，非空=显示并展示文案
    val aiDepthChipText: String? = null,         // null=隐藏，非空=显示并展示文案
    // ---- 自由镜头提示（对应桌面版 #free-camera-hint）----
    val showFreeCameraHint: Boolean = false,
    // ---- 手势 HUD（对应桌面版 #gesture-hud，移动端为视觉占位，实际手势需摄像头+ML Kit）----
    val showGestureHud: Boolean = false,
    val gestureLabel: String = "待命",           // 当前手势标签
    // ---- 导入文件（对应桌面版 #upload-btn + #file-input）----
    val importedFiles: List<String> = emptyList(), // 已导入文件的 URI 列表
    // ---- 自动换源提示（对应桌面版 #source-fallback-notice）----
    val showSourceFallback: Boolean = false,       // 自动换源横幅
    val sourceFallbackText: String = "",           // 换源说明文案
    // ---- 歌词源切换（对应桌面版 #lyric-source-seg：original/custom）----
    val lyricSource: String = "original",          // original=原词 / custom=自定义
    val originalLyricsLines: List<LyricLine> = emptyList(), // 原词缓存（切回原词时恢复）
    // ---- 搜索历史（对应桌面版 .search-history-chip）----
    val searchHistory: List<String> = emptyList(),
    // ---- 更新面板（对应桌面版 #update-modal 完整结构）----
    val updateChangelog: List<String> = emptyList(),       // 更新内容列表
    val updateHeroMain: String = "",                       // 主文案
    val updateHeroSub: String = "",                        // 副文案
    val updateDownloading: Boolean = false,                // 是否下载中
    val updateDownloadProgress: Float = 0f,                // 0..1
    // ---- 全局加载浮层（对应桌面版 #loading-overlay）----
    val globalLoading: Boolean = false,
    // ---- 听歌画像（对应桌面版 listenStatsState / homeListenSummary）----
    val listenSummary: com.mineradio.player.data.stats.ListenSummary? = null,
    val recentListen: List<com.mineradio.player.data.stats.ListenRecord> = emptyList(),
    val showListenProfile: Boolean = false,        // 听歌画像详情弹层
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
    private val appContext: android.content.Context,
) : ViewModel() {

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private val handler = CoroutineExceptionHandler { _, e ->
        _state.update { it.copy(toast = "出错: ${e.message ?: e::class.simpleName}") }
    }

    /** 二维码登录轮询协程（2 秒一次，对应桌面版 pollQr）。 */
    private var qrPollJob: Job? = null

    val playback: StateFlow<PlayerController.PlaybackState> = player.state

    // 注意：prefs 必须在 init 块之前声明，否则构造期访问会 NPE（by lazy 委托字段按声明顺序初始化）
    private val prefs by lazy {
        appContext.getSharedPreferences("mineradio_prefs", android.content.Context.MODE_PRIVATE)
    }

    /** 听歌画像追踪器 —— 复刻桌面版 listenStatsState 机制。 */
    private val listenTracker by lazy {
        com.mineradio.player.data.stats.ListenStatsTracker(appContext)
    }

    /** 上一次见到的曲目 key，用于检测切歌事件以 begin 新会话。 */
    private var lastSeenSongKey: String? = null

    init {
        // 加载持久化的搜索历史与 FX 存档
        _state.update {
            it.copy(
                searchHistory = loadSearchHistory(),
                fxArchives = loadPersistedFxArchives(),
                listenSummary = listenTracker.summary(),
                recentListen = listenTracker.recentSongs(5),
            )
        }
        // 监听后端地址变化，地址变化后刷新登录态与首页
        viewModelScope.launch(handler) {
            repo.backendUrlFlow.collect { url ->
                _state.update { it.copy(backendUrl = url, backendReachable = url.isNotEmpty()) }
                refreshAll()
            }
        }
        // 听歌画像会话钩子：监听 playback StateFlow，按 current / positionMs / isPlaying 驱动 begin/tick/finalize。
        // 对应桌面版 onAudioTimeUpdate→updateListenStatsTick、onSongChanged→beginListenSession、STATE_ENDED→finalizeListenSession(true)。
        viewModelScope.launch(handler) {
            var lastEnded = false
            player.state.collect { pb ->
                val song = pb.current
                val key = song?.let { com.mineradio.player.data.stats.ListenStatsTracker.queueItemKey(it) }
                // 1. 切歌检测：key 变化时开启新会话（旧会话由 begin 内部 finalize）
                if (key != null && key != lastSeenSongKey) {
                    listenTracker.begin(song)
                    lastSeenSongKey = key
                    lastEnded = false
                }
                // 2. 自然播完检测：isPlaying=false 且 positionMs>=durationMs 且 durationMs>0
                //    （PlayerController.onPlaybackStateChanged(STATE_ENDED) 会调 skipNext，这里同步 finalize）
                if (song != null && !pb.isPlaying && pb.durationMs > 0 && pb.positionMs >= pb.durationMs && !lastEnded) {
                    listenTracker.finalize(completed = true)
                    lastEnded = true
                    refreshListenSummary()
                }
                // 3. 进度推进 tick（仅 isPlaying 时累加）
                if (song != null && pb.isPlaying) {
                    listenTracker.tick(pb.positionMs, pb.durationMs, pb.isPlaying)
                }
            }
        }
    }

    /** 刷新听歌画像汇总到 UiState（finalize 后调用，或进入首页时调用）。 */
    private fun refreshListenSummary() {
        _state.update {
            it.copy(
                listenSummary = listenTracker.summary(),
                recentListen = listenTracker.recentSongs(5),
            )
        }
    }

    /** 从 SharedPreferences 恢复 FX 存档槽位（对应桌面版 readUserFxArchives）。 */
    private fun loadPersistedFxArchives(): List<FxArchiveSlot> {
        return FxArchives.defaultSlots().map { slot ->
            val json = prefs.getString("fx_archive_${slot.index}", null)
            val snap = json?.let { jsonToSnapshot(it) }
            if (snap != null) slot.copy(savedAt = System.currentTimeMillis(), snapshot = snap) else slot
        }
    }

    /** 搜索历史持久化（对应桌面版 localStorage searchHistory）。 */
    private fun loadSearchHistory(): List<String> =
        prefs.getString("search_history", null)?.split("\n")?.filter { it.isNotEmpty() }.orEmpty()

    private fun saveSearchHistory(history: List<String>) {
        prefs.edit().putString("search_history", history.take(10).joinToString("\n")).apply()
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
        val kw = keywords.trim()
        _state.update { it.copy(searchKeywords = keywords, searchLoading = true) }
        // 记录搜索历史（非空、去重、置顶、最多 10 条）
        if (kw.isNotEmpty()) {
            val newHistory = (listOf(kw) + _state.value.searchHistory.filter { it != kw }).take(10)
            _state.update { it.copy(searchHistory = newHistory) }
            saveSearchHistory(newHistory)
        }
        viewModelScope.launch(handler) {
            val mode = _state.value.searchMode
            when (mode) {
                "podcast" -> {
                    // 播客搜索
                    val pods = runCatching { repo.searchPodcast(keywords) }.getOrNull().orEmpty()
                    _state.update {
                        it.copy(searchResults = emptyList(), podcastSearchResults = pods, searchLoading = false)
                    }
                }
                else -> {
                    // 歌曲：all 时跟随当前 activeSource；netease/qq 强制指定源
                    val src = when (mode) {
                        "netease" -> "netease"
                        "qq" -> "qq"
                        else -> _state.value.activeSource
                    }
                    val res = runCatching { repo.search(keywords, src) }.getOrNull()
                    _state.update {
                        it.copy(searchResults = res?.songs.orEmpty(), podcastSearchResults = emptyList(), searchLoading = false)
                    }
                }
            }
        }
    }

    /** 设置搜索模式 —— 对应桌面版 setSearchMode(mode)。 */
    fun setSearchMode(mode: String) {
        _state.update { it.copy(searchMode = mode, searchResults = emptyList(), podcastSearchResults = emptyList()) }
        // 切换模式后若有现有关键词，自动重新搜索
        val kw = _state.value.searchKeywords
        if (kw.isNotEmpty()) search(kw)
    }

    /** 清空搜索历史 —— 对应桌面版 clearSearchHistory()。 */
    fun clearSearchHistory() {
        _state.update { it.copy(searchHistory = emptyList()) }
        saveSearchHistory(emptyList())
    }

    /** 点击历史 chip 重跑搜索 —— 对应桌面版 runSearchHistory(q)。 */
    fun runSearchHistory(q: String) = search(q)

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
            // 缓存原词，并按当前 lyricSource 决定显示哪套
            // （切到 custom 时保留原词缓存以便回切；切到 original 时直接显示原词）
            _state.update {
                it.copy(
                    currentLyric = lrc,
                    originalLyricsLines = lines,
                    lyricsLines = if (it.lyricSource == "custom") it.lyricsLines else lines,
                )
            }
        }
    }

    // ============ 导航 ============

    fun navigateTo(screen: Screen) {
        // 进入首页时刷新听歌画像（对应桌面版 emptyHomeActive 时 renderHomeDiscover 读 summary）
        if (screen == Screen.HOME) refreshListenSummary()
        _state.update { it.copy(screen = screen) }
    }
    fun backToPlayer() = _state.update { it.copy(screen = Screen.PLAYER) }

    /** 打开/关闭听歌画像详情弹层 —— 对应桌面版 #profile-modal。 */
    fun toggleListenProfile() {
        refreshListenSummary()
        _state.update { it.copy(showListenProfile = !it.showListenProfile) }
    }
    fun dismissListenProfile() = _state.update { it.copy(showListenProfile = false) }

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
        _state.update { it.copy(showCollect = true, collectTargetSong = playback.value.current, collectPlaylists = it.playlists) }
    }

    /** 打开「收藏到歌单」弹窗，目标为搜索结果中的指定曲目 —— 对应桌面版搜索行 collect 按钮。 */
    fun openCollectModalForSong(song: Song) {
        refreshUserPlaylists()
        _state.update { it.copy(showCollect = true, collectTargetSong = song, collectPlaylists = it.playlists) }
    }

    fun toggleCollect() = _state.update { it.copy(showCollect = !it.showCollect, collectTargetSong = null) }

    /** 把当前播放曲目加入指定歌单 —— 对应桌面版 collectCurrentToPlaylist(id)。 */
    fun collectCurrentToPlaylist(playlistId: Long) {
        val song = _state.value.collectTargetSong ?: playback.value.current ?: return
        viewModelScope.launch(handler) {
            val res = runCatching { repo.addSongToPlaylist(playlistId, song.id) }.getOrNull()
            if (res?.code == 200) {
                _state.update { it.copy(showCollect = false, collectTargetSong = null, toast = "已收藏到歌单") }
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
    fun toggleFxPanel() = _state.update { it.copy(showFxPanel = !it.showFxPanel) }
    fun toggleCoverCrop() = _state.update { it.copy(showCoverCrop = !it.showCoverCrop) }
    /** 封面裁剪提交：把裁剪后的方形封面应用到当前曲目的 MediaSession artwork。 */
    fun commitCoverCrop(bitmap: android.graphics.Bitmap) {
        viewModelScope.launch(handler) {
            player.setArtworkBitmap(bitmap)
            _state.update { it.copy(showCoverCrop = false, toast = "已应用裁剪封面") }
        }
    }

    // ---- 自定义歌词 LRC 编辑器（对应桌面版 #custom-lyric-modal）----
    fun toggleCustomLyric() = _state.update { it.copy(showCustomLyric = !it.showCustomLyric) }
    fun setCustomLyricText(text: String) = _state.update { it.copy(customLyricText = text) }
    /** 保存自定义歌词：解析 LRC 并立即应用到当前歌词显示。 */
    fun saveCustomLyric() {
        val raw = _state.value.customLyricText
        val lines = parseLyric(raw)
        // 保留当前原词缓存（若已存在），切到 custom 并应用自定义歌词
        _state.update {
            it.copy(
                lyricsLines = lines,
                lyricSource = "custom",
                showCustomLyric = false,
                toast = "已应用自定义歌词",
            )
        }
    }

    /** 切换歌词源 —— 对应桌面版 setLyricSourceMode(mode)。 */
    fun setLyricSource(mode: String) {
        val normalized = if (mode == "custom") "custom" else "original"
        _state.update { s ->
            val target = if (normalized == "custom") {
                // 切到自定义：若已有自定义文本则解析应用，否则保持当前
                if (s.customLyricText.isNotEmpty()) parseLyric(s.customLyricText) else s.lyricsLines
            } else {
                // 切回原词：恢复缓存
                s.originalLyricsLines
            }
            s.copy(lyricSource = normalized, lyricsLines = target)
        }
    }

    // ---- 色彩实验室（对应桌面版 #color-lab-pop）----
    fun toggleColorLab(target: String = "lyric") = _state.update {
        it.copy(showColorLab = !it.showColorLab, colorLabTarget = target)
    }
    /** 色彩实验室选色后应用到 FxState 指定字段。 */
    fun applyColorLab(color: androidx.compose.ui.graphics.Color) {
        val target = _state.value.colorLabTarget
        val fx = _state.value.fx
        val newFx = when (target) {
            "lyric" -> fx.copy(lyricColor = color, lyricColorMode = "custom")
            "highlight" -> fx.copy(lyricHighlightColor = color, lyricColorMode = "custom")
            "glow" -> fx.copy(lyricGlowColor = color, lyricColorMode = "custom")
            "tint" -> fx.copy(visualTintColor = color, lyricColorMode = "custom")
            "shelfAccent" -> fx.copy(shelfAccent = color)
            "bgColor" -> fx.copy(customBgColor = color, customBgType = "color")
            else -> fx
        }
        _state.update { it.copy(fx = newFx) }
    }

    // ---- 封面取色弹层（对应桌面版 .cover-color-pop）----
    fun toggleCoverColor(target: String = "lyric") = _state.update {
        it.copy(showCoverColor = !it.showCoverColor, coverColorTarget = target)
    }
    /** 封面取色后应用到 FxState 指定字段（复用 applyColorLab 的目标映射逻辑）。 */
    fun applyCoverColor(color: androidx.compose.ui.graphics.Color) {
        val target = _state.value.coverColorTarget
        val fx = _state.value.fx
        val newFx = when (target) {
            "lyric" -> fx.copy(lyricColor = color, lyricColorMode = "custom")
            "highlight" -> fx.copy(lyricHighlightColor = color, lyricColorMode = "custom")
            "glow" -> fx.copy(lyricGlowColor = color, lyricColorMode = "custom")
            "tint" -> fx.copy(visualTintColor = color, lyricColorMode = "custom")
            "shelfAccent" -> fx.copy(shelfAccent = color)
            "bgColor" -> fx.copy(customBgColor = color, customBgType = "color")
            else -> fx
        }
        _state.update { it.copy(fx = newFx) }
    }

    // ---- 自定义背景（对应桌面版 wallpaperType / wallpaperColor / wallpaperImage / wallpaperVideo）----
    /** 切换背景类型：none=默认粒子 / color=纯色 / image=图片 / video=视频。 */
    fun setCustomBgType(type: String) = _state.update {
        it.copy(fx = it.fx.copy(customBgType = type))
    }

    /** 设置纯色背景颜色。 */
    fun setCustomBgColor(color: androidx.compose.ui.graphics.Color) = _state.update {
        it.copy(fx = it.fx.copy(customBgColor = color, customBgType = "color"))
    }

    /** 设置图片/视频背景 URI。type 必须为 "image" 或 "video"。 */
    fun setCustomBgUri(type: String, uri: String) = _state.update {
        it.copy(fx = it.fx.copy(customBgType = type, customBgUri = uri))
    }

    /** 清除自定义背景，回到默认粒子星河。 */
    fun clearCustomBg() = _state.update {
        it.copy(fx = it.fx.copy(customBgType = "none", customBgUri = ""))
    }

    // ---- 视觉引导（对应桌面版 #visual-guide）----
    fun toggleVisualGuide() = _state.update { it.copy(showVisualGuide = !it.showVisualGuide) }
    fun dismissVisualGuide() = _state.update { it.copy(showVisualGuide = false, visualGuideSeen = true) }

    // ---- 本地节奏分析（对应桌面版 #local-beat-modal）----
    fun toggleLocalBeat() = _state.update { it.copy(showLocalBeat = !it.showLocalBeat) }

    // ---- 更新面板（对应桌面版 #update-modal / #update-entry）----
    fun toggleUpdateModal() = _state.update { it.copy(showUpdateModal = !it.showUpdateModal) }
    fun setUpdateAvailable(version: String) = _state.update { it.copy(updateAvailable = true, updateVersion = version) }

    /** 设置更新面板的完整内容 —— 对应桌面版 applyLatestUpdateInfo(info)。 */
    fun setUpdateInfo(version: String, changelog: List<String>, heroMain: String, heroSub: String) {
        _state.update {
            it.copy(
                updateAvailable = true,
                updateVersion = version,
                updateChangelog = changelog,
                updateHeroMain = heroMain,
                updateHeroSub = heroSub,
            )
        }
    }

    /** 模拟下载更新 —— 对应桌面版 startUpdatePreviewDownload()。
     *  移动端无 Electron autoupdater，这里用协程模拟下载进度。 */
    fun startUpdateDownload() {
        if (_state.value.updateDownloading) return
        _state.update { it.copy(updateDownloading = true, updateDownloadProgress = 0f) }
        viewModelScope.launch(handler) {
            for (i in 1..100) {
                delay(60)
                _state.update { it.copy(updateDownloadProgress = i / 100f) }
            }
            _state.update {
                it.copy(
                    updateDownloading = false,
                    updateDownloadProgress = 1f,
                    toast = "新版本已下载，重启应用以完成更新",
                )
            }
        }
    }

    /** 显示/隐藏全局加载浮层 —— 对应桌面版 showLoading()/hideLoading()。 */
    fun showGlobalLoading() = _state.update { it.copy(globalLoading = true) }
    fun hideGlobalLoading() = _state.update { it.copy(globalLoading = false) }

    // ---- 歌曲/歌手详情（对应桌面版 #track-detail-modal openTrackDetailModal）----
    /** 打开详情：type=song 显示歌曲信息+评论，type=artist 显示歌手主页+热门歌曲。 */
    fun openTrackDetail(type: String, songOverride: Song? = null) {
        val song = songOverride ?: playback.value.current
        if (song == null) {
            _state.update { it.copy(toast = "先播放或选择一首歌") }
            return
        }
        _state.update {
            it.copy(
                showTrackDetail = true,
                trackDetailType = type,
                trackDetailSong = song,
                trackDetailArtist = null,
                trackDetailComments = emptyList(),
                trackDetailLoading = true,
            )
        }
        viewModelScope.launch(handler) {
            when (type) {
                "artist" -> {
                    val detail = runCatching { repo.artistDetail(song) }.getOrNull()
                    _state.update { it.copy(trackDetailArtist = detail, trackDetailLoading = false) }
                }
                else -> {
                    val comments = runCatching { repo.comments(song, limit = 18) }.getOrNull().orEmpty()
                    _state.update { it.copy(trackDetailComments = comments, trackDetailLoading = false) }
                }
            }
        }
    }

    fun closeTrackDetail() = _state.update {
        it.copy(showTrackDetail = false, trackDetailArtist = null, trackDetailComments = emptyList(), trackDetailLoading = false)
    }

    // ---- 启动遮罩（#splash dismissSplash）----
    fun dismissSplash() = _state.update { it.copy(showSplash = false) }

    // ---- 新建歌单（对应桌面版 createPlaylistFromCollect）----
    fun createPlaylist(name: String) {
        if (name.isBlank()) {
            _state.update { it.copy(toast = "请输入歌单名称") }
            return
        }
        viewModelScope.launch(handler) {
            val res = runCatching { repo.createPlaylist(name) }.getOrNull()
            if (res?.code == 200 && res.id != 0L) {
                refreshUserPlaylists()
                _state.update { it.copy(toast = "已创建歌单「$name」") }
            } else {
                _state.update { it.copy(toast = "创建失败：${res?.message ?: "未知"}") }
            }
        }
    }

    /** 删除当前曲目的自定义歌词（对应桌面版 deleteCustomLyricForCurrent）。 */
    fun deleteCustomLyric() {
        // 删除自定义歌词：清空自定义文本，切回原词，恢复原词缓存
        _state.update {
            it.copy(
                customLyricText = "",
                lyricSource = "original",
                lyricsLines = it.originalLyricsLines,
                showCustomLyric = false,
                toast = "已删除自定义歌词",
            )
        }
    }

    // ============ 歌单/队列三 tab 面板（对应桌面版 #playlist-panel）============

    /** 切换歌单/队列面板显隐 —— 对应桌面版 togglePlaylistPanel(force)。 */
    fun togglePlaylistPanel() = _state.update { it.copy(showPlaylistPanel = !it.showPlaylistPanel) }

    /** 切换 tab —— 对应桌面版 switchPlaylistTab(tab)。 */
    fun switchPlaylistTab(tab: String) {
        val normalized = when (tab) {
            "podcasts" -> "podcasts"
            "playlists" -> "playlists"
            else -> "queue"
        }
        _state.update { it.copy(playlistPanelTab = normalized) }
        // 切到歌单/播客 tab 时刷新数据
        when (normalized) {
            "playlists" -> refreshUserPlaylists()
            "podcasts" -> { if (_state.value.hotPodcasts.isEmpty()) loadPodcastHot() }
        }
    }

    /** 常开开关 —— 对应桌面版 togglePlaylistPanelPinned()。 */
    fun togglePlaylistPanelPinned() = _state.update { it.copy(playlistPanelPinned = !it.playlistPanelPinned) }

    /** 清空播放队列 —— 对应桌面版 clearQueue()。 */
    fun clearQueue() = player.clearQueue()

    /** 随机打乱队列 —— 对应桌面版 shuffleQueue()。 */
    fun shuffleQueue() = player.shuffleQueue()

    /** 设置音量（0..1）—— 对应桌面版 setVolume()。 */
    fun setVolume(volume: Float) = player.setVolume(volume)

    /** 静音切换 —— 对应桌面版 toggleMute()。 */
    fun toggleMute() = player.toggleMute()

    /** 清除自定义封面 —— 对应桌面版 clearCustomCoverForCurrent()。 */
    fun clearCustomCover() = player.clearArtwork()

    // ============ 试听片段提示（对应桌面版 #trial-banner）============

    /** 显示试听横幅 —— 桌面版在 song/url 返回 data.trial 时触发。 */
    fun showTrialBanner(text: String, loggedIn: Boolean) {
        _state.update { it.copy(showTrialBanner = true, trialText = text, trialLoggedIn = loggedIn) }
    }

    fun dismissTrialBanner() = _state.update { it.copy(showTrialBanner = false) }

    // ============ 节奏/AI 深度状态角标（对应桌面版 #beat-chip / #ai-depth-chip）============

    /** 显示节奏分析角标 —— 对应桌面版 showBeatChip(text)。 */
    fun showBeatChip(text: String) = _state.update { it.copy(beatChipText = text) }
    fun hideBeatChip() = _state.update { it.copy(beatChipText = null) }

    /** 显示 AI 深度估计角标。 */
    fun showAiDepthChip(text: String) = _state.update { it.copy(aiDepthChipText = text) }
    fun hideAiDepthChip() = _state.update { it.copy(aiDepthChipText = null) }

    // ============ 自由镜头提示（对应桌面版 #free-camera-hint）============

    fun toggleFreeCameraHint() = _state.update { it.copy(showFreeCameraHint = !it.showFreeCameraHint) }
    fun dismissFreeCameraHint() = _state.update { it.copy(showFreeCameraHint = false) }

    // ============ 手势 HUD（对应桌面版 #gesture-hud，移动端视觉占位）============

    fun toggleGestureHud() = _state.update { it.copy(showGestureHud = !it.showGestureHud) }
    fun setGestureLabel(label: String) = _state.update { it.copy(gestureLabel = label) }

    // ============ 导入音乐/封面文件（对应桌面版 #upload-btn + #file-input）============

    /** 导入本地文件 —— 对应桌面版 handleFileImport(files)。
     *  ActivityResultContracts.GetMultipleContents() 返回的是 content:// URI，
     *  不能用扩展名判断（多数 MediaStore URI 不带扩展名）。
     *  这里用 ContentResolver 查询 DISPLAY_NAME 与 MIME 类型识别音频，
     *  并申请持久化读权限，保证 PlaybackService 跨进程可读。 */
    fun importFiles(uris: List<String>) {
        if (uris.isEmpty()) {
            _state.update { it.copy(toast = "未选择文件") }
            return
        }
        _state.update {
            it.copy(importedFiles = it.importedFiles + uris, toast = "已导入 ${uris.size} 个文件")
        }
        val resolver = appContext.contentResolver
        val audioExt = setOf("mp3", "flac", "wav", "ogg", "m4a", "aac", "opus")
        // 解析为 (uri, displayName) 的音频列表
        val audio: List<Pair<String, String>> = uris.mapNotNull { uriStr ->
            val uri = runCatching { android.net.Uri.parse(uriStr) }.getOrNull() ?: return@mapNotNull null
            // 1) MIME 前缀 audio/* 直接判定为音频
            val mime = runCatching { resolver.getType(uri) }.getOrNull()
            val name = runCatching {
                resolver.query(uri, null, null, null, null)?.use { c ->
                    val idx = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0 && c.moveToFirst()) c.getString(idx) else null
                }
            }.getOrNull() ?: uriStr.substringAfterLast('/').substringBeforeLast('.')
            val isAudio = mime?.startsWith("audio/") == true ||
                audioExt.any { name.endsWith(".$it", ignoreCase = true) }
            if (isAudio) {
                // 申请持久化读权限（仅 content:// 且含 FLAG_GRANT_READ 时生效）
                runCatching {
                    val flags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                    resolver.takePersistableUriPermission(uri, flags)
                }
                uriStr to name.substringBeforeLast('.')
            } else null
        }
        if (audio.isNotEmpty()) {
            val songs = audio.mapIndexed { i, (uri, name) ->
                Song(
                    id = -1_000_000L - i,
                    name = name.ifBlank { "本地曲目 ${i + 1}" },
                    source = "local",
                    mid = uri,
                )
            }
            val urls = audio.map { it.first }
            player.playQueue(songs, urls, 0)
            _state.update { it.copy(toast = "已加入播放队列 ${songs.size} 首") }
        } else {
            _state.update { it.copy(toast = "未识别到音频文件") }
        }
    }

    // ============ 自动换源提示（对应桌面版 #source-fallback-notice）============

    /** 显示自动换源提示 —— 桌面版在播放源回退时触发。 */
    fun showSourceFallbackNotice(text: String) {
        _state.update { it.copy(showSourceFallback = true, sourceFallbackText = text) }
    }

    fun closeSourceFallbackNotice() = _state.update { it.copy(showSourceFallback = false) }

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
                // 自定义背景（对应桌面版 wallpaperType / wallpaperColor / wallpaperImage / wallpaperVideo）
                customBgType = s.fx.customBgType,
                customBgColor = s.fx.customBgColor.value.toLong(),
                customBgUri = s.fx.customBgUri,
                lyricFont = s.fx.lyricFont,
                lyricLetterSpacing = s.fx.lyricLetterSpacing,
                lyricLineHeight = s.fx.lyricLineHeight,
                lyricWeight = s.fx.lyricWeight,
                lyricScale = s.fx.lyricScale,
                particleSize = s.fx.particleSize,
                particleSpeed = s.fx.particleSpeed,
                particleTwist = s.fx.particleTwist,
                particleColor = s.fx.particleColor,
                particleBloom = s.fx.particleBloom,
                particleScatter = s.fx.particleScatter,
                particleBgFade = s.fx.particleBgFade,
                shelfSize = s.fx.shelfSize,
                shelfX = s.fx.shelfX,
                shelfY = s.fx.shelfY,
                shelfZ = s.fx.shelfZ,
                shelfAngle = s.fx.shelfAngle,
                shelfOpacity = s.fx.shelfOpacity,
                shelfBgAlpha = s.fx.shelfBgAlpha,
                shelfAccent = s.fx.shelfAccent.value.toLong(),
                shelfShowPodcasts = s.fx.shelfShowPodcasts,
                shelfMergeCollections = s.fx.shelfMergeCollections,
                shelfCameraMode = s.fx.shelfCameraMode,
                shelfPresenceMode = s.fx.shelfPresenceMode,
                cameraInteraction = s.fx.cameraInteraction,
            )
            val archives = s.fxArchives.map {
                if (it.index == index) it.copy(savedAt = System.currentTimeMillis(), snapshot = snap) else it
            }
            // 持久化到 SharedPreferences（对应桌面版 localStorage userFxArchives）
            prefs.edit().putString("fx_archive_$index", snapshotToJson(snap)).apply()
            s.copy(fxArchives = archives, toast = "已保存到 ${archives[index].name}")
        }
    }

    /** 导出 FX 存档为 JSON 字符串 —— 对应桌面版 exportUserFxArchive(index)。 */
    fun exportFxArchiveJson(index: Int): String {
        val snap = _state.value.fxArchives.getOrNull(index)?.snapshot ?: return ""
        return snapshotToJson(snap)
    }

    /** 从 JSON 字符串导入 FX 存档 —— 对应桌面版 importUserFxArchiveText(text)。 */
    fun importFxArchiveJson(json: String): Boolean {
        val snap = jsonToSnapshot(json) ?: return false
        _state.update { s ->
            // 写入第一个空槽位，若无空位则覆盖槽位 0
            val targetIndex = s.fxArchives.indexOfFirst { !it.hasSave }.takeIf { it >= 0 } ?: 0
            val archives = s.fxArchives.map {
                if (it.index == targetIndex) it.copy(savedAt = System.currentTimeMillis(), snapshot = snap) else it
            }
            prefs.edit().putString("fx_archive_$targetIndex", json).apply()
            s.copy(fxArchives = archives, toast = "已导入到 ${archives[targetIndex].name}")
        }
        return true
    }

    /** 序列化 FX 快照为 JSON（用 Android 内置 org.json，无需额外依赖）。 */
    private fun snapshotToJson(snap: FxArchiveSnapshot): String {
        val o = org.json.JSONObject()
        o.put("preset", snap.preset)
        o.put("desktopLyrics", snap.desktopLyrics)
        o.put("desktopLyricsSize", snap.desktopLyricsSize)
        o.put("desktopLyricsOpacity", snap.desktopLyricsOpacity)
        o.put("desktopLyricsY", snap.desktopLyricsY)
        o.put("desktopLyricsClickThrough", snap.desktopLyricsClickThrough)
        o.put("desktopLyricsCinema", snap.desktopLyricsCinema)
        o.put("desktopLyricsHighlight", snap.desktopLyricsHighlight)
        o.put("desktopLyricsFps", snap.desktopLyricsFps)
        o.put("lyricColorMode", snap.lyricColorMode)
        o.put("lyricColor", snap.lyricColor)
        o.put("lyricHighlightColor", snap.lyricHighlightColor)
        o.put("lyricGlowColor", snap.lyricGlowColor)
        o.put("visualTintColor", snap.visualTintColor)
        o.put("lyricGlowParticles", snap.lyricGlowParticles)
        o.put("wallpaperMode", snap.wallpaperMode)
        o.put("wallpaperOpacity", snap.wallpaperOpacity)
        // 自定义背景
        o.put("customBgType", snap.customBgType)
        o.put("customBgColor", snap.customBgColor)
        o.put("customBgUri", snap.customBgUri)
        o.put("lyricFont", snap.lyricFont)
        o.put("lyricLetterSpacing", snap.lyricLetterSpacing)
        o.put("lyricLineHeight", snap.lyricLineHeight)
        o.put("lyricWeight", snap.lyricWeight)
        o.put("lyricScale", snap.lyricScale)
        o.put("particleSize", snap.particleSize)
        o.put("particleSpeed", snap.particleSpeed)
        o.put("particleTwist", snap.particleTwist)
        o.put("particleColor", snap.particleColor)
        o.put("particleBloom", snap.particleBloom)
        o.put("particleScatter", snap.particleScatter)
        o.put("particleBgFade", snap.particleBgFade)
        o.put("shelfSize", snap.shelfSize)
        o.put("shelfX", snap.shelfX)
        o.put("shelfY", snap.shelfY)
        o.put("shelfZ", snap.shelfZ)
        o.put("shelfAngle", snap.shelfAngle)
        o.put("shelfOpacity", snap.shelfOpacity)
        o.put("shelfBgAlpha", snap.shelfBgAlpha)
        o.put("shelfAccent", snap.shelfAccent)
        o.put("shelfShowPodcasts", snap.shelfShowPodcasts)
        o.put("shelfMergeCollections", snap.shelfMergeCollections)
        o.put("shelfCameraMode", snap.shelfCameraMode)
        o.put("shelfPresenceMode", snap.shelfPresenceMode)
        o.put("cameraInteraction", snap.cameraInteraction)
        return o.toString()
    }

    /** 从 JSON 解析 FX 快照。 */
    private fun jsonToSnapshot(json: String): FxArchiveSnapshot? {
        return runCatching {
            val o = org.json.JSONObject(json)
            FxArchiveSnapshot(
                preset = o.optInt("preset", 0),
                desktopLyrics = o.optBoolean("desktopLyrics", false),
                desktopLyricsSize = o.optDouble("desktopLyricsSize", 1.0).toFloat(),
                desktopLyricsOpacity = o.optDouble("desktopLyricsOpacity", 0.92).toFloat(),
                desktopLyricsY = o.optDouble("desktopLyricsY", 0.76).toFloat(),
                desktopLyricsClickThrough = o.optBoolean("desktopLyricsClickThrough", false),
                desktopLyricsCinema = o.optBoolean("desktopLyricsCinema", true),
                desktopLyricsHighlight = o.optBoolean("desktopLyricsHighlight", false),
                desktopLyricsFps = o.optInt("desktopLyricsFps", 60),
                lyricColorMode = o.optString("lyricColorMode", "auto"),
                lyricColor = o.optLong("lyricColor", 0xFFA9B8C8),
                lyricHighlightColor = o.optLong("lyricHighlightColor", 0xFFFFF0B8),
                lyricGlowColor = o.optLong("lyricGlowColor", 0xFF9DB8CF),
                visualTintColor = o.optLong("visualTintColor", 0xFF9DB8CF),
                lyricGlowParticles = o.optBoolean("lyricGlowParticles", false),
                wallpaperMode = o.optBoolean("wallpaperMode", false),
                wallpaperOpacity = o.optDouble("wallpaperOpacity", 1.0).toFloat(),
                // 自定义背景
                customBgType = o.optString("customBgType", "none"),
                customBgColor = o.optLong("customBgColor", 0xFF05060A),
                customBgUri = o.optString("customBgUri", ""),
                lyricFont = o.optString("lyricFont", "default"),
                lyricLetterSpacing = o.optDouble("lyricLetterSpacing", 0.0).toFloat(),
                lyricLineHeight = o.optDouble("lyricLineHeight", 1.18).toFloat(),
                lyricWeight = o.optInt("lyricWeight", 400),
                lyricScale = o.optDouble("lyricScale", 1.0).toFloat(),
                particleSize = o.optDouble("particleSize", 1.0).toFloat(),
                particleSpeed = o.optDouble("particleSpeed", 1.0).toFloat(),
                particleTwist = o.optDouble("particleTwist", 1.0).toFloat(),
                particleColor = o.optDouble("particleColor", 1.0).toFloat(),
                particleBloom = o.optDouble("particleBloom", 1.0).toFloat(),
                particleScatter = o.optDouble("particleScatter", 1.0).toFloat(),
                particleBgFade = o.optDouble("particleBgFade", 1.0).toFloat(),
                shelfSize = o.optDouble("shelfSize", 1.0).toFloat(),
                shelfX = o.optDouble("shelfX", 0.0).toFloat(),
                shelfY = o.optDouble("shelfY", 0.0).toFloat(),
                shelfZ = o.optDouble("shelfZ", 0.0).toFloat(),
                shelfAngle = o.optDouble("shelfAngle", 0.0).toFloat(),
                shelfOpacity = o.optDouble("shelfOpacity", 1.0).toFloat(),
                shelfBgAlpha = o.optDouble("shelfBgAlpha", 0.0).toFloat(),
                shelfAccent = o.optLong("shelfAccent", 0xFFF4D28A),
                shelfShowPodcasts = o.optBoolean("shelfShowPodcasts", false),
                shelfMergeCollections = o.optBoolean("shelfMergeCollections", false),
                shelfCameraMode = o.optInt("shelfCameraMode", 0),
                shelfPresenceMode = o.optInt("shelfPresenceMode", 0),
                cameraInteraction = o.optInt("cameraInteraction", 0),
            )
        }.getOrNull()
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
                // 自定义背景
                customBgType = snap.customBgType,
                customBgColor = Color(snap.customBgColor),
                customBgUri = snap.customBgUri,
                lyricFont = snap.lyricFont,
                lyricLetterSpacing = snap.lyricLetterSpacing,
                lyricLineHeight = snap.lyricLineHeight,
                lyricWeight = snap.lyricWeight,
                lyricScale = snap.lyricScale,
                particleSize = snap.particleSize,
                particleSpeed = snap.particleSpeed,
                particleTwist = snap.particleTwist,
                particleColor = snap.particleColor,
                particleBloom = snap.particleBloom,
                particleScatter = snap.particleScatter,
                particleBgFade = snap.particleBgFade,
                shelfSize = snap.shelfSize,
                shelfX = snap.shelfX,
                shelfY = snap.shelfY,
                shelfZ = snap.shelfZ,
                shelfAngle = snap.shelfAngle,
                shelfOpacity = snap.shelfOpacity,
                shelfBgAlpha = snap.shelfBgAlpha,
                shelfAccent = Color(snap.shelfAccent),
                shelfShowPodcasts = snap.shelfShowPodcasts,
                shelfMergeCollections = snap.shelfMergeCollections,
                shelfCameraMode = snap.shelfCameraMode,
                shelfPresenceMode = snap.shelfPresenceMode,
                cameraInteraction = snap.cameraInteraction,
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
        return MainViewModel(app.repository, app.playerController, app.applicationContext) as T
    }
}
