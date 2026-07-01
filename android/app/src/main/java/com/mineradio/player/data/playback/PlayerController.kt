package com.mineradio.player.data.playback

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.mineradio.player.data.api.dto.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 播放控制器 —— 桥接 UI 与 MediaController（PlaybackService）。
 * 维护播放队列、当前曲目、播放状态、进度，并提供 play/pause/seek/skip 等命令。
 */
class PlayerController(private val context: Context) {

    data class PlaybackState(
        val current: Song? = null,
        val isPlaying: Boolean = false,
        val isLoading: Boolean = false,
        val positionMs: Long = 0L,
        val durationMs: Long = 0L,
        val queue: List<Song> = emptyList(),
        val queueIndex: Int = -1,
        val error: String? = null,
        // 播放模式（对应桌面版 playMode：0=顺序播放 REPEAT_MODE_OFF / 1=单曲循环 REPEAT_MODE_ONE / 2=列表循环 REPEAT_MODE_ALL）
        val repeatMode: Int = Player.REPEAT_MODE_OFF,
        // 随机播放（桌面版 playMode=='shuffle' 时为 true）
        val shuffle: Boolean = false,
        // 音量（0..1，对应桌面版 #volume-slider，移动端映射到 STREAM_MUSIC）
        val volume: Float = 1f,
        // 静音（对应桌面版 toggleMute）
        val muted: Boolean = false,
    )

    private val _state = MutableStateFlow(PlaybackState())
    val state: StateFlow<PlaybackState> = _state.asStateFlow()

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var progressJob: Job? = null
    private val audioManager by lazy {
        context.getSystemService(android.content.Context.AUDIO_SERVICE) as android.media.AudioManager
    }

    fun connect() {
        if (controllerFuture != null) return
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        controllerFuture = future
        future.addListener({
            val c = future.get()
            controller = c
            attachListeners(c)
        }, MoreExecutors.directExecutor())
    }

    fun disconnect() {
        progressJob?.cancel()
        controller?.release()
        controller = null
        controllerFuture = null
    }

    private fun attachListeners(c: MediaController) {
        c.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _state.update { it.copy(isPlaying = isPlaying) }
                if (isPlaying) startProgress() else stopProgress()
            }
            override fun onIsLoadingChanged(isLoading: Boolean) {
                _state.update { it.copy(isLoading = isLoading) }
            }
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_READY -> _state.update { it.copy(durationMs = c.duration.coerceAtLeast(0)) }
                    Player.STATE_ENDED -> skipNext()
                    else -> {}
                }
            }
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val idx = c.currentMediaItemIndex
                val q = _state.value.queue
                val song = q.getOrNull(idx)
                _state.update {
                    it.copy(current = song, queueIndex = idx, positionMs = 0L, durationMs = c.duration.coerceAtLeast(0))
                }
            }
            override fun onRepeatModeChanged(repeatMode: Int) {
                _state.update { it.copy(repeatMode = repeatMode) }
            }
            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                _state.update { it.copy(shuffle = shuffleModeEnabled) }
            }
        })
        // 初始同步一次当前播放器配置
        val initVol = currentStreamVolumeFraction()
        _state.update {
            it.copy(
                repeatMode = c.repeatMode,
                shuffle = c.shuffleModeEnabled,
                volume = initVol,
                muted = initVol <= 0f,
            )
        }
    }

    private fun startProgress() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (true) {
                val c = controller ?: break
                _state.update { it.copy(positionMs = c.currentPosition.coerceAtLeast(0)) }
                delay(500)
            }
        }
    }

    private fun stopProgress() {
        progressJob?.cancel()
        progressJob = null
    }

    /** 设置队列并从指定索引开始播放。urls[i] 为对应歌曲的可播放 URL（已套后端代理）。 */
    fun playQueue(songs: List<Song>, urls: List<String>, startIndex: Int = 0) {
        val c = controller ?: return
        require(songs.size == urls.size) { "songs 与 urls 数量不一致" }
        _state.update { it.copy(queue = songs, queueIndex = startIndex) }
        val items = songs.mapIndexed { i, s -> s.toMediaItem(urls[i]) }
        c.setMediaItems(items, startIndex, 0L)
        c.prepare()
        c.play()
    }

    /** 替换单曲队列并播放。 */
    fun playSingle(song: Song, url: String) {
        playQueue(listOf(song), listOf(url), 0)
    }

    fun playPause() {
        val c = controller ?: return
        if (c.isPlaying) c.pause() else {
            if (c.playbackState == Player.STATE_IDLE || c.playbackState == Player.STATE_ENDED) c.prepare()
            c.play()
        }
    }

    fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs)
        _state.update { it.copy(positionMs = positionMs) }
    }

    fun skipNext() {
        controller?.seekToNextMediaItem()
    }

    fun skipPrev() {
        controller?.seekToPreviousMediaItem()
    }

    fun setRepeatMode(mode: Int) {
        controller?.repeatMode = mode
        _state.update { it.copy(repeatMode = mode) }
    }
    fun setShuffle(enabled: Boolean) {
        controller?.shuffleModeEnabled = enabled
        _state.update { it.copy(shuffle = enabled) }
    }

    /** 清空播放队列并停止播放 —— 对应桌面版 clearQueue()。
     *  显式重置全部播放字段，避免依赖 MediaController 异步回调造成状态残留。 */
    fun clearQueue() {
        val c = controller ?: return
        c.clearMediaItems()
        c.stop()
        _state.update {
            it.copy(
                queue = emptyList(),
                queueIndex = -1,
                current = null,
                isPlaying = false,
                positionMs = 0L,
                durationMs = 0L,
                error = null,
            )
        }
    }

    /** 启用随机播放 —— 对应桌面版 shuffleQueue()。
     *  移动端使用 ExoPlayer 内置 shuffle 模式重排播放顺序（不改动 UI 队列展示顺序）。
     *  与 cyclePlayMode 状态机保持一致：开启随机时把单曲循环退回列表循环，避免语义冲突。 */
    fun shuffleQueue() {
        val c = controller ?: return
        c.shuffleModeEnabled = true
        if (c.repeatMode == Player.REPEAT_MODE_ONE) {
            c.repeatMode = Player.REPEAT_MODE_ALL
        }
        _state.update {
            it.copy(
                shuffle = true,
                repeatMode = if (it.repeatMode == Player.REPEAT_MODE_ONE) Player.REPEAT_MODE_ALL else it.repeatMode,
            )
        }
    }

    /** 设置音量 —— 对应桌面版 setVolume(volume)。
     *  volume 范围 0..1，映射到 STREAM_MUSIC 的最大音量档位。 */
    fun setVolume(volume: Float) {
        val max = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        val target = (volume.coerceIn(0f, 1f) * max).toInt().coerceIn(0, max)
        audioManager.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, target, 0)
        _state.update { it.copy(volume = target.toFloat() / max, muted = target <= 0) }
    }

    /** 静音切换 —— 对应桌面版 toggleMute()。 */
    fun toggleMute() {
        if (_state.value.muted) {
            // 取消静音：恢复到中等音量（避免 0 后无法拖回）
            setVolume(0.5f)
        } else {
            audioManager.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, 0, 0)
            _state.update { it.copy(volume = 0f, muted = true) }
        }
    }

    private fun currentStreamVolumeFraction(): Float {
        val max = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        val cur = audioManager.getStreamVolume(android.media.AudioManager.STREAM_MUSIC).coerceIn(0, max)
        return cur.toFloat() / max
    }

    /**
     * 把裁剪后的方形封面应用到当前曲目的 MediaSession artwork。
     *
     * 关键：必须用 FileProvider 生成 content:// URI，而非 Uri.fromFile(file)（file://）。
     * SystemUI（状态栏媒体控件 / vivo 流体云 / 锁屏）是独立进程，无权读取应用私有缓存目录的 file:// URI，
     * 只能通过 FileProvider 授予的 content:// URI 访问。这是流体云显示封面的阻塞前提。
     */
    fun setArtworkBitmap(bitmap: android.graphics.Bitmap) {
        val c = controller ?: return
        val current = c.currentMediaItem ?: return
        runCatching {
            val coverDir = java.io.File(context.cacheDir, "artwork").apply { mkdirs() }
            val file = java.io.File(coverDir, "cropped_cover_${System.currentTimeMillis()}.jpg")
            java.io.FileOutputStream(file).use { out ->
                bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 92, out)
            }
            // 用 FileProvider 暴露为 content:// URI，授权 SystemUI 读取
            val authority = "${context.packageName}.fileprovider"
            val uri = androidx.core.content.FileProvider.getUriForFile(context, authority, file)
            // 授予所有可能读取封面的外部进程（SystemUI / vivo 流体云 / 锁屏）读权限
            context.grantUriPermission(
                "com.android.systemui",
                uri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
            val newMeta = current.mediaMetadata.buildUpon()
                .setArtworkUri(uri)
                .build()
            val newItem = current.buildUpon().setMediaMetadata(newMeta).build()
            val idx = c.currentMediaItemIndex
            c.replaceMediaItem(idx, newItem)
        }
    }

    /** 清除自定义封面，恢复当前曲目的原始 artwork —— 对应桌面版 clearCustomCoverForCurrent()。 */
    fun clearArtwork() {
        val c = controller ?: return
        val current = c.currentMediaItem ?: return
        val song = _state.value.current ?: return
        runCatching {
            val newMeta = current.mediaMetadata.buildUpon()
                .setArtworkUri(
                    if (song.displayCover.isNotEmpty()) android.net.Uri.parse(song.displayCover) else null,
                )
                .build()
            val newItem = current.buildUpon().setMediaMetadata(newMeta).build()
            val idx = c.currentMediaItemIndex
            c.replaceMediaItem(idx, newItem)
        }
    }

    private fun Song.toMediaItem(playUrl: String): MediaItem {
        // 流体云 / SystemUI 媒体控件依赖完整 MediaMetadata：
        //  - mediaType=MUSIC 让 vivo OriginOS 把卡片归到「音乐」实时活动类
        //  - durationMs 让进度条可显示（否则流体云只显示标题无进度）
        //  - isBrowsable=false / isPlayable=true 表明这是可播但不可浏览的叶子节点
        val meta = MediaMetadata.Builder()
            .setTitle(name)
            .setArtist(displayArtist)
            .setAlbumTitle(displayAlbum)
            .setArtworkUri(if (displayCover.isNotEmpty()) android.net.Uri.parse(displayCover) else null)
            .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
            .setIsBrowsable(false)
            .setIsPlayable(true)
            .apply {
                // duration 单位是秒（网易云/QQ API），MediaMetadata 期望毫秒
                if (duration > 0) setDurationMs(duration * 1000L)
            }
            .build()
        return MediaItem.Builder()
            .setMediaId("${source}:${id}")
            .setUri(playUrl)
            .setMediaMetadata(meta)
            .build()
    }
}
