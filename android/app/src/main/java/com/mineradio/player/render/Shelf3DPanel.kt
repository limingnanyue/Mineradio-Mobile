package com.mineradio.player.render

import android.content.Context
import android.graphics.Bitmap
import android.opengl.GLSurfaceView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.ImageLoader
import coil.request.ImageRequest
import coil.size.Size
import com.mineradio.player.data.api.dto.Playlist
import com.mineradio.player.ui.theme.MineradioColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * 3D 歌单架 —— Compose 包装层。
 *
 * 上半部：OpenGL ES 渲染的 PSP 弧形歌单架（视觉主体，对应桌面 Three.js 场景）。
 * 下半部：可点击的封面缩略图横条（移动端触摸适配，因 GLES2 射线拾取成本过高，
 *   用 Compose 缩略条提供可靠的点击目标，同时保留 3D 视觉）。
 *
 * 三种模式与桌面 shelfMode 一致：OFF（平铺）/ SIDE（侧弧）/ STAGE（舞台）。
 */
class ShelfState {
    internal val renderer = ShelfRenderer()
    internal var view: GLSurfaceView? = null

    fun setMode(mode: ShelfRenderer.ShelfMode, urls: List<String>, selectedIndex: Int) {
        renderer.setState(ShelfRenderer.State(mode = mode, coverUrls = urls, selectedIndex = selectedIndex))
    }

    /** 推送完整状态（含 DIY 参数）到渲染器。 */
    fun setFullState(state: ShelfRenderer.State) {
        renderer.setState(state)
    }
}

@Composable
fun Shelf3DPanel(
    playlists: List<Playlist>,
    selectedIndex: Int,
    mode: ShelfRenderer.ShelfMode,
    onSelect: (Int) -> Unit,
    onModeChange: (ShelfRenderer.ShelfMode) -> Unit,
    modifier: Modifier = Modifier,
    // 3D 架 DIY 参数（对应桌面版 fx.shelf*）
    shelfSize: Float = 1.0f,
    shelfX: Float = 0f,
    shelfY: Float = 0f,
    shelfZ: Float = 0f,
    shelfAngle: Float = 0f,
    shelfOpacity: Float = 1.0f,
    shelfBgAlpha: Float = 0.0f,
    shelfAccent: Color = Color(0xFFF4D28A),
) {
    val context = LocalContext.current
    val shelfState = remember { ShelfState() }
    val scope = rememberCoroutineScope()
    val coverUrls = remember(playlists) {
        playlists.map { it.coverImgUrl ?: it.picUrl ?: "" }.filter { it.isNotEmpty() }
    }
    var loadedCount by remember(playlists) { mutableStateOf(0) }

    // 推送状态到渲染器（含 DIY 参数）
    LaunchedEffect(mode, coverUrls, selectedIndex, shelfSize, shelfX, shelfY, shelfZ, shelfAngle, shelfOpacity, shelfBgAlpha, shelfAccent) {
        shelfState.setFullState(
            ShelfRenderer.State(
                mode = mode,
                coverUrls = coverUrls,
                selectedIndex = selectedIndex,
                size = shelfSize,
                offsetX = shelfX,
                offsetY = shelfY,
                offsetZ = shelfZ,
                angle = shelfAngle,
                opacity = shelfOpacity,
                bgAlpha = shelfBgAlpha,
                accent = floatArrayOf(shelfAccent.red, shelfAccent.green, shelfAccent.blue),
            )
        )
    }

    // 用 Coil 异步加载封面 bitmap → 推入渲染器队列
    LaunchedEffect(coverUrls) {
        if (coverUrls.isEmpty()) return@LaunchedEffect
        val loader = ImageLoader(context)
        scope.launch {
            for ((i, url) in coverUrls.withIndex()) {
                val bmp = loadCover(loader, context, url) ?: continue
                withContext(Dispatchers.Main) {
                    shelfState.renderer.enqueueCover(url, bmp)
                    loadedCount = i + 1
                }
            }
        }
    }

    Column(modifier.fillMaxSize().background(MineradioColors.FcBg)) {
        // 上：3D 渲染区
        Box(Modifier.weight(1f).fillMaxWidth()) {
            AndroidView(
                factory = {
                    val v = ShelfGLSurfaceView(it, shelfState.renderer)
                    shelfState.view = v
                    v.onResume()
                    v
                },
                modifier = Modifier.fillMaxSize(),
            )
            // 模式切换胶囊（左上）
            Row(
                Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp),
            ) {
                listOf(
                    ShelfRenderer.ShelfMode.OFF to "平铺",
                    ShelfRenderer.ShelfMode.SIDE to "侧弧",
                    ShelfRenderer.ShelfMode.STAGE to "舞台",
                ).forEach { (m, label) ->
                    val active = mode == m
                    Box(
                        Modifier
                            .padding(end = 6.dp)
                            .clip(RoundedCornerShape(50))
                            .background(if (active) MineradioColors.FcAccent else Color(0x22FFFFFF))
                            .clickable { onModeChange(m) }
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    ) {
                        Text(label, color = if (active) MineradioColors.ChillInk else MineradioColors.FcInk2, fontSize = 11.sp)
                    }
                }
            }
            // 加载进度（右上）
            if (loadedCount < coverUrls.size) {
                Text(
                    "$loadedCount / ${coverUrls.size}",
                    color = MineradioColors.FcMuted,
                    fontSize = 10.sp,
                    modifier = Modifier.align(Alignment.TopEnd).padding(12.dp),
                )
            }
        }
        // 下：缩略图横条（触摸选择）
        if (playlists.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                itemsIndexed(playlists) { i, pl ->
                    val sel = i == selectedIndex
                    Column(
                        Modifier
                            .width(96.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xA6101216))
                            .border(
                                if (sel) 1.5.dp else 0.dp,
                                if (sel) MineradioColors.FcAccent else Color.Transparent,
                                RoundedCornerShape(10.dp),
                            )
                            .clickable { onSelect(i) }
                            .padding(6.dp),
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(MineradioColors.GlassDark),
                        ) {
                            val cover = pl.coverImgUrl ?: pl.picUrl
                            if (!cover.isNullOrEmpty()) {
                                coil.compose.AsyncImage(model = cover, contentDescription = pl.name, modifier = Modifier.fillMaxSize())
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            pl.name,
                            color = if (sel) MineradioColors.FcAccent else MineradioColors.FcInk2,
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

private suspend fun loadCover(loader: ImageLoader, context: Context, url: String): Bitmap? {
    return runCatching {
        val req = ImageRequest.Builder(context)
            .data(url)
            .size(Size.ORIGINAL)
            .build()
        loader.execute(req).drawable?.toBitmap()
    }.getOrNull()
}

private fun android.graphics.drawable.Drawable.toBitmap(): Bitmap {
    return if (this is android.graphics.drawable.BitmapDrawable) {
        bitmap
    } else {
        val w = maxOf(1, intrinsicWidth.coerceAtMost(512))
        val h = maxOf(1, intrinsicHeight.coerceAtMost(512))
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bmp)
        setBounds(0, 0, w, h)
        draw(canvas)
        bmp
    }
}

private class ShelfGLSurfaceView(
    context: Context,
    private val renderer: ShelfRenderer,
) : GLSurfaceView(context) {
    init {
        setEGLContextClientVersion(2)
        setEGLConfigChooser(8, 8, 8, 8, 16, 0)
        setRenderer(object : Renderer {
            override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
                renderer.onSurfaceCreated()
            }

            override fun onSurfaceChanged(gl: GL10?, w: Int, h: Int) {
                renderer.onSurfaceChanged(w, h)
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
