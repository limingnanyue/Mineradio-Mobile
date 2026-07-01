package com.mineradio.player.render

import android.graphics.Bitmap
import android.opengl.GLES20
import android.opengl.GLUtils
import android.opengl.Matrix
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.util.Collections

/**
 * 3D 歌单架渲染器 —— 忠实移植桌面版 Three.js 沉浸式歌单架（index.html 3D shelf 场景）。
 *
 * 桌面版关键参数（与 Three.js PlaneGeometry(2.05, 1.025) + PSP 弧形一致）：
 *  - 每张封面是一个 2:1 宽高比的 plane（width=2.05, height=1.025）
 *  - shelfMode: "off" | "side" | "stage"
 *      · off   : 平铺一排，无旋转
 *      · side  : PSP 弧形侧展，封面沿圆弧排列并绕 Y 轴朝向圆心
 *      · stage : 舞台模式，中间封面正对镜头并放大，两侧逐级后退+旋转
 *  - 弧形半径 R、总跨度角、封面间距由数量自适应
 *  - 每张封面独立纹理（封面 URL → Bitmap → GL_TEXTURE_2D）
 *  - 香槟金边框 + 圆角（fragment 内 SDF 实现，对应桌面 .shelf-cover::after）
 *
 * 纹理上传策略：封面 Bitmap 在非 GL 线程解码后入队 [pendingBitmaps]，
 * onDrawFrame 时批量 glGenTextures + texImage2D。已淘汰的纹理主动回收。
 */
class ShelfRenderer {

    // ---- 程序 ----
    private var program = 0
    private var aPosLoc = -1
    private var aUvLoc = -1
    private var uMvpLoc = -1
    private var uTexLoc = -1
    private var uSelectedLoc = -1
    private var uTimeLoc = -1
    private var uModeLoc = -1

    // ---- 顶点 ----
    private var quadPosBuffer: FloatBuffer? = null
    private var quadUvBuffer: FloatBuffer? = null
    private val quadIndices = intArrayOf(0, 1, 2, 0, 2, 3)

    // ---- 视口 ----
    private var width = 1
    private var height = 1

    // ---- 矩阵 ----
    private val projection = FloatArray(16)
    private val view = FloatArray(16)
    private val model = FloatArray(16)
    private val mvp = FloatArray(16)

    // ---- 状态 ----
    data class State(
        val mode: ShelfMode = ShelfMode.SIDE,
        val coverUrls: List<String> = emptyList(),
        val selectedIndex: Int = 0,
    )

    enum class ShelfMode { OFF, SIDE, STAGE }

    @Volatile
    private var state: State = State()

    // ---- 纹理 ----
    private val textures = Collections.synchronizedList(mutableListOf<Int>())
    private val textureKeys = Collections.synchronizedList(mutableListOf<String>()) // url 对应已上传纹理
    private val pendingBitmaps = Collections.synchronizedList(mutableListOf<Pair<String, Bitmap>>())

    fun setState(next: State) {
        state = next
    }

    /** 在非 GL 线程把解码好的封面 Bitmap 推入队列，等待上传。 */
    fun enqueueCover(url: String, bmp: Bitmap) {
        pendingBitmaps.add(url to bmp)
    }

    fun onSurfaceCreated() {
        program = GlUtils.linkProgram(VERTEX_SHADER, FRAGMENT_SHADER)
        aPosLoc = GlUtils.attribLocation(program, "aPosition")
        aUvLoc = GlUtils.attribLocation(program, "aUv")
        uMvpLoc = GlUtils.uniformLocation(program, "uMvp")
        uTexLoc = GlUtils.uniformLocation(program, "uTex")
        uSelectedLoc = GlUtils.uniformLocation(program, "uSelected")
        uTimeLoc = GlUtils.uniformLocation(program, "uTime")
        uModeLoc = GlUtils.uniformLocation(program, "uMode")

        // 单个 plane 顶点（2:1，width=2, height=1，居中）
        val pos = floatArrayOf(
            -1f, -0.5f,  // 左下
             1f, -0.5f,  // 右下
             1f,  0.5f,  // 右上
            -1f,  0.5f,  // 左上
        )
        val uv = floatArrayOf(
            0f, 1f,
            1f, 1f,
            1f, 0f,
            0f, 0f,
        )
        quadPosBuffer = makeFloatBuffer(pos)
        quadUvBuffer = makeFloatBuffer(uv)
    }

    fun onSurfaceChanged(w: Int, h: Int) {
        width = maxOf(1, w)
        height = maxOf(1, h)
        GLES20.glViewport(0, 0, width, height)
        // 透视投影：fov 50°，近 0.1，远 100
        Matrix.perspectiveM(projection, 0, 50f, width.toFloat() / height.toFloat(), 0.1f, 100f)
        // 摄像机位于 (0, 0, 6)，看向原点
        Matrix.setLookAtM(view, 0, 0f, 0f, 6f, 0f, 0f, 0f, 0f, 1f, 0f)
    }

    fun onDrawFrame(timeSeconds: Float) {
        val s = state
        // 上传待处理封面
        uploadPending()

        GLES20.glClearColor(0f, 0f, 0f, 0f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)

        GLES20.glUseProgram(program)
        GLES20.glUniform1f(uTimeLoc, timeSeconds)
        GLES20.glUniform1i(uModeLoc, s.mode.ordinal)

        val count = s.coverUrls.size
        if (count == 0 || textures.isEmpty()) {
            GLES20.glDisable(GLES20.GL_DEPTH_TEST)
            GLES20.glDisable(GLES20.GL_BLEND)
            return
        }

        // 每张封面按 PSP 弧形摆放
        for (i in 0 until count) {
            val texIdx = i.coerceAtMost(textures.size - 1)
            val (tx, ty, tz, ry, scale) = layoutFor(i, count, s.mode)
            Matrix.setIdentityM(model, 0)
            Matrix.translateM(model, 0, tx, ty, tz)
            Matrix.rotateM(model, 0, ry, 0f, 1f, 0f)
            Matrix.scaleM(model, 0, scale, scale, 1f)
            multiplyMvp()
            GLES20.glUniformMatrix4fv(uMvpLoc, 1, false, mvp, 0)
            GLES20.glUniform1i(uSelectedLoc, if (i == s.selectedIndex) 1 else 0)
            // 绑定纹理
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[texIdx])
            GLES20.glUniform1i(uTexLoc, 0)
            drawQuad()
        }

        GLES20.glDisable(GLES20.GL_DEPTH_TEST)
        GLES20.glDisable(GLES20.GL_BLEND)
        GlUtils.checkGlError("drawShelf")
    }

    /**
     * PSP 弧形布局 —— 返回 (x, y, z, rotateY, scale)。
     *  - OFF   : 等距横排，z=0，无旋转
     *  - SIDE  : 沿半径 R 的圆弧排列，角度均匀分布，每张朝圆心（绕 Y 旋转）
     *  - STAGE : 中心封面正对镜头并放大，两侧逐级后退 + 偏转
     */
    private fun layoutFor(i: Int, count: Int, mode: ShelfMode): FloatArray {
        val center = (count - 1) / 2f
        val offset = i - center  // 相对中心的偏移
        return when (mode) {
            ShelfMode.OFF -> floatArrayOf(offset * 2.2f, 0f, 0f, 0f, 1f)
            ShelfMode.SIDE -> {
                val radius = 6f
                val totalAngle = minOf(90f, count * 14f)  // 总跨度，最多 90°
                val step = if (count > 1) totalAngle / (count - 1) else 0f
                val angle = (-totalAngle / 2f) + i * step  // 度
                val rad = Math.toRadians(angle.toDouble()).toFloat()
                val x = radius * sin(rad)
                val z = radius * (1f - cos(rad))  // 中心 z≈0，两侧 z>0（后退）
                floatArrayOf(x, 0f, -z, angle, 1f)  // rotateY = angle 让封面朝圆心
            }
            ShelfMode.STAGE -> {
                val absOff = abs(offset)
                val x = offset * 1.9f
                val z = absOff * 0.6f           // 越靠两侧越后退
                val ry = offset * 22f           // 两侧逐级偏转
                val scale = if (absOff < 0.5f) 1.18f else 1f - absOff * 0.08f
                floatArrayOf(x, 0f, -z, ry, scale.coerceAtLeast(0.7f))
            }
        }
    }

    private fun multiplyMvp() {
        Matrix.multiplyMM(mvp, 0, view, 0, model, 0)
        val tmp = FloatArray(16)
        Matrix.multiplyMM(tmp, 0, projection, 0, mvp, 0)
        System.arraycopy(tmp, 0, mvp, 0, 16)
    }

    private fun drawQuad() {
        quadPosBuffer?.let {
            it.position(0)
            GLES20.glEnableVertexAttribArray(aPosLoc)
            GLES20.glVertexAttribPointer(aPosLoc, 2, GLES20.GL_FLOAT, false, 0, it)
        }
        quadUvBuffer?.let {
            it.position(0)
            GLES20.glEnableVertexAttribArray(aUvLoc)
            GLES20.glVertexAttribPointer(aUvLoc, 2, GLES20.GL_FLOAT, false, 0, it)
        }
        // 两个三角形组成矩形
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, 4)
        if (aPosLoc >= 0) GLES20.glDisableVertexAttribArray(aPosLoc)
        if (aUvLoc >= 0) GLES20.glDisableVertexAttribArray(aUvLoc)
    }

    private fun uploadPending() {
        val s = state
        while (pendingBitmaps.isNotEmpty()) {
            val (url, bmp) = pendingBitmaps.removeAt(0)
            if (textureKeys.contains(url)) {
                bmp.recycle()
                continue
            }
            val tex = IntArray(1)
            GLES20.glGenTextures(1, tex, 0)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, tex[0])
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, bmp, 0)
            textures.add(tex[0])
            textureKeys.add(url)
            bmp.recycle()
            // 同步纹理数与封面 URL 列表（保持索引对齐）
            trimTextures(s.coverUrls)
        }
    }

    /** 丢弃不再需要的纹理（URL 已从列表移除）。 */
    private fun trimTextures(currentUrls: List<String>) {
        val toRemove = textureKeys.filter { it !in currentUrls }
        for (url in toRemove) {
            val idx = textureKeys.indexOf(url)
            if (idx >= 0) {
                GLES20.glDeleteTextures(1, intArrayOf(textures[idx]), 0)
                textures.removeAt(idx)
                textureKeys.removeAt(idx)
            }
        }
    }

    private fun makeFloatBuffer(arr: FloatArray): FloatBuffer {
        val bb = ByteBuffer.allocateDirect(arr.size * 4).order(ByteOrder.nativeOrder())
        val fb = bb.asFloatBuffer()
        fb.put(arr)
        fb.position(0)
        return fb
    }

    private fun abs(v: Float) = if (v < 0) -v else v
    private fun sin(v: Float) = kotlin.math.sin(v)
    private fun cos(v: Float) = kotlin.math.cos(v)

    companion object {
        /**
         * 顶点着色器 —— 接收 plane 顶点（2:1 宽高比）+ UV，乘以 mvp。
         */
        private val VERTEX_SHADER = """
            uniform mat4 uMvp;
            attribute vec2 aPosition;
            attribute vec2 aUv;
            varying vec2 vUv;
            void main() {
                vUv = aUv;
                gl_Position = uMvp * vec4(aPosition, 0.0, 1.0);
            }
        """.trimIndent()

        /**
         * 片段着色器 —— 采样封面纹理 + 圆角裁剪 + 选中高亮 + 香槟金描边。
         * 圆角与描边用 SDF（signed distance field）实现，对应桌面 .shelf-cover 圆角 + ::after 边框。
         */
        private val FRAGMENT_SHADER = """
            precision mediump float;
            uniform sampler2D uTex;
            uniform int uSelected;
            uniform float uTime;
            uniform int uMode;
            varying vec2 vUv;
            // 圆角矩形 SDF
            float roundedBox(vec2 p, vec2 b, float r) {
                vec2 q = abs(p) - b + r;
                return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - r;
            }
            void main() {
                // 圆角：把 UV 中心化到 [-1,1]，做 SDF
                vec2 p = vUv * 2.0 - 1.0;
                float d = roundedBox(p, vec2(0.96, 0.92), 0.12);
                if (d > 0.0) discard;
                vec4 col = texture2D(uTex, vUv);
                // 选中态：香槟金高光描边 + 轻微提亮
                float border = smoothstep(-0.06, 0.0, d);
                vec3 champagne = vec3(0.957, 0.823, 0.541);
                if (uSelected == 1) {
                    col.rgb = mix(col.rgb, champagne, border * 0.85);
                    col.rgb *= 1.08;
                } else {
                    // 未选中：暗化 + 香槟描边（更弱）
                    col.rgb = mix(col.rgb, champagne * 0.6, border * 0.4);
                    col.rgb *= 0.82;
                }
                gl_FragColor = vec4(col.rgb, col.a);
            }
        """.trimIndent()
    }
}
