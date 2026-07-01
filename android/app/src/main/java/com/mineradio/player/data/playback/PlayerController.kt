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
    )

    private val _state = MutableStateFlow(PlaybackState())
    val state: StateFlow<PlaybackState> = _state.asStateFlow()

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var progressJob: Job? = null

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
        _state.update { it.copy(repeatMode = c.repeatMode, shuffle = c.shuffleModeEnabled) }
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

    /** 清空播放队列并停止播放 —— 对应桌面版 clearQueue()。 */
    fun clearQueue() {
        val c = controller ?: return
        c.clearMediaItems()
        c.stop()
        _state.update { it.copy(queue = emptyList(), queueIndex = -1) }
    }

    /** 随机打乱当前队列顺序 —— 对应桌面版 shuffleQueue()。
     *  移动端使用 ExoPlayer 内置 shuffle 模式（等价于重排播放顺序）。 */
    fun shuffleQueue() {
        val c = controller ?: return
        c.shuffleModeEnabled = true
        _state.update { it.copy(shuffle = true) }
    }

    /**
     * 把裁剪后的方形封面应用到当前曲目的 MediaSession artwork。
     * 写入缓存文件后用 file:// URI 替换当前 MediaItem 的 artworkUri。
     */
    fun setArtworkBitmap(bitmap: android.graphics.Bitmap) {
        val c = controller ?: return
        val current = c.currentMediaItem ?: return
        runCatching {
            val file = java.io.File(context.cacheDir, "cropped_cover_${System.currentTimeMillis()}.jpg")
            java.io.FileOutputStream(file).use { out ->
                bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 92, out)
            }
            val uri = android.net.Uri.fromFile(file)
            val newMeta = current.mediaMetadata.buildUpon()
                .setArtworkUri(uri)
                .build()
            val newItem = current.buildUpon().setMediaMetadata(newMeta).build()
            val idx = c.currentMediaItemIndex
            c.replaceMediaItem(idx, newItem)
        }
    }

    private fun Song.toMediaItem(playUrl: String): MediaItem {
        val meta = MediaMetadata.Builder()
            .setTitle(name)
            .setArtist(displayArtist)
            .setAlbumTitle(displayAlbum)
            .setArtworkUri(if (displayCover.isNotEmpty()) android.net.Uri.parse(displayCover) else null)
            .build()
        return MediaItem.Builder()
            .setMediaId("${source}:${id}")
            .setUri(playUrl)
            .setMediaMetadata(meta)
            .build()
    }
}
