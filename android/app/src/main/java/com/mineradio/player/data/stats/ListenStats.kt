package com.mineradio.player.data.stats

import android.content.Context
import com.mineradio.player.data.api.dto.Song
import org.json.JSONArray
import org.json.JSONObject

/**
 * 听歌画像统计 —— 复刻桌面版 listenStatsState / beginListenSession / finalizeListenSession
 * （index.html:15323-15463）。
 *
 * 数据模型：
 *  - history: 最近播放记录（最多 180 条，去重保留最新）
 *  - songs: 每首歌的累计统计 { key, name, artist, cover, source, plays, listenMs, completed, lastPlayedAt }
 *  - artists: 每位歌手的累计统计 { name, plays, listenMs, lastPlayedAt }
 *  - updatedAt: 最后更新时间戳
 *
 * 会话机制：
 *  - beginListenSession(song)：切歌时开启新会话（同 key 不重开）
 *  - tick(positionMs)：播放中按进度增量累加 listenMs（限制单次增量 ≤8s，防跳进）
 *  - finalizeListenSession(completed)：结束时判定是否「有效播放」
 *      （completed || listenMs≥45s || maxProgress≥0.5 || 无时长时 listenMs≥30s），
 *      有效则写入 history / songs / artists 并持久化。
 *
 * 持久化：SharedPreferences key="mineradio-listen-stats-v1"，JSON 序列化（与桌面版 localStorage 同名）。
 */
data class ListenStatsState(
    val history: List<ListenRecord> = emptyList(),
    val songs: Map<String, SongStat> = emptyMap(),
    val artists: Map<String, ArtistStat> = emptyMap(),
    val updatedAt: Long = 0L,
)

/** 单条播放历史记录。 */
data class ListenRecord(
    val key: String,
    val id: Long,
    val name: String,
    val artist: String,
    val cover: String,
    val source: String,
    val playedAt: Long,
    val listenMs: Long,
    val completed: Boolean,
)

/** 单首歌的累计统计。 */
data class SongStat(
    val key: String,
    val name: String,
    val artist: String,
    val cover: String,
    val source: String,
    val plays: Int,
    val listenMs: Long,
    val completed: Int,
    val lastPlayedAt: Long,
)

/** 单位歌手的累计统计。 */
data class ArtistStat(
    val name: String,
    val plays: Int,
    val listenMs: Long,
    val lastPlayedAt: Long,
)

/** 听歌画像汇总 —— 用于首页「听歌画像」卡片与 tile 数据回填。 */
data class ListenSummary(
    val recent: ListenRecord? = null,
    val topSong: SongStat? = null,
    val topArtist: ArtistStat? = null,
    val totalPlays: Int = 0,
    val totalListenMs: Long = 0L,
) {
    /** 总收听分钟数（向下取整）。 */
    val totalMinutes: Long get() = totalListenMs / 60000L
}

/** 进行中的播放会话（mutable，仅 Tracker 内部使用）。 */
private data class ListenSession(
    val key: String,
    val song: Song,
    val startedAt: Long,
    var lastWallAt: Long,
    var lastAudioTimeMs: Long,
    var listenMs: Long,
    var maxProgress: Float,
)

/**
 * 听歌画像追踪器 —— 单例，由 MainViewModel 持有。
 *
 * 使用方式：
 *   player.connect() 后，把 onSongChanged / onPlayingTick / onSongEnded 钩子接到对应位置即可。
 */
class ListenStatsTracker(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private var state: ListenStatsState = loadFromPrefs()
    private var session: ListenSession? = null

    /** 开启新会话；若同 key 则不重开（与桌面版一致）。 */
    fun begin(song: Song?) {
        if (song == null) return
        val key = queueItemKey(song)
        if (key.isEmpty()) return
        val cur = session
        if (cur != null && cur.key != key) finalize(completed = false)
        if (session == null || session?.key != key) {
            val now = System.currentTimeMillis()
            session = ListenSession(
                key = key,
                song = song,
                startedAt = now,
                lastWallAt = now,
                lastAudioTimeMs = 0L,
                listenMs = 0L,
                maxProgress = 0f,
            )
        }
    }

    /**
     * 进度推进时调用 —— 按 wall clock + audio position 增量累加 listenMs。
     * @param positionMs 当前播放位置（毫秒）
     * @param durationMs 总时长（毫秒，用于 maxProgress 判定）
     * @param isPlaying 是否正在播放（暂停时不累加）
     */
    fun tick(positionMs: Long, durationMs: Long, isPlaying: Boolean) {
        if (!isPlaying) return
        val s = session ?: return
        val now = System.currentTimeMillis()
        val deltaByAudio = (positionMs - s.lastAudioTimeMs).coerceAtLeast(0L)
        val deltaByWall = (now - s.lastWallAt).coerceAtLeast(0L)
        // 取音频增量与墙钟增量的较小值，限制单次 ≤4.2s（防 seek 后虚假累积）
        val delta = if (deltaByAudio > 0) minOf(deltaByAudio, deltaByWall.ifZero { deltaByAudio }, 4200L) else 0L
        if (delta in 1L until 8000L) s.listenMs += delta
        s.lastWallAt = now
        s.lastAudioTimeMs = positionMs
        if (durationMs > 0) s.maxProgress = maxOf(s.maxProgress, positionMs.toFloat() / durationMs.toFloat())
    }

    /**
     * 结束当前会话并判定是否写入统计。
     * @param completed 是否自然播完（STATE_ENDED）
     */
    fun finalize(completed: Boolean) {
        val s = session ?: return
        // 结束前再 tick 一次（force 兜底，与桌面版 updateListenStatsTick(true) 一致）
        tick(s.lastAudioTimeMs, 0L, isPlaying = true)
        session = null
        val effective = completed ||
            s.listenMs >= 45_000L ||
            s.maxProgress >= 0.5f ||
            s.listenMs >= 30_000L
        if (!effective) return
        val now = System.currentTimeMillis()
        val song = s.song
        val record = ListenRecord(
            key = s.key,
            id = song.id,
            name = song.name,
            artist = song.displayArtist,
            cover = song.displayCover,
            source = song.source,
            playedAt = now,
            listenMs = s.listenMs,
            completed = completed,
        )
        // history 去重置顶，最多 180 条
        val newHistory = (listOf(record) + state.history.filter { it.key != record.key }).take(180)
        // songs 累加
        val prevSong = state.songs[record.key]
        val newSong = SongStat(
            key = record.key,
            name = record.name,
            artist = record.artist,
            cover = record.cover,
            source = record.source,
            plays = (prevSong?.plays ?: 0) + 1,
            listenMs = (prevSong?.listenMs ?: 0) + record.listenMs,
            completed = (prevSong?.completed ?: 0) + if (completed) 1 else 0,
            lastPlayedAt = now,
        )
        val newSongs = state.songs + (record.key to newSong)
        // artists 累加（按 / , 、 & 拆分歌手名）
        val newArtists = state.artists.toMutableMap()
        splitArtists(record.artist).forEach { name ->
            val prev = newArtists[name]
            newArtists[name] = ArtistStat(
                name = name,
                plays = (prev?.plays ?: 0) + 1,
                listenMs = (prev?.listenMs ?: 0) + record.listenMs,
                lastPlayedAt = now,
            )
        }
        state = ListenStatsState(newHistory, newSongs, newArtists, now)
        saveToPrefs()
    }

    /** 当前汇总（供首页卡片读取）。 */
    fun summary(): ListenSummary {
        val topSong = state.songs.values
            .sortedWith(compareByDescending<SongStat> { it.plays }.thenByDescending { it.listenMs }.thenByDescending { it.lastPlayedAt })
            .firstOrNull()
        val topArtist = state.artists.values
            .sortedWith(compareByDescending<ArtistStat> { it.plays }.thenByDescending { it.listenMs }.thenByDescending { it.lastPlayedAt })
            .firstOrNull()
        val totalPlays = state.songs.values.sumOf { it.plays }
        val totalMs = state.songs.values.sumOf { it.listenMs }
        return ListenSummary(
            recent = state.history.firstOrNull(),
            topSong = topSong,
            topArtist = topArtist,
            totalPlays = totalPlays,
            totalListenMs = totalMs,
        )
    }

    /** 最近播放 N 首（用于 home rail tile）。 */
    fun recentSongs(limit: Int = 5): List<ListenRecord> = state.history.take(limit)

    // ---- 持久化 ----

    private fun loadFromPrefs(): ListenStatsState {
        val raw = prefs.getString(KEY_STATE, null) ?: return ListenStatsState()
        return runCatching {
            val o = JSONObject(raw)
            val history = mutableListOf<ListenRecord>()
            o.optJSONArray("history")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val h = arr.optJSONObject(i) ?: continue
                    history.add(
                        ListenRecord(
                            key = h.optString("key"),
                            id = h.optLong("id"),
                            name = h.optString("name"),
                            artist = h.optString("artist"),
                            cover = h.optString("cover"),
                            source = h.optString("source"),
                            playedAt = h.optLong("playedAt"),
                            listenMs = h.optLong("listenMs"),
                            completed = h.optBoolean("completed"),
                        ),
                    )
                }
            }
            val songs = mutableMapOf<String, SongStat>()
            o.optJSONObject("songs")?.let { obj ->
                obj.keys().forEach { k ->
                    val s = obj.optJSONObject(k) ?: return@forEach
                    songs[k] = SongStat(
                        key = s.optString("key"),
                        name = s.optString("name"),
                        artist = s.optString("artist"),
                        cover = s.optString("cover"),
                        source = s.optString("source"),
                        plays = s.optInt("plays"),
                        listenMs = s.optLong("listenMs"),
                        completed = s.optInt("completed"),
                        lastPlayedAt = s.optLong("lastPlayedAt"),
                    )
                }
            }
            val artists = mutableMapOf<String, ArtistStat>()
            o.optJSONObject("artists")?.let { obj ->
                obj.keys().forEach { k ->
                    val a = obj.optJSONObject(k) ?: return@forEach
                    artists[k] = ArtistStat(
                        name = a.optString("name"),
                        plays = a.optInt("plays"),
                        listenMs = a.optLong("listenMs"),
                        lastPlayedAt = a.optLong("lastPlayedAt"),
                    )
                }
            }
            ListenStatsState(history, songs, artists, o.optLong("updatedAt"))
        }.getOrDefault(ListenStatsState())
    }

    private fun saveToPrefs() {
        val o = JSONObject()
        o.put("history", JSONArray().apply { state.history.forEach { put(recordToJson(it)) } })
        o.put("songs", JSONObject().apply { state.songs.forEach { (k, v) -> put(k, songStatToJson(v)) } })
        o.put("artists", JSONObject().apply { state.artists.forEach { (k, v) -> put(k, artistStatToJson(v)) } })
        o.put("updatedAt", state.updatedAt)
        prefs.edit().putString(KEY_STATE, o.toString()).apply()
    }

    private fun recordToJson(r: ListenRecord) = JSONObject().apply {
        put("key", r.key); put("id", r.id); put("name", r.name); put("artist", r.artist)
        put("cover", r.cover); put("source", r.source); put("playedAt", r.playedAt)
        put("listenMs", r.listenMs); put("completed", r.completed)
    }

    private fun songStatToJson(s: SongStat) = JSONObject().apply {
        put("key", s.key); put("name", s.name); put("artist", s.artist); put("cover", s.cover)
        put("source", s.source); put("plays", s.plays); put("listenMs", s.listenMs)
        put("completed", s.completed); put("lastPlayedAt", s.lastPlayedAt)
    }

    private fun artistStatToJson(a: ArtistStat) = JSONObject().apply {
        put("name", a.name); put("plays", a.plays); put("listenMs", a.listenMs); put("lastPlayedAt", a.lastPlayedAt)
    }

    companion object {
        private const val PREFS_NAME = "mineradio_stats"
        private const val KEY_STATE = "mineradio-listen-stats-v1"

        /** 生成歌曲唯一 key（与桌面版 queueItemKey 一致：source:id）。 */
        fun queueItemKey(song: Song): String = "${song.source}:${song.id}"
    }
}

/** 按 / , 、 & 拆分歌手名（与桌面版正则 /\s*\/\s*|\s*,\s*|、|&/ 一致）。 */
private fun splitArtists(raw: String): List<String> =
    raw.split(Regex("\\s*/\\s*|\\s*,\\s*|、|&")).map { it.trim() }.filter { it.isNotEmpty() }

private fun Long.ifZero(fallback: () -> Long): Long = if (this == 0L) fallback() else this
