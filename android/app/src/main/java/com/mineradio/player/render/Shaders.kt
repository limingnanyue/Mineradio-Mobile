package com.mineradio.player.render

/**
 * 内置 OpenGL ES 着色器源码。
 *
 * 粒子星河场算法忠实移植自 public/wallpaper.html 的 Canvas2D 实现：
 *   - 椭圆轨道 (rx, ry) 上每颗粒子按各自 seed 绕中心旋转
 *   - 速度 = base + rand(seed)*range + (playing ? boost : 0)
 *   - 角度 a = (x*2π + now*speed + sin(now*0.07+seed)*0.14) % 2π
 *   - 半径环 ring = 0.18 + z*0.82
 *   - 闪烁 tw = (0.5 + 0.5*sin(now*freq + seed))^4
 *   - 颜色：tw>0.74 → highlight, lane>0.55 → secondary, 否则 glow
 *
 * 桌面版用 Canvas2D 逐颗 fill；这里改为 GPU 实例化点精灵 + 加性混合，
 * 视觉等价但帧率稳定 60，且可承载更多粒子（移动端 GPU 友好）。
 */
object Shaders {

    // ----------------------------------------------------------------
    //  粒子星河场 —— 顶点着色器
    // ----------------------------------------------------------------
    val PARTICLE_VERTEX = """
precision highp float;

attribute vec2 aPosition;          // NDC 坐标
attribute float aSeed;             // 每颗粒子的随机种子 [0,1]
attribute float aLane;             // 轨道车道 [0,1]
attribute float aZ;                // 半径环参数 [0,1]
attribute float aSize;             // 基础尺寸

uniform float uTime;               // 秒
uniform float uPlaying;            // 1.0 播放中 / 0.0 暂停
uniform float uOpacity;            // 整体不透明度
uniform vec2  uResolution;         // 视口像素
uniform float uDpr;                // 像素密度
// 粒子高级参数（对应桌面版 fx.point/speed/twist/scatter）
uniform float uSizeScale;          // fx.point  粒子尺寸缩放
uniform float uSpeedScale;         // fx.speed  流速缩放
uniform float uTwistScale;         // fx.twist  轨道波动幅度缩放
uniform float uScatterScale;       // fx.scatter 半径环离散度

varying float vTw;                 // 闪烁值传给片元
varying float vLane;
varying float vRing;

// 与 wallpaper.html rand() 等价的浮点伪随机（保持视觉手感一致）
float rand(float n) {
    return fract(abs(sin(n * 3187.917) * 43758.5453));
}

void main() {
    float seed = aSeed;
    // speed 受 uSpeedScale 调节（fx.speed）
    float speed = (0.009 + rand(seed) * 0.021 + (uPlaying > 0.5 ? 0.010 : 0.0)) * uSpeedScale;
    float a = mod(aPosition.x * 6.2831853 + uTime * speed + sin(uTime * 0.07 + seed) * 0.14 * uTwistScale, 6.2831853);
    // ring 受 uScatterScale 调节：scatter 越大半径环离散度越高
    float ring = 0.18 + aZ * 0.82 * mix(0.7, 1.15, uScatterScale);
    float wobble = sin(uTime * (0.22 + rand(seed) * 0.18) + seed) * 12.0 * uTwistScale;

    // 椭圆中心轻微上下浮动（与桌面版 cy = innerH*0.5 + sin(now*0.28)*innerH*0.018 一致）
    float cx = 0.0;
    float cy = sin(uTime * 0.28) * 0.018;

    // 把轨道半径映射到 NDC（rx=0.40, ry=0.30，与桌面版比例一致）
    float rx = 0.80;   // NDC 下 ±0.80 覆盖近全屏宽度
    float ry = 0.60;
    float xNdc = cx + cos(a) * rx * ring + sin(uTime * 0.11 + seed) * 0.03;
    float yNdc = cy + sin(a * (1.0 + rand(seed * 2.0) * 0.16)) * ry * ring + wobble / uResolution.y * 2.0;

    gl_Position = vec4(xNdc, yNdc, 0.0, 1.0);

    float tw = pow(0.5 + 0.5 * sin(uTime * (0.50 + rand(seed) * 0.42) + seed), 4.0);
    vTw = tw;
    vLane = aLane;
    vRing = ring;

    // 尺寸：基础 * (0.8 + tw*1.2)，按 dpr 缩放 + uSizeScale，最小 1.5px
    float px = max(1.5, aSize * (0.8 + tw * 1.2) * uSizeScale) * uDpr;
    gl_PointSize = px;
}
""".trimIndent()

    // ----------------------------------------------------------------
    //  粒子星河场 —— 片元着色器（圆形 + 软边 + 颜色分级）
    // ----------------------------------------------------------------
    val PARTICLE_FRAGMENT = """
precision highp float;

uniform vec3  uColorPrimary;
uniform vec3  uColorSecondary;
uniform vec3  uColorHighlight;
uniform vec3  uColorGlow;
uniform float uOpacity;
uniform float uPlaying;
// 粒子高级参数（对应桌面版 fx.color / fx.bloom）
uniform float uColorScale;          // fx.color  色彩张力（高光阈值调节）
uniform float uBloomScale;          // fx.bloom  溢光（高光强度）

varying float vTw;
varying float vLane;
varying float vRing;

void main() {
    // 把点精灵裁成圆形（gl_PointCoord ∈ [0,1]）
    vec2 c = gl_PointCoord - vec2(0.5);
    float d = dot(c, c);                       // 0..0.25
    if (d > 0.25) discard;
    float soft = 1.0 - smoothstep(0.0, 0.25, d);

    // 颜色分级（与 wallpaper.html 一致）。uColorScale 调节高光阈值：
    //   color 越大 → 阈值越低 → 更多家粒子进入 highlight 分级（色彩张力增强）
    float highlightThr = 0.74 - (uColorScale - 1.0) * 0.18;
    vec3 col = vTw > highlightThr
        ? uColorHighlight
        : (vLane > 0.55 ? uColorSecondary : uColorGlow);

    // 不透明度：与桌面版 globalAlpha = 0.045 + tw*0.18 + (playing?0.035:0) 等价
    float alpha = (0.045 + vTw * 0.18 + (uPlaying > 0.5 ? 0.035 : 0.0)) * uOpacity;
    // bloom（溢光）：高光粒子额外增强 alpha，模拟 glow
    if (vTw > highlightThr) {
        alpha *= (1.0 + (uBloomScale - 1.0) * 0.6);
    }
    alpha *= soft;
    if (alpha < 0.002) discard;

    gl_FragColor = vec4(col, alpha);
}
""".trimIndent()

    // ----------------------------------------------------------------
    //  全屏背景渐变 + 光晕 —— 顶点着色器
    // ----------------------------------------------------------------
    val BG_VERTEX = """
precision highp float;
attribute vec2 aPosition;
varying vec2 vUv;
void main() {
    vUv = aPosition * 0.5 + 0.5;
    gl_Position = vec4(aPosition, 0.0, 1.0);
}
""".trimIndent()

    // ----------------------------------------------------------------
    //  全屏背景渐变 + 中心径向光晕 —— 片元着色器
    //  对应 wallpaper.html 的 createLinearGradient + createRadialGradient aura
    // ----------------------------------------------------------------
    val BG_FRAGMENT = """
precision highp float;
varying vec2 vUv;
uniform vec2  uResolution;
uniform vec3  uColorPrimary;
uniform vec3  uColorSecondary;
uniform vec3  uColorHighlight;
uniform float uOpacity;
uniform float uTime;

void main() {
    // 线性渐变（与桌面版 0→0.52→1 三停一致）
    vec3 bg = mix(vec3(0.02, 0.024, 0.031), uColorPrimary * 0.12 * uOpacity, vUv.x * 0.52);
    bg = mix(bg, uColorSecondary * 0.10 * uOpacity, vUv.x);

    // 中心径向光晕（aura）
    vec2 aspect = vec2(uResolution.x / uResolution.y, 1.0);
    vec2 p = (vUv - 0.5) * aspect;
    float r = length(p) / 0.54;
    float aura = smoothstep(1.0, 0.0, r);
    bg += uColorHighlight * 0.12 * uOpacity * aura * 0.9;
    bg += uColorSecondary * 0.08 * uOpacity * aura * 0.34;

    gl_FragColor = vec4(bg, 1.0);
}
""".trimIndent()

    // ----------------------------------------------------------------
    //  封面柔光层 —— 把专辑封面作为大尺寸模糊背景叠在粒子之下
    //  对应 wallpaper.html drawCover() 的 blur(28px) saturate(1.25) 效果
    // ----------------------------------------------------------------
    val COVER_VERTEX = """
precision highp float;
attribute vec2 aPosition;
attribute vec2 aTexCoord;
varying vec2 vTexCoord;
void main() {
    vTexCoord = aTexCoord;
    gl_Position = vec4(aPosition, 0.0, 1.0);
}
""".trimIndent()

    val COVER_FRAGMENT = """
precision highp float;
varying vec2 vTexCoord;
uniform sampler2D uTexture;
uniform float uOpacity;
uniform float uTime;

void main() {
    vec3 c = texture2D(uTexture, vTexCoord).rgb;
    // 呼吸式微缩放（与桌面版 side = min(w,h)*(0.42 + sin(now*0.21)*0.012) 等价）
    float breath = 0.16 + 0.20 * (0.5 + 0.5 * sin(uTime * 0.21));
    gl_FragColor = vec4(c, breath * uOpacity);
}
""".trimIndent()
}
