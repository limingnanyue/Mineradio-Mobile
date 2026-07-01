package com.mineradio.player

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.mineradio.player.data.api.ApiFactory
import com.mineradio.player.data.api.BackendConfig
import com.mineradio.player.data.playback.PlayerController
import com.mineradio.player.data.repo.MineradioRepository

/**
 * Application —— 手动 DI 容器（避免引入 Hilt 增加复杂度）。
 * 全局单例：BackendConfig / ApiFactory / Repository / PlayerController。
 */
class MineradioApp : Application() {

    lateinit var backendConfig: BackendConfig
        private set
    lateinit var apiFactory: ApiFactory
        private set
    lateinit var repository: MineradioRepository
        private set
    lateinit var playerController: PlayerController
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        backendConfig = BackendConfig(this)
        apiFactory = ApiFactory(this)
        repository = MineradioRepository(backendConfig) {
            apiFactory.api(backendConfig.baseUrl())
        }
        playerController = PlayerController(this).also { it.connect() }
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            val ch = NotificationChannel(
                CHANNEL_PLAYBACK,
                getString(R.string.playback_channel_name),
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = getString(R.string.playback_channel_desc)
                setShowBadge(false)
            }
            nm.createNotificationChannel(ch)
        }
    }

    companion object {
        const val CHANNEL_PLAYBACK = "mineradio_playback"
        @Volatile private var instance: MineradioApp? = null
        fun get(): MineradioApp = instance!!
    }
}
