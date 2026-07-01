package com.mineradio.player.data.api

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.mineradio.player.BuildConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "mineradio_backend")

/**
 * 后端地址配置 —— DataStore 持久化用户在「设置」面板填写的自建后端 URL。
 * 默认取 BuildConfig.DEFAULT_BACKEND_URL（构建时可注入）。
 *
 * 后端 server.js 的 /api/audio?url=... 与 /api/cover?url=... 是流式代理，
 * 由 [audioProxyUrl] / [coverProxyUrl] 拼接后给 ExoPlayer / Coil 直接消费。
 */
class BackendConfig(private val context: Context) {

    private val key = stringPreferencesKey("backend_url")

    val backendUrlFlow: Flow<String> = context.dataStore.data.map { p ->
        p[key]?.takeIf { it.isNotBlank() } ?: BuildConfig.DEFAULT_BACKEND_URL
    }

    suspend fun current(): String = backendUrlFlow.first()

    suspend fun set(url: String) {
        val normalized = normalize(url)
        context.dataStore.edit { p ->
            if (normalized.isEmpty()) p.remove(key) else p[key] = normalized
        }
    }

    companion object {
        fun normalize(url: String): String {
            var u = url.trim().trimEnd('/')
            if (u.isEmpty()) return ""
            if (!u.startsWith("http://") && !u.startsWith("https://")) u = "https://$u"
            return u
        }
    }

    /** 当前生效的 baseUrl（带末尾斜杠，供 Retrofit 用）。 */
    suspend fun baseUrl(): String {
        val u = current()
        return if (u.isEmpty()) "" else "$u/"
    }

    /** 拼接音频流代理 URL：{backend}/api/audio?url={原始音频URL} */
    fun audioProxyUrl(rawAudioUrl: String, backendBase: String): String {
        if (rawAudioUrl.isEmpty()) return ""
        if (backendBase.isEmpty()) return rawAudioUrl
        // 已经是后端绝对地址 —— 不再套代理
        if (rawAudioUrl.startsWith(backendBase, ignoreCase = true)) return rawAudioUrl
        val base = backendBase.trimEnd('/')
        return "$base/api/audio?url=" + java.net.URLEncoder.encode(rawAudioUrl, "UTF-8")
    }

    /** 拼接封面代理 URL：{backend}/api/cover?url={原始封面URL} */
    fun coverProxyUrl(rawCoverUrl: String, backendBase: String): String {
        if (rawCoverUrl.isEmpty()) return ""
        if (backendBase.isEmpty()) return rawCoverUrl
        if (rawCoverUrl.startsWith(backendBase, ignoreCase = true)) return rawCoverUrl
        val base = backendBase.trimEnd('/')
        return "$base/api/cover?url=" + java.net.URLEncoder.encode(rawCoverUrl, "UTF-8")
    }
}

/**
 * API 工厂 —— 后端地址变化时重建 Retrofit 实例。
 */
class ApiFactory(private val context: Context) {
    private var currentBase: String = ""
    private var apiRef: MineradioApi? = null

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC
                else HttpLoggingInterceptor.Level.NONE
            })
            .build()
    }

    @Synchronized
    fun api(backendBase: String): MineradioApi {
        if (apiRef != null && currentBase == backendBase) return apiRef!!
        val retrofit = Retrofit.Builder()
            .baseUrl(if (backendBase.isEmpty()) "http://127.0.0.1:3000/" else backendBase)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val api = retrofit.create(MineradioApi::class.java)
        apiRef = api
        currentBase = backendBase
        return api
    }
}
