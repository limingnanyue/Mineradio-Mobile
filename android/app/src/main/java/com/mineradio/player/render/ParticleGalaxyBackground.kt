package com.mineradio.player.render

import android.content.Context
import android.opengl.GLSurfaceView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.min
import kotlin.math.max

/**
 * Compose 中的 OpenGL ES 粒子星河场 —— 全屏背景层。
 *
 * 用 AndroidView 持有 GLSurfaceView，渲染循环由 GLSurfaceView 自身维持；
 * 播放状态/封面通过 [GalaxyState] 推送到渲染器。
 *
 * 视觉与桌面版 wallpaper.html 1:1：粒子星河 + 中心光晕 + 加性混合 + 暗场渐变。
 */
class GalaxyState {
    internal val renderer = ParticleGalaxyRenderer()
    internal var view: GLSurfaceView? = null

    fun setState(state: ParticleGalaxyRenderer.State) {
        renderer.setState(state)
    }
}

@Composable
fun ParticleGalaxyBackground(
    modifier: Modifier = Modifier,
    state: GalaxyState,
    playing: Boolean,
    coverUrl: String?,
    opacity: Float = 1f,
    preset: Int = 0,
    title: String = "Mineradio",
    artist: String = "",
    primaryColor: Color = Color(0xFFD6F8FF),
    secondaryColor: Color = Color(0xFF9CFFDF),
    highlightColor: Color = Color(0xFFFFF0B8),
    glowColor: Color = Color(0xFF9CFFDF),
    // 粒子高级参数（对应桌面版 fx.point/speed/twist/color/bloom/scatter/bgfade）
    particleSize: Float = 1.0f,
    particleSpeed: Float = 1.0f,
    particleTwist: Float = 1.0f,
    particleColor: Float = 1.0f,
    particleBloom: Float = 1.0f,
    particleScatter: Float = 1.0f,
    particleBgFade: Float = 1.0f,
) {
    val context = LocalContext.current
    val glView = remember { GalaxyGLSurfaceView(context, state.renderer) }

    DisposableEffect(glView) {
        onDispose {
            glView.onPause()
            state.view = null
        }
    }

    // Compose Color -> renderer 的 FloatArray（归一化 RGB）
    fun Color.toFloatRgb(): FloatArray = floatArrayOf(
        red, green, blue,
    )

    LaunchedEffect(playing, opacity, coverUrl, preset, title, artist, primaryColor, secondaryColor, highlightColor, glowColor,
        particleSize, particleSpeed, particleTwist, particleColor, particleBloom, particleScatter, particleBgFade) {
        state.renderer.setState(
            ParticleGalaxyRenderer.State(
                playing = playing,
                opacity = opacity,
                cover = coverUrl ?: "",
                preset = preset,
                title = title,
                artist = artist,
                colors = ParticleGalaxyRenderer.State.Colors(
                    primary = primaryColor.toFloatRgb(),
                    secondary = secondaryColor.toFloatRgb(),
                    highlight = highlightColor.toFloatRgb(),
                    glow = glowColor.toFloatRgb(),
                ),
                particle = ParticleGalaxyRenderer.State.ParticleParams(
                    size = particleSize,
                    speed = particleSpeed,
                    twist = particleTwist,
                    color = particleColor,
                    bloom = particleBloom,
                    scatter = particleScatter,
                    bgFade = particleBgFade,
                ),
            )
        )
    }

    AndroidView(
        factory = {
            state.view = glView
            glView.onResume()
            glView
        },
        modifier = modifier,
    )
}

/**
 * GLSurfaceView 子类 —— 设置 GLES2 上下文 + 持续渲染 + DPR 透传。
 */
private class GalaxyGLSurfaceView(
    context: Context,
    private val renderer: ParticleGalaxyRenderer,
) : GLSurfaceView(context) {

    init {
        setEGLContextClientVersion(2)
        // 8888 + 深度16（粒子不需要深度，但部分设备要求配置完整）
        setEGLConfigChooser(8, 8, 8, 8, 0, 0)
        setRenderer(object : Renderer {
            override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
                renderer.onSurfaceCreated()
            }

            override fun onSurfaceChanged(gl: GL10?, w: Int, h: Int) {
                val dpr = min(1.35f, max(1f, resources.displayMetrics.density))
                renderer.onSurfaceChanged(w, h, dpr)
            }

            override fun onDrawFrame(gl: GL10?) {
                val t = (System.currentTimeMillis() % 1000000L) / 1000f
                renderer.onDrawFrame(t)
            }
        })
        renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
        preserveEGLContextOnPause = true
    }
}
