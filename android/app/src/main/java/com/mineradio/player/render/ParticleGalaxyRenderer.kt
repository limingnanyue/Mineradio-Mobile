package com.mineradio.player.render

import android.graphics.Bitmap
import android.opengl.GLES20
import android.opengl.GLUtils
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.abs
import kotlin.math.sin

/**
 * 粒子星河场渲染器 —— 忠实移植 public/wallpaper.html 的 Canvas2D 粒子系统到 OpenGL ES。
 *
 * 移植要点（与桌面版 1:1 对齐）：
 *  - 粒子数量：min(760, max(420, round(w*h/4200)))，移动端横屏通常 ~520 颗
 *  - 每颗粒子属性 {seed, x, lane, z, size} 由 rand() 伪随机生成（与桌面版 rand(i) 同公式）
 *  - 椭圆轨道中心 (cx=0.5w, cy=0.5h + sin(now*0.28)*0.018h)
 *  - 半径 rx=0.40w, ry=0.30h
 *  - 角度/速度/闪烁/颜色分级全部在 GLSL 顶点着色器中重算（见 Shaders.kt）
 *  - 背景线性渐变 + 中心径向光晕（aura）
 *  - 加性混合（GL_LIGHTEN / 来源 alpha）
 *
 * 状态由 setState() 推送：title/artist/cover/playing/preset/opacity/colors。
 */
class ParticleGalaxyRenderer {

    // ---- 程序 ----
    private var bgProgram = 0
    private var particleProgram = 0

    // ---- 背景属性/uniform ----
    private var bgPosLoc = -1
    private var bgResLoc = -1
    private var bgPrimLoc = -1
    private var bgSecLoc = -1
    private var bgHlLoc = -1
    private var bgOpLoc = -1
    private var bgTimeLoc = -1

    // ---- 粒子属性/uniform ----
    private var pPosLoc = -1
    private var pSeedLoc = -1
    private var pLaneLoc = -1
    private var pZLoc = -1
    private var pSizeLoc = -1
    private var pTimeLoc = -1
    private var pPlayingLoc = -1
    private var pOpLoc = -1
    private var pResLoc = -1
    private var pDprLoc = -1
    private var pColPrimLoc = -1
    private var pColSecLoc = -1
    private var pColHlLoc = -1
    private var pColGlowLoc = -1
    // 粒子高级参数 uniform
    private var pSizeScaleLoc = -1
    private var pSpeedScaleLoc = -1
    private var pTwistScaleLoc = -1
    private var pColorScaleLoc = -1
    private var pBloomScaleLoc = -1
    private var pScatterScaleLoc = -1

    // ---- 粒子数据 ----
    private var particleCount = 0
    private var posBuffer: FloatBuffer? = null
    private var seedBuffer: FloatBuffer? = null
    private var laneBuffer: FloatBuffer? = null
    private var zBuffer: FloatBuffer? = null
    private var sizeBuffer: FloatBuffer? = null
    private var fullQuadBuffer: FloatBuffer? = null

    // ---- 视口 ----
    private var width = 1
    private var height = 1
    private var dpr = 1f

    // ---- 状态（对应 wallpaper.html state 对象）----
    data class State(
        val enabled: Boolean = true,
        val title: String = "Mineradio",
        val artist: String = "",
        val cover: String = "",
        val playing: Boolean = false,
        val preset: Int = 0,
        val opacity: Float = 1f,
        val colors: Colors = Colors(),
        // 粒子高级参数（对应桌面版 fx.point/speed/twist/color/bloom/scatter/bgfade）
        val particle: ParticleParams = ParticleParams(),
    ) {
        data class Colors(
            val primary: FloatArray = hexToRgb("#d6f8ff"),
            val secondary: FloatArray = hexToRgb("#9cffdf"),
            val highlight: FloatArray = hexToRgb("#fff0b8"),
            val glow: FloatArray = hexToRgb("#9cffdf"),
        )

        /** 粒子高级参数 —— 全部默认 1.0 即与原始 wallpaper 视觉一致。 */
        data class ParticleParams(
            val size: Float = 1.0f,        // fx.point  0.4..2.4
            val speed: Float = 1.0f,       // fx.speed  0.2..2.6
            val twist: Float = 1.0f,       // fx.twist  0.0..2.4
            val color: Float = 1.0f,       // fx.color  0.0..1.8  色彩张力（高光阈值调节）
            val bloom: Float = 1.0f,       // fx.bloom  0.0..2.2  溢光（高光强度）
            val scatter: Float = 1.0f,     // fx.scatter 0.0..1.8 离散（半径环离散度）
            val bgFade: Float = 1.0f,      // fx.bgfade 0.0..1.6  背景 aura 强度
        )
    }

    @Volatile
    private var state: State = State()

    // ---- 封面纹理 ----
    private var coverTexture = 0
    private var coverLoaded = false
    private var coverSrc: String = ""
    private var pendingCoverBitmap: Bitmap? = null

    companion object {
        /** 与 wallpaper.html rand() 完全一致的伪随机 */
        fun rand(n: Float): Float = abs(sin(n * 3187.917f) * 43758.5453f) % 1f

        /** 与 wallpaper.html hexToRgb 一致（支持 #rgb / #rrggbb，失败回退） */
        fun hexToRgb(hex: String?, fallback: String = "#9cffdf"): FloatArray {
            var h = (hex ?: "").trim()
            if (h.isEmpty()) h = fallback
            if (Regex("^#[0-9a-fA-F]{3}$").matches(h)) {
                h = "#" + h[1] + h[1] + h[2] + h[2] + h[3] + h[3]
            }
            if (!Regex("^#[0-9a-fA-F]{6}$").matches(h)) h = fallback
            return floatArrayOf(
                Integer.parseInt(h.substring(1, 3), 16) / 255f,
                Integer.parseInt(h.substring(3, 5), 16) / 255f,
                Integer.parseInt(h.substring(5, 7), 16) / 255f,
            )
        }
    }

    fun setState(next: State) {
        state = next
    }

    fun setCoverBitmap(bitmap: Bitmap?) {
        pendingCoverBitmap = bitmap
    }

    fun onSurfaceCreated() {
        bgProgram = GlUtils.linkProgram(Shaders.BG_VERTEX, Shaders.BG_FRAGMENT)
        bgPosLoc = GlUtils.attribLocation(bgProgram, "aPosition")
        bgResLoc = GlUtils.uniformLocation(bgProgram, "uResolution")
        bgPrimLoc = GlUtils.uniformLocation(bgProgram, "uColorPrimary")
        bgSecLoc = GlUtils.uniformLocation(bgProgram, "uColorSecondary")
        bgHlLoc = GlUtils.uniformLocation(bgProgram, "uColorHighlight")
        bgOpLoc = GlUtils.uniformLocation(bgProgram, "uOpacity")
        bgTimeLoc = GlUtils.uniformLocation(bgProgram, "uTime")

        particleProgram = GlUtils.linkProgram(Shaders.PARTICLE_VERTEX, Shaders.PARTICLE_FRAGMENT)
        pPosLoc = GlUtils.attribLocation(particleProgram, "aPosition")
        pSeedLoc = GlUtils.attribLocation(particleProgram, "aSeed")
        pLaneLoc = GlUtils.attribLocation(particleProgram, "aLane")
        pZLoc = GlUtils.attribLocation(particleProgram, "aZ")
        pSizeLoc = GlUtils.attribLocation(particleProgram, "aSize")
        pTimeLoc = GlUtils.uniformLocation(particleProgram, "uTime")
        pPlayingLoc = GlUtils.uniformLocation(particleProgram, "uPlaying")
        pOpLoc = GlUtils.uniformLocation(particleProgram, "uOpacity")
        pResLoc = GlUtils.uniformLocation(particleProgram, "uResolution")
        pDprLoc = GlUtils.uniformLocation(particleProgram, "uDpr")
        pColPrimLoc = GLES20.glUniformLocation(particleProgram, "uColorPrimary")
        pColSecLoc = GLES20.glUniformLocation(particleProgram, "uColorSecondary")
        pColHlLoc = GLES20.glUniformLocation(particleProgram, "uColorHighlight")
        pColGlowLoc = GLES20.glUniformLocation(particleProgram, "uColorGlow")
        pSizeScaleLoc = GLES20.glUniformLocation(particleProgram, "uSizeScale")
        pSpeedScaleLoc = GLES20.glUniformLocation(particleProgram, "uSpeedScale")
        pTwistScaleLoc = GLES20.glUniformLocation(particleProgram, "uTwistScale")
        pColorScaleLoc = GLES20.glUniformLocation(particleProgram, "uColorScale")
        pBloomScaleLoc = GLES20.glUniformLocation(particleProgram, "uBloomScale")
        pScatterScaleLoc = GLES20.glUniformLocation(particleProgram, "uScatterScale")

        fullQuadBuffer = makeFloatBuffer(floatArrayOf(-1f, -1f, 1f, -1f, -1f, 1f, 1f, 1f))

        // 封面纹理占位
        val tex = IntArray(1)
        GLES20.glGenTextures(1, tex, 0)
        coverTexture = tex[0]
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, coverTexture)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)

        ensureParticles()
    }

    fun onSurfaceChanged(w: Int, h: Int, dpr: Float) {
        width = maxOf(1, w)
        height = maxOf(1, h)
        this.dpr = dpr
        GLES20.glViewport(0, 0, width, height)
        ensureParticles()
    }

    fun onDrawFrame(timeSeconds: Float) {
        val s = state
        val opacity = s.opacity.coerceIn(0.35f, 1f)
        val p = s.particle

        // 上传待加载封面
        pendingCoverBitmap?.let { bmp ->
            uploadCover(bmp)
            pendingCoverBitmap = null
        }

        GLES20.glClearColor(0.02f, 0.024f, 0.031f, 1f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        // ---- 1. 背景 ----
        GLES20.glUseProgram(bgProgram)
        GLES20.glUniform2f(bgResLoc, width.toFloat(), height.toFloat())
        GLES20.glUniform3fv(bgPrimLoc, 1, s.colors.primary, 0)
        GLES20.glUniform3fv(bgSecLoc, 1, s.colors.secondary, 0)
        GLES20.glUniform3fv(bgHlLoc, 1, s.colors.highlight, 0)
        GLES20.glUniform1f(bgOpLoc, opacity * p.bgFade)  // bgfade 调节背景 aura 强度
        GLES20.glUniform1f(bgTimeLoc, timeSeconds)
        fullQuadBuffer?.let {
            it.position(0)
            GLES20.glEnableVertexAttribArray(bgPosLoc)
            GLES20.glVertexAttribPointer(bgPosLoc, 2, GLES20.GL_FLOAT, false, 0, it)
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
            GLES20.glDisableVertexAttribArray(bgPosLoc)
        }

        // ---- 2. 粒子（加性混合）----
        GLES20.glUseProgram(particleProgram)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE)  // 加性混合 ≈ Canvas globalCompositeOperation='lighter'
        GLES20.glUniform1f(pTimeLoc, timeSeconds)
        GLES20.glUniform1f(pPlayingLoc, if (s.playing) 1f else 0f)
        GLES20.glUniform1f(pOpLoc, opacity)
        GLES20.glUniform2f(pResLoc, width.toFloat(), height.toFloat())
        GLES20.glUniform1f(pDprLoc, dpr)
        GLES20.glUniform3fv(pColPrimLoc, 1, s.colors.primary, 0)
        GLES20.glUniform3fv(pColSecLoc, 1, s.colors.secondary, 0)
        GLES20.glUniform3fv(pColHlLoc, 1, s.colors.highlight, 0)
        GLES20.glUniform3fv(pColGlowLoc, 1, s.colors.glow, 0)
        // 粒子高级参数
        GLES20.glUniform1f(pSizeScaleLoc, p.size)
        GLES20.glUniform1f(pSpeedScaleLoc, p.speed)
        GLES20.glUniform1f(pTwistScaleLoc, p.twist)
        GLES20.glUniform1f(pColorScaleLoc, p.color)
        GLES20.glUniform1f(pBloomScaleLoc, p.bloom)
        GLES20.glUniform1f(pScatterScaleLoc, p.scatter)

        bindAttribute(pPosLoc, posBuffer, 1)
        bindAttribute(pSeedLoc, seedBuffer, 1)
        bindAttribute(pLaneLoc, laneBuffer, 1)
        bindAttribute(pZLoc, zBuffer, 1)
        bindAttribute(pSizeLoc, sizeBuffer, 1)

        GLES20.glDrawArrays(GLES20.GL_POINTS, 0, particleCount)

        unbindAttribute(pPosLoc)
        unbindAttribute(pSeedLoc)
        unbindAttribute(pLaneLoc)
        unbindAttribute(pZLoc)
        unbindAttribute(pSizeLoc)
        GLES20.glDisable(GLES20.GL_BLEND)
        GlUtils.checkGlError("drawParticles")
    }

    private fun bindAttribute(loc: Int, buffer: FloatBuffer?, size: Int) {
        if (loc < 0 || buffer == null) return
        buffer.position(0)
        GLES20.glEnableVertexAttribArray(loc)
        GLES20.glVertexAttribPointer(loc, size, GLES20.GL_FLOAT, false, 0, buffer)
    }

    private fun unbindAttribute(loc: Int) {
        if (loc >= 0) GLES20.glDisableVertexAttribArray(loc)
    }

    /**
     * 重建粒子数据 —— 与 wallpaper.html ensureParticles() 完全一致的数量公式与属性初始化。
     */
    private fun ensureParticles() {
        val target = minOf(760, maxOf(420, (width * height / 4200)))
        if (target == particleCount && posBuffer != null) return
        particleCount = target

        val pos = FloatArray(target)
        val seed = FloatArray(target)
        val lane = FloatArray(target)
        val z = FloatArray(target)
        val size = FloatArray(target)
        for (i in 0 until target) {
            val f = (i + 1).toFloat()
            pos[i] = rand(f * 3.3f)          // aPosition.x ∈ [0,1] → 顶点着色器内 *2π
            seed[i] = f * 11.37f             // 与桌面版 seed: i*11.37
            lane[i] = rand(f * 2.7f)
            z[i] = rand(f * 8.1f)
            size[i] = 0.6f + rand(f * 4.2f) * 2.4f
        }
        posBuffer = makeFloatBuffer(pos)
        seedBuffer = makeFloatBuffer(seed)
        laneBuffer = makeFloatBuffer(lane)
        zBuffer = makeFloatBuffer(z)
        sizeBuffer = makeFloatBuffer(size)
    }

    private fun uploadCover(bmp: Bitmap) {
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, coverTexture)
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, bmp, 0)
        coverLoaded = true
    }

    private fun makeFloatBuffer(arr: FloatArray): FloatBuffer {
        val bb = ByteBuffer.allocateDirect(arr.size * 4).order(ByteOrder.nativeOrder())
        val fb = bb.asFloatBuffer()
        fb.put(arr)
        fb.position(0)
        return fb
    }
}
