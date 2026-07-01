package com.mineradio.player.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import coil.request.ImageRequest
import coil.size.Size
import androidx.core.graphics.drawable.toBitmap
import com.mineradio.player.data.api.dto.Podcast
import com.mineradio.player.data.api.dto.Song
import com.mineradio.player.render.GalaxyState
import com.mineradio.player.render.ParticleGalaxyBackground
import com.mineradio.player.render.Shelf3DPanel
import com.mineradio.player.render.ShelfRenderer
import com.mineradio.player.ui.MainViewModel
import com.mineradio.player.ui.Screen
import com.mineradio.player.ui.UiState
import com.mineradio.player.ui.component.*
import com.mineradio.player.ui.theme.MineradioColors

/**
 * 主外壳 —— 横屏布局：左侧歌词舞台 + 右侧上下文侧栏（搜索/首页/歌单/详情）。
 * 底部固定播放控制条。顶部标题栏带全部入口（登录/首页/歌单架/播客/评论/DIY/沉浸/设置）。
 *
 * 全屏浮层：登录 / 账户 / 评论 / 播客 / 3D 歌单架 / 设置 / DIY 系列。
 *
 * 设计对应桌面版 index.html 的整体结构与导航流。
 */
@Composable
fun PlayerShell(
    vm: MainViewModel,
    state: UiState,
) {
    val playback by vm.playback.collectAsState()
    val galaxyState = remember { GalaxyState() }
    var searchOpen by remember { mutableStateOf(false) }
    var searchText by remember { mutableStateOf("") }

    // 粒子背景配色：custom 模式用 FxState 自定义色，否则用默认 wallpaper 配色
    val galaxyColors = state.fx.overlayColors()

    // 封面裁剪：showCoverCrop 打开时异步加载当前曲目封面为 Bitmap
    val context = LocalContext.current
    var coverBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    LaunchedEffect(state.showCoverCrop, playback.current?.displayCover) {
        if (state.showCoverCrop) {
            val url = playback.current?.displayCover.orEmpty()
            coverBitmap = if (url.isEmpty()) null else runCatching {
                val loader = ImageLoader(context)
                val req = ImageRequest.Builder(context).data(url).size(Size.ORIGINAL).build()
                loader.execute(req).drawable?.let { d ->
                    if (d is android.graphics.drawable.BitmapDrawable) d.bitmap
                    else d.toBitmap()
                }
            }.getOrNull()
        } else {
            coverBitmap = null
        }
    }

    // 首次进入自动弹出视觉引导（对应桌面版 onboarding 检查 localStorage.visualGuideSeen）
    LaunchedEffect(Unit) {
        if (!state.visualGuideSeen && !state.showVisualGuide) {
            vm.toggleVisualGuide()
        }
    }

    Box(Modifier.fillMaxSize().background(MineradioColors.FcBg)) {
        // 1. 粒子星河背景（永远铺底）
        ParticleGalaxyBackground(
            state = galaxyState,
            playing = playback.isPlaying,
            coverUrl = playback.current?.displayCover,
            opacity = 1f,
            preset = state.fx.preset,
            title = playback.current?.name ?: "Mineradio",
            artist = playback.current?.displayArtist ?: "",
            primaryColor = galaxyColors.primary,
            secondaryColor = galaxyColors.secondary,
            highlightColor = galaxyColors.highlight,
            glowColor = galaxyColors.glow,
            particleSize = state.fx.particleSize,
            particleSpeed = state.fx.particleSpeed,
            particleTwist = state.fx.particleTwist,
            particleColor = state.fx.particleColor,
            particleBloom = state.fx.particleBloom,
            particleScatter = state.fx.particleScatter,
            particleBgFade = state.fx.particleBgFade,
            modifier = Modifier.fillMaxSize(),
        )

        // 2. 主内容区（横屏左右分栏）—— 沉浸模式下隐藏侧栏，歌词舞台占满
        Row(Modifier.fillMaxSize()) {
            // 左：歌词舞台
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                if (state.showLyricsPanel) {
                    LyricStage(
                        lines = state.lyricsLines,
                        positionMs = playback.positionMs,
                        fx = state.fx,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                // 当前曲目信息（左下）—— 仅显示标题/艺人，喜欢与评论入口在底栏与歌曲详情弹窗
                if (playback.current != null) {
                    Column(
                        Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 24.dp, bottom = 110.dp)
                    ) {
                        Text(
                            playback.current!!.name,
                            color = MineradioColors.FcInk,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            playback.current!!.displayArtist,
                            color = MineradioColors.FcMuted,
                            fontSize = 14.sp,
                        )
                    }
                }
            }

            // 右：侧栏（上下文路由）—— 沉浸模式下隐藏
            AnimatedVisibility(visible = !state.immersiveMode, enter = fadeIn(), exit = fadeOut()) {
                SideRouter(
                    vm = vm,
                    state = state,
                    searchOpen = searchOpen,
                    searchText = searchText,
                    onSearchToggle = { searchOpen = !searchOpen; if (!searchOpen) searchText = "" },
                    onSearchChange = { searchText = it; vm.search(it) },
                    onSongClick = { song ->
                        val queue = when {
                            searchText.isNotEmpty() -> state.searchResults
                            state.screen == Screen.PLAYLIST_DETAIL -> state.currentPlaylistTracks
                            else -> state.discover?.dailyRecommend.orEmpty()
                        }
                        vm.playSong(song, queue)
                    },
                    modifier = Modifier
                        .width(440.dp)
                        .fillMaxHeight()
                        .padding(16.dp),
                )
            }
        }

        // 3. 顶部标题栏 —— 沉浸模式下隐藏
        AnimatedVisibility(
            visible = !state.immersiveMode,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            TopBar(
                vm = vm,
                state = state,
                onSearchClick = { searchOpen = true },
            )
        }

        // 4. 底部控制条
        BottomControlBar(
            state = playback,
            liked = playback.current?.let { vm.isLiked(it) } ?: false,
            quality = state.playbackQuality,
            showLyricsPanel = state.showLyricsPanel,
            controlsAutoHide = state.controlsAutoHide,
            immersiveMode = state.immersiveMode,
            onPlayPause = vm::playPause,
            onSkipPrev = vm::skipPrev,
            onSkipNext = vm::skipNext,
            onSeek = vm::seekTo,
            onToggleLike = { playback.current?.let { vm.toggleLike(it) } },
            onCollect = vm::openCollectModalForCurrent,
            onCyclePlayMode = vm::cyclePlayMode,
            onToggleMiniQueue = vm::toggleMiniQueue,
            onToggleLyricsPanel = vm::toggleLyricsPanel,
            onToggleControlsAutoHide = vm::toggleControlsAutoHide,
            onToggleImmersive = vm::toggleImmersive,
            onQualityChange = vm::setQuality,
            onSongClick = { song, queue -> vm.playSong(song, queue) },
            queue = playback.queue,
            showMiniQueue = state.showMiniQueue,
            collectPlaylists = state.collectPlaylists,
            showCollect = state.showCollect,
            onCollectToPlaylist = vm::collectCurrentToPlaylist,
            onDismissCollect = vm::toggleCollect,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp),
        )

        // 5. 搜索框浮层（顶部）
        AnimatedVisibility(
            visible = searchOpen,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 72.dp, start = 24.dp, end = 24.dp),
        ) {
            SearchBar(
                value = searchText,
                onValueChange = { searchText = it; vm.search(it) },
                onSubmit = { vm.search(searchText) },
                modifier = Modifier.fillMaxWidth(),
                searchMode = state.searchMode,
                onModeChange = vm::setSearchMode,
            )
        }

        // 6. 设置面板浮层
        AnimatedVisibility(visible = state.showSettings, enter = fadeIn(), exit = fadeOut()) {
            SettingsPanel(
                currentBackend = state.backendUrl,
                onSave = { vm.setBackendUrl(it); vm.toggleSettings() },
                onDismiss = vm::toggleSettings,
            )
        }

        // 6.5 DIY 浮层集合
        AnimatedVisibility(visible = state.showDesktopLyricsDiy, enter = fadeIn(), exit = fadeOut()) {
            DiyOverlayPanel(title = "桌面歌词 DIY", onClose = vm::toggleDesktopLyricsDiy) {
                DesktopLyricsDiyPanel(
                    enabled = state.fx.desktopLyrics,
                    onEnabledChange = { v -> vm.updateFx { it.copy(desktopLyrics = v) } },
                    size = state.fx.desktopLyricsSize,
                    onSizeChange = { v -> vm.updateFx { it.copy(desktopLyricsSize = v) } },
                    opacity = state.fx.desktopLyricsOpacity,
                    onOpacityChange = { v -> vm.updateFx { it.copy(desktopLyricsOpacity = v) } },
                    yPos = state.fx.desktopLyricsY,
                    onYChange = { v -> vm.updateFx { it.copy(desktopLyricsY = v) } },
                    locked = state.fx.desktopLyricsClickThrough,
                    onLockedChange = { v -> vm.updateFx { it.copy(desktopLyricsClickThrough = v) } },
                    fps = state.fx.desktopLyricsFps,
                    onFpsChange = { v -> vm.updateFx { it.copy(desktopLyricsFps = v) } },
                    lyricColor = state.fx.lyricColor,
                    onLyricColorChange = { v -> vm.updateFx { it.copy(lyricColorMode = "custom", lyricColor = v) } },
                    highlightColor = state.fx.lyricHighlightColor,
                    onHighlightColorChange = { v -> vm.updateFx { it.copy(lyricColorMode = "custom", lyricHighlightColor = v) } },
                    glowColor = state.fx.lyricGlowColor,
                    onGlowColorChange = { v -> vm.updateFx { it.copy(lyricColorMode = "custom", lyricGlowColor = v) } },
                    tintColor = state.fx.visualTintColor,
                    onTintColorChange = { v -> vm.updateFx { it.copy(lyricColorMode = "custom", visualTintColor = v) } },
                )
            }
        }
        AnimatedVisibility(visible = state.showWallpaperDiy, enter = fadeIn(), exit = fadeOut()) {
            DiyOverlayPanel(title = "壁纸模式 DIY", onClose = vm::toggleWallpaperDiy) {
                WallpaperDiyPanel(
                    enabled = state.fx.wallpaperMode,
                    onEnabledChange = { v -> vm.updateFx { it.copy(wallpaperMode = v) } },
                    devLocked = state.fx.wallpaperDevLocked,
                    opacity = state.fx.wallpaperOpacity,
                    onOpacityChange = { v -> vm.updateFx { it.copy(wallpaperOpacity = v) } },
                    preset = state.fx.preset,
                    onPresetChange = { v -> vm.updateFx { it.copy(preset = v) } },
                )
            }
        }
        AnimatedVisibility(visible = state.showFxArchives, enter = fadeIn(), exit = fadeOut()) {
            DiyOverlayPanel(title = "FX 存档", onClose = vm::toggleFxArchives) {
                FxArchiveSlots(
                    slots = state.fxArchives,
                    onSave = vm::saveFxArchive,
                    onLoad = vm::loadFxArchive,
                )
            }
        }
        AnimatedVisibility(visible = state.showCoverCrop, enter = fadeIn(), exit = fadeOut()) {
            if (coverBitmap != null) {
                CoverCropModal(bitmap = coverBitmap, onCommit = vm::commitCoverCrop, onDismiss = vm::toggleCoverCrop)
            } else {
                DiyOverlayPanel(title = "裁剪封面", onClose = vm::toggleCoverCrop) {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("正在加载封面…", color = MineradioColors.FcMuted, fontSize = 13.sp)
                    }
                }
            }
        }

        // 6.5.1 FX 视觉控制台（总面板，对应桌面版 #fx-panel）
        AnimatedVisibility(visible = state.showFxPanel, enter = fadeIn(), exit = fadeOut()) {
            DiyOverlayPanel(title = "视觉控制台", onClose = vm::toggleFxPanel) {
                FxPanel(
                    fx = state.fx,
                    onUpdate = vm::updateFx,
                    onOpenColorLab = { target -> vm.toggleColorLab(target) },
                )
            }
        }
        // 6.5.2 自定义歌词 LRC 编辑器（#custom-lyric-modal）
        AnimatedVisibility(visible = state.showCustomLyric, enter = fadeIn(), exit = fadeOut()) {
            CustomLyricModal(
                text = state.customLyricText,
                onTextChange = vm::setCustomLyricText,
                onSave = vm::saveCustomLyric,
                onDismiss = vm::toggleCustomLyric,
            )
        }
        // 6.5.3 色彩实验室（#color-lab-pop）—— 初始色由 target 字段决定
        AnimatedVisibility(visible = state.showColorLab, enter = fadeIn(), exit = fadeOut()) {
            val initialColor = when (state.colorLabTarget) {
                "lyric" -> state.fx.lyricColor
                "highlight" -> state.fx.lyricHighlightColor
                "glow" -> state.fx.lyricGlowColor
                "tint" -> state.fx.visualTintColor
                "shelfAccent" -> state.fx.shelfAccent
                else -> state.fx.lyricColor
            }
            ColorLabPop(
                initialColor = initialColor,
                target = state.colorLabTarget,
                onPick = vm::applyColorLab,
                onDismiss = { vm.toggleColorLab(state.colorLabTarget) },
            )
        }
        // 6.5.4 本地节奏分析（#local-beat-modal）
        AnimatedVisibility(visible = state.showLocalBeat, enter = fadeIn(), exit = fadeOut()) {
            LocalBeatModal(onDismiss = vm::toggleLocalBeat)
        }
        // 6.5.5 更新面板（#update-modal）
        AnimatedVisibility(visible = state.showUpdateModal, enter = fadeIn(), exit = fadeOut()) {
            DiyOverlayPanel(title = "发现新版本", onClose = vm::toggleUpdateModal) {
                Column {
                    Text(
                        if (state.updateAvailable) "新版本 ${state.updateVersion} 可用" else "已是最新版本",
                        color = MineradioColors.FcInk,
                        fontSize = 14.sp,
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Text(
                            "暂不更新",
                            color = MineradioColors.FcMuted,
                            fontSize = 12.sp,
                            modifier = Modifier.clickable { vm.toggleUpdateModal() }.padding(8.dp),
                        )
                    }
                }
            }
        }

        // 6.5.6 视觉引导（#visual-guide）—— 全屏遮罩，置顶
        AnimatedVisibility(visible = state.showVisualGuide, enter = fadeIn(), exit = fadeOut()) {
            VisualGuide(onDismiss = vm::dismissVisualGuide)
        }

        // 6.6 登录浮层
        AnimatedVisibility(visible = state.showLogin, enter = fadeIn(), exit = fadeOut()) {
            LoginPanel(
                provider = state.loginProvider,
                onProviderChange = { vm.setLoginProvider(it) },
                qrImgUrl = state.qrImgUrl,
                qrStatus = state.qrStatus,
                qqCookieInput = state.qqCookieInput,
                onQqCookieChange = vm::onQqCookieInput,
                onRefreshQr = vm::refreshQr,
                onSubmitQqCookie = vm::submitQqCookie,
                onDismiss = vm::toggleLogin,
            )
        }
        // 6.7 账户浮层
        AnimatedVisibility(visible = state.showAccount, enter = fadeIn(), exit = fadeOut()) {
            AccountPanel(
                neteaseLogin = state.neteaseLogin,
                qqLogin = state.qqLogin,
                activeProvider = state.activeSource,
                onSwitchProvider = vm::setSource,
                onLogout = vm::logout,
                onDismiss = vm::toggleAccount,
            )
        }
        // 6.8 评论浮层
        AnimatedVisibility(visible = state.showComments, enter = fadeIn(), exit = fadeOut()) {
            CommentsPanel(
                song = state.commentsSong,
                comments = state.comments,
                onDismiss = vm::dismissComments,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp)
                    .width(520.dp)
                    .height(560.dp),
            )
        }
        // 6.9 播客浮层
        AnimatedVisibility(visible = state.showPodcast, enter = fadeIn(), exit = fadeOut()) {
            PodcastPanel(
                hotPodcasts = state.hotPodcasts,
                selectedPodcast = state.selectedPodcast,
                programs = state.podcastPrograms,
                onPodcastClick = vm::selectPodcast,
                onProgramClick = vm::playPodcastProgram,
                onBack = vm::backToPodcastList,
                onDismiss = vm::dismissPodcast,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp)
                    .width(720.dp)
                    .height(560.dp),
            )
        }
        // 6.10 3D 歌单架浮层（全屏）
        AnimatedVisibility(visible = state.showShelf, enter = fadeIn(), exit = fadeOut()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(MineradioColors.FcBg.copy(alpha = 0.96f))
                    .clickable(enabled = false) {},
            ) {
                Shelf3DPanel(
                    playlists = state.playlists,
                    selectedIndex = state.shelfSelectedIndex,
                    mode = state.shelfMode,
                    onSelect = vm::setShelfSelected,
                    onModeChange = vm::setShelfMode,
                    modifier = Modifier.fillMaxSize(),
                    shelfSize = state.fx.shelfSize,
                    shelfX = state.fx.shelfX,
                    shelfY = state.fx.shelfY,
                    shelfZ = state.fx.shelfZ,
                    shelfAngle = state.fx.shelfAngle,
                    shelfOpacity = state.fx.shelfOpacity,
                    shelfBgAlpha = state.fx.shelfBgAlpha,
                    shelfAccent = state.fx.shelfAccent,
                )
                // 顶部操作条：打开选中歌单 / 关闭
                Row(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = vm::openShelfSelected) {
                        Icon(Icons.Filled.Bookmarks, "打开选中歌单", tint = MineradioColors.FcAccent)
                    }
                    IconButton(onClick = vm::toggleShelf) {
                        Icon(Icons.Filled.Settings, "关闭", tint = MineradioColors.FcInk2)
                    }
                }
            }
        }

        // 6.11 DIY 浮区按钮（右上角，常驻，对应 #fullscreen-diy-zone）
        DiyFloatingZone(
            visible = !state.immersiveMode,
            diyMode = state.fx.diyMode,
            onToggle = vm::toggleDiyMode,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 12.dp, end = 60.dp),
        )

        // 7. Toast
        state.toast?.let { msg ->
            Box(
                Modifier
                    .align(Alignment.Center)
                    .padding(24.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MineradioColors.GlassDark.copy(alpha = 0.92f))
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .clickable { vm.dismissToast() },
            ) {
                Text(msg, color = MineradioColors.FcInk, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun TopBar(
    vm: MainViewModel,
    state: UiState,
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "MINERADIO",
            color = MineradioColors.Champagne,
            fontSize = 16.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 2.sp,
        )
        Spacer(Modifier.weight(1f))
        // 登录态指示 + 账户入口
        val ne = state.neteaseLogin
        val qq = state.qqLogin
        val loggedIn = ne?.loggedIn == true || qq?.loggedIn == true
        if (loggedIn) {
            Text(
                "${ne?.nickname ?: qq?.nickname ?: ""}",
                color = MineradioColors.FcMuted,
                fontSize = 11.sp,
            )
            IconButton(onClick = vm::toggleAccount) {
                Icon(Icons.Filled.AccountCircle, "账户", tint = MineradioColors.Champagne)
            }
            Spacer(Modifier.width(4.dp))
        } else {
            IconButton(onClick = vm::toggleLogin) {
                Icon(Icons.Filled.AccountCircle, "登录", tint = MineradioColors.FcInk2)
            }
        }
        // 主导航入口
        IconButton(onClick = { vm.navigateTo(Screen.HOME) }) {
            Icon(Icons.Filled.Home, "首页", tint = MineradioColors.FcInk2)
        }
        IconButton(onClick = vm::openPlaylistLibrary) {
            Icon(Icons.Filled.Bookmarks, "我的歌单", tint = MineradioColors.FcInk2)
        }
        IconButton(onClick = vm::toggleShelf) {
            Icon(Icons.Filled.ViewCarousel, "3D 歌单架", tint = MineradioColors.FcInk2)
        }
        IconButton(onClick = vm::togglePodcast) {
            Icon(Icons.Filled.GraphicEq, "播客", tint = MineradioColors.FcInk2)
        }
        IconButton(onClick = onSearchClick) {
            Icon(Icons.Filled.Search, "搜索", tint = MineradioColors.FcInk2)
        }
        // DIY 入口：视觉控制台 / 桌面歌词 / 壁纸 / FX 存档 / 自定义歌词 / 本地节奏 / 封面裁剪 / 沉浸模式
        IconButton(onClick = vm::toggleFxPanel) {
            Icon(Icons.Filled.Dashboard, "视觉控制台", tint = MineradioColors.FcInk2)
        }
        IconButton(onClick = vm::toggleDesktopLyricsDiy) {
            Icon(Icons.Filled.Tune, "桌面歌词 DIY", tint = MineradioColors.FcInk2)
        }
        IconButton(onClick = vm::toggleWallpaperDiy) {
            Icon(Icons.Filled.Wallpaper, "壁纸 DIY", tint = MineradioColors.FcInk2)
        }
        IconButton(onClick = vm::toggleFxArchives) {
            Icon(Icons.Filled.PhotoLibrary, "FX 存档", tint = MineradioColors.FcInk2)
        }
        IconButton(onClick = vm::toggleCustomLyric) {
            Icon(Icons.Filled.Edit, "自定义歌词", tint = MineradioColors.FcInk2)
        }
        IconButton(onClick = vm::toggleLocalBeat) {
            Icon(Icons.Filled.Speed, "本地节奏分析", tint = MineradioColors.FcInk2)
        }
        IconButton(onClick = vm::toggleCoverCrop) {
            Icon(Icons.Filled.Image, "封面裁剪", tint = MineradioColors.FcInk2)
        }
        IconButton(onClick = vm::toggleImmersive) {
            Icon(Icons.Filled.Fullscreen, "沉浸模式", tint = if (state.immersiveMode) MineradioColors.FcAccent else MineradioColors.FcInk2)
        }
        IconButton(onClick = vm::toggleVisualGuide) {
            Icon(Icons.Filled.HelpOutline, "使用引导", tint = MineradioColors.FcInk2)
        }
        // 设置 + 更新角标（#update-entry）
        IconButton(onClick = if (state.updateAvailable) vm::toggleUpdateModal else vm::toggleSettings) {
            BadgedBox(badge = {
                if (state.updateAvailable) {
                    Badge { Text("${state.updateVersion}", fontSize = 9.sp) }
                }
            }) {
                Icon(Icons.Filled.Settings, "设置", tint = MineradioColors.FcInk2)
            }
        }
    }
}

/**
 * 右侧侧栏路由 —— 根据 search / screen 状态显示不同内容：
 *  - 有搜索词 → 搜索结果列表
 *  - screen=HOME → 首页网格
 *  - screen=PLAYLIST_LIBRARY → 我的歌单列表
 *  - screen=PLAYLIST_DETAIL → 歌单详情
 *  - 默认 → 每日推荐
 */
@Composable
private fun SideRouter(
    vm: MainViewModel,
    state: UiState,
    searchOpen: Boolean,
    searchText: String,
    onSearchToggle: () -> Unit,
    onSearchChange: (String) -> Unit,
    onSongClick: (Song) -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        searchText.isNotEmpty() -> SearchSidePanel(
            vm = vm,
            state = state,
            searchText = searchText,
            onSearchChange = onSearchChange,
            onSongClick = onSongClick,
            modifier = modifier,
        )
        state.screen == Screen.HOME -> Box(modifier) {
            HomeGrid(
                discover = state.discover,
                weatherRadio = state.weatherRadio,
                onLibraryClick = vm::openPlaylistLibrary,
                onDailyClick = vm::playDailyRecommend,
                onPrivateRadioClick = vm::playPrivateRadio,
                onContinueClick = vm::playRecentVoice,
                onProfileClick = { vm.navigateTo(Screen.PLAYER) },
                onWeatherSongClick = vm::playWeatherSong,
                onTileClick = { /* tile 点击暂复用每日推荐 */ vm.playDailyRecommend() },
            )
        }
        state.screen == Screen.PLAYLIST_LIBRARY -> PlaylistLibrary(
            playlists = state.playlists,
            onPlaylistClick = vm::openPlaylistDetail,
            onBack = vm::backToPlayer,
            modifier = modifier,
        )
        state.screen == Screen.PLAYLIST_DETAIL -> PlaylistDetail(
            playlist = state.selectedPlaylist,
            tracks = state.currentPlaylistTracks,
            onPlayAll = vm::playAllPlaylist,
            onSongClick = onSongClick,
            onBack = vm::backToPlayer,
            modifier = modifier,
        )
        else -> DailyRecommendSidePanel(
            state = state,
            onSongClick = onSongClick,
            onSearchToggle = onSearchToggle,
            modifier = modifier,
        )
    }
}

@Composable
private fun SearchSidePanel(
    vm: MainViewModel,
    state: UiState,
    searchText: String,
    onSearchChange: (String) -> Unit,
    onSongClick: (Song) -> Unit,
    modifier: Modifier = Modifier,
) {
    GlassPanel(modifier = modifier, cornerRadius = 28) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            val isPodcastMode = state.searchMode == "podcast"
            Text(
                if (isPodcastMode) "播客结果" else "搜索结果",
                color = MineradioColors.FcInk,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(8.dp))
            if (state.searchLoading) {
                Text("搜索中…", color = MineradioColors.FcMuted, fontSize = 12.sp)
            } else if (isPodcastMode) {
                PodcastSearchResults(
                    podcasts = state.podcastSearchResults,
                    onPodcastClick = vm::selectPodcast,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                LikeableSongList(
                    songs = state.searchResults,
                    likedMap = state.likedSongMap,
                    onSongClick = onSongClick,
                    onToggleLike = vm::toggleLike,
                    onCheckLikes = vm::checkLikes,
                )
            }
        }
    }
}

/** 播客搜索结果列表（点击进入播客详情）。 */
@Composable
private fun PodcastSearchResults(
    podcasts: List<Podcast>,
    onPodcastClick: (Podcast) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (podcasts.isEmpty()) {
        Text("暂无播客结果", color = MineradioColors.FcMuted, fontSize = 12.sp)
        return
    }
    LazyColumn(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        items(podcasts) { pod ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onPodcastClick(pod) }
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier.size(44.dp).clip(RoundedCornerShape(8.dp)).background(MineradioColors.GlassDark),
                    contentAlignment = Alignment.Center,
                ) {
                    if (!pod.cover.isNullOrEmpty()) {
                        coil.compose.AsyncImage(
                            model = pod.cover,
                            contentDescription = pod.name,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Icon(Icons.Filled.GraphicEq, null, tint = MineradioColors.FcMuted, modifier = Modifier.size(20.dp))
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        pod.name,
                        color = MineradioColors.FcInk,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "${pod.dj?.nickname ?: "未知主播"}  ·  ${pod.programCount} 期",
                        color = MineradioColors.FcMuted,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun DailyRecommendSidePanel(
    state: UiState,
    onSongClick: (Song) -> Unit,
    onSearchToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GlassPanel(modifier = modifier, cornerRadius = 28) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            val songs = state.discover?.dailyRecommend.orEmpty()
            Text("每日推荐", color = MineradioColors.FcInk, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            LikeableSongList(
                songs = songs,
                likedMap = state.likedSongMap,
                onSongClick = onSongClick,
                onToggleLike = { /* 默认列表的喜欢走当前播放曲 */ },
                onCheckLikes = { /* 首页推荐不做批量喜欢检查 */ },
            )
        }
    }
}
