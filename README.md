# Mineradio-Mobile

> Mineradio 移动端适配版 —— 在桌面版 Electron 播放器基础上，新增 **原生 Android (Kotlin Jetpack Compose)**、**Capacitor WebView 壳** 与 **Docker 后端** 三套移动端方案，UI 与功能与桌面版完全对齐。

[![License](https://img.shields.io/badge/license-GPL--3.0-blue.svg)](./LICENSE)
[![Platform](https://img.shields.io/badge/platform-Android%20%7C%20Capacitor%20%7C%20Electron%20%7C%20Docker-lightgrey.svg)]()
[![Original](https://img.shields.io/badge/upstream-XxHuberrr%2FMineradio-orange.svg)](https://github.com/XxHuberrr/Mineradio)

---

## 📖 项目说明

### 来源与开源声明

本项目是 [**XxHuberrr/Mineradio**](https://github.com/XxHuberrr/Mineradio)（桌面版 Electron 音乐播放器）的 **社区移动端移植分支**，由移动端适配工作独立完成，遵循原项目 [GPL-3.0](./LICENSE) 协议开源。

- **上游原始项目**：<https://github.com/XxHuberrr/Mineradio>
- **本仓库新增内容**：`android/`（原生 Kotlin Compose 工程）、`mobile/`（Capacitor 壳配置）、`public/mobile/`（WebView 桥接层）、`backend/`（Docker 部署文件）
- **桌面版代码**：`desktop/`、`public/index.html`、`server.js` 等保持上游原貌，零侵入
- **License**：GPL-3.0（与上游一致），任何二次分发必须保留本声明与协议

如上游作者认为本移植分支有任何不妥，请联系本仓库维护者处理。

### 移动端方案对比

本仓库同时提供两套移动端方案，可按需选择其一或共存：

| 方案 | 路径 | 特点 | 适用场景 |
|------|------|------|----------|
| **原生 Android** | `android/` | Kotlin + Jetpack Compose + OpenGL ES 2.0 + Media3 ExoPlayer，3D 粒子星河背景与 3D 歌单架均为真 OpenGL 渲染 | 追求原生性能、3D 视觉、MediaSession 通知栏集成的场景 |
| **Capacitor 壳** | `mobile/` + `public/mobile/` | 复用桌面版前端 WebView，桥接层在非 Capacitor 环境完全 no-op，桌面版零影响 | 想以最小改动跑现有前端、快速出移动端的场景 |
| **Docker 后端** | `backend/` | 复用原 `server.js`，通过环境变量重映射路径到 `/data`，含健康检查与 `.env.example` | 远程部署后端供移动端连接的场景 |

### 原生 Android 工程要点

- **包名**：`com.mineradio.player`
- **构建**：Gradle 8.9 + AGP 8.5.2 + Kotlin 2.0.20 + Compose BOM 2024.09
- **3D 视觉**：`ParticleGalaxyBackground` / `Shelf3DPanel` / `ShelfRenderer` 均为 `GLSurfaceView` + GLES2 + 自写 GLSL，1:1 移植 `wallpaper.html` 粒子星系
- **播放服务**：`PlaybackService` 继承 `MediaSessionService`，自动持有前台通知 + 锁屏媒体控件（替代桌面歌词悬浮窗）
- **UI 完整度**：底栏 1:1 复刻桌面版 `#bottom-bar` 三段式（actions / transport / modes），DIY 配置真正驱动歌词与粒子背景渲染

### 原生 Android 还原进度（P1 完成）

原生 Android 端已按桌面版 `public/index.html` 完成两阶段功能/UI/特效还原：

**P0 阶段（基础对齐）**
- `BottomControlBar` 三段式：音质 pill / 喜欢 / 收藏 / 播放模式 / 迷你队列 / 歌词开关 / 自动隐藏 / 沉浸 / 可拖拽进度条
- `LyricStage` 消费 FxState：overlayColors + lyricScale / Weight / LetterSpacing / LineHeight / Font
- `CoverCropModal` 九宫格裁剪舞台 + 香槟金内发光
- `ParticleGalaxyBackground` DIY 驱动：custom 配色实时传入渲染器
- `FxState` 字段补齐：lyricFont / lyricLetterSpacing / lyricLineHeight / lyricWeight / lyricScale

**P1 阶段（视觉控制台与弹窗体系）**
- `FxState` / `FxArchiveSnapshot` 同步：粒子高级参数（particleSize/Speed/Twist/Color/Bloom/Scatter/BgFade）+ 3D 架参数（shelfSize/X/Y/Z/Angle/Opacity/BgAlpha/Accent/ShowPodcasts/MergeCollections/CameraMode/PresenceMode）+ cameraInteraction
- `ParticleGalaxyRenderer` + GLSL 着色器接入 7 个粒子 uniform（uSizeScale/uSpeedScale/uTwistScale/uColorScale/uBloomScale/uScatterScale + bgFade），高光阈值与溢光强度由 DIY 实时调节
- `ShelfRenderer` 接入 3D 架参数：全局 translate(offset)+rotate(angle)，片元着色器用 uAccent 替代硬编码香槟金，col.a 乘 uOpacity
- `SearchBar` 4 模式 tab（all/netease/qq/podcast），播客搜索分流到 podcastSearchResults
- `FxPanel` 完整视觉控制台：6 个可折叠 fold（视觉预设 / 主控 / 歌词 / 叠加壁纸 / 3D 歌单架 / 粒子星河高级），直接绑定 FxState，色彩行调起色彩实验室
- `CustomLyricModal` LRC 编辑器：粘贴 `[mm:ss.xx]` 时间轴或纯文本，保存即解析应用
- `ColorLabPop` 色彩实验室：HSV 三滑块 + 8 预设色板，按 target 应用到 lyric/highlight/glow/tint/shelfAccent
- `VisualGuide` 首次使用引导：5 步分步卡片（DIY/3D架/色彩实验室/自定义歌词/沉浸），进度点 + 跳过
- `LocalBeatModal` 本地节奏分析：Canvas 滚动波形 + BPM 估算结果展示
- `CoverCropModal` 接入实际封面：Coil 异步加载当前曲目封面 → Bitmap，裁剪后写回 MediaSession artwork
- `TopBar` 更新角标：设置图标 `BadgedBox` 显示新版本号，新增视觉控制台/自定义歌词/本地节奏/使用引导入口
- `PlayerController.setArtworkBitmap`：裁剪封面写入缓存并替换当前 MediaItem artworkUri

**渲染管线总览**

```
FxState (DIY 配置)
  ├─→ ParticleGalaxyBackground → ParticleGalaxyRenderer (GLES2)
  │     └─ 7 个粒子 uniform + bgFade 背景压缩
  ├─→ Shelf3DPanel → ShelfRenderer (GLES2)
  │     └─ uAccent/uOpacity/uBgAlpha + 全局 transform
  └─→ LyricStage (Compose)
        └─ overlayColors + 字体/字间距/行高/字重/缩放
```

---

## 🛠️ 打包与编译方法

### 一、原生 Android APK 编译（推荐）

#### 方式 A：Android Studio（最简单，推荐新手）

1. **安装 Android Studio**（Hedgehog 2023.1.1 或更高版本）：
   - 下载：<https://developer.android.com/studio>
   - 安装时勾选 **Android SDK**、**Android SDK Platform-Tools**、**Android Emulator**

2. **打开工程**：
   - `File → Open` → 选择 `android/` 目录
   - 等待 Gradle Sync 完成（首次会下载约 1.5GB 依赖，需联网）

3. **配置签名**（发布版必需）：
   - `Build → Generate Signed Bundle / APK → APK`
   - 首次需 `Create new...` 一个 keystore（妥善保管，丢失则无法升级）
   - 选 `release` Build Variant，勾选 V1/V2 签名

4. **打包**：
   - `Build → Build Bundle(s) / APK(s) → Build APK(s)`
   - 输出路径：`android/app/build/outputs/apk/debug/app-debug.apk`（debug）
   - 或 `app/release/app-release.apk`（release）

5. **安装到设备**：
   ```bash
   adb install -r android/app/build/outputs/apk/debug/app-debug.apk
   ```

#### 方式 B：命令行 Gradle（CI / 自动化推荐）

```bash
# 进入 android 目录
cd android

# 1. 确保环境：JDK 17、Android SDK、ANDROID_HOME 已配置
java -version          # 需要 17+
echo $ANDROID_HOME     # 例如 ~/Android/Sdk

# 2. 调试版打包（无需签名，可直接安装）
./gradlew assembleDebug
# 产物：app/build/outputs/apk/debug/app-debug.apk

# 3. 发布版打包（需先在 app/build.gradle.kts 配置签名）
./gradlew assembleRelease
# 产物：app/build/outputs/apk/release/app-release.apk

# 4. 清理
./gradlew clean
```

#### 配置签名（命令行 release 必需）

在 `android/app/build.gradle.kts` 的 `android { signingConfigs { ... } }` 中加入：

```kotlin
signingConfigs {
    create("release") {
        storeFile = file(System.getenv("KEYSTORE_PATH") ?: "release.keystore")
        storePassword = System.getenv("KEYSTORE_PASSWORD")
        keyAlias = System.getenv("KEY_ALIAS")
        keyPassword = System.getenv("KEY_PASSWORD")
    }
}
buildTypes {
    release {
        signingConfig = signingConfigs.getByName("release")
        isMinifyEnabled = true
        proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
}
```

然后导出环境变量后打包：
```bash
export KEYSTORE_PATH=/path/to/release.keystore
export KEYSTORE_PASSWORD=******
export KEY_ALIAS=******
export KEY_PASSWORD=******
./gradlew assembleRelease
```

#### 配置后端地址

APK 安装后，启动 App → 顶部齿轮「设置」→ 填入后端地址（默认为空）。后端可：
- 本地用 Node 起 `node server.js`（同 WiFi 调试）
- 远程用下方 Docker 部署

---

### 二、Capacitor 壳打包（复用前端 WebView）

```bash
# 1. 进入 mobile 目录
cd mobile

# 2. 安装依赖并生成原生壳
npm install
npx cap add android     # 首次生成 android 壳（与上层 android/ 目录无关，互不干扰）
# 或 npx cap add ios

# 3. 把前端资源同步到原生壳
npm run sync            # 等价于 npx cap copy

# 4. 用 Android Studio 打开生成的工程打包
npx cap open android
# 后续步骤同「原生 Android」方式 A
```

> ⚠️ 注意：根目录的 `android/` 是独立原生 Kotlin 工程，与 `mobile/android/`（Capacitor 自动生成）是两套，请勿混淆。

---

### 三、Docker 后端部署

```bash
cd backend

# 1. 复制环境变量模板并按需修改
cp .env.example .env
# 编辑 .env：PORT=3000、DATA_DIR=/data、MEDIA_ROOT=/data/media 等

# 2. 一键启动（含健康检查 + 自动重启）
docker compose up -d

# 3. 查看日志
docker compose logs -f

# 4. 升级 / 重建
docker compose pull && docker compose up -d
```

部署后，移动端「设置」填入 `http://<服务器IP>:3000` 即可连接。

---

### 四、桌面版（参考，未改动）

```bash
# 开发模式
npm install
npm start

# 打包 Windows 安装包
npm run build:win
```

桌面版入口、依赖、构建脚本均与上游一致，本仓库未做任何修改。

---

## 📁 项目结构

```
Mineradio-Mobile/
├── android/              # 【新增】原生 Android Kotlin Compose 工程
│   ├── app/
│   │   └── src/main/java/com/mineradio/player/
│   │       ├── data/         # API / 播放服务 / 仓库
│   │       ├── render/       # OpenGL ES 粒子星河 + 3D 歌单架
│   │       ├── ui/           # Compose UI（screen / component / fx / theme）
│   │       └── MainActivity.kt
│   ├── build.gradle.kts
│   └── settings.gradle.kts
├── mobile/               # 【新增】Capacitor 壳配置
│   ├── capacitor.config.json
│   └── package.json
├── public/
│   ├── mobile/           # 【新增】WebView 桥接层（mobile.css / mobile-bridge.js / mobile-init.js）
│   ├── index.html        # 桌面版前端（仅 +4 行引入移动端桥接，桌面/浏览器完全 no-op）
│   └── ...
├── backend/              # 【新增】Docker 部署
│   ├── Dockerfile
│   ├── docker-compose.yml
│   └── .env.example
├── desktop/              # 桌面版 Electron 主进程（上游原貌）
├── server.js             # Node 服务端（上游原貌，Docker 复用）
├── package.json
└── LICENSE               # GPL-3.0
```

---

## 🤝 致谢

- **上游项目**：[XxHuberrr/Mineradio](https://github.com/XxHuberrr/Mineradio) —— 提供完整的桌面版播放器、3D 视觉、登录、歌单、播客等核心逻辑
- **依赖开源项目**：
  - [Jetpack Compose](https://developer.android.com/jetpack/compose) — Android 声明式 UI
  - [Media3 / ExoPlayer](https://github.com/androidx/media) — 媒体播放
  - [Coil](https://github.com/coil-kt/coil) — 图片加载
  - [Retrofit](https://github.com/square/retrofit) — HTTP 客户端
  - [Three.js](https://threejs.org/) — 桌面版 3D 引擎
  - [Capacitor](https://capacitorjs.com/) — WebView 壳
  - [Electron](https://www.electronjs.org/) — 桌面端框架

---

## 📜 协议

[GPL-3.0](./LICENSE) © 上游 XxHuberrr 与本仓库贡献者。

任何二次分发、修改、商用必须：
1. 保留本协议与版权声明
2. 开源衍生代码
3. 注明上游与本仓库来源
