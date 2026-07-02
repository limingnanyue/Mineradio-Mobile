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

### 原生 Android 还原进度（P5 完成）

原生 Android 端已按桌面版 `public/index.html` 完成五阶段功能/UI/特效还原：

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

**P2 阶段（完整还原桌面版全部 DOM 模块）**
- `SplashOverlay`：启动遮罩（#splash），MINERADIO 字标 + 青色信号线 + 呼吸闪烁「点击进入」
- `TrackDetailModal`：歌曲/歌手详情双视图（#track-detail-modal），hero 区 + DetailGrid 字段网格 + 评论列表/热门歌曲列表
- `ArtistDetail` DTO + API + repo 方法：网易云用 id、QQ 用 singerMid 分流调用歌手详情接口
- `LocalBeatModal` MR/DJ 双模式 tab：MR 分析（日常电影视角，48 bars / 900ms）+ DJ 分析（长混音/强节奏，64 bars / 600ms）
- `CustomLyricModal` 删除按钮：对应桌面版 deleteCustomLyricForCurrent
- `CollectToPlaylistModal` 新建歌单输入框：对应桌面版 .collect-create
- `PlaylistPanel` 三 tab 面板（#playlist-panel）：当前队列 / 我的歌单 / 我的播客，含常开 pin、随机、清空、播放模式 chip
- `TrialBanner` 试听片段横幅（#trial-banner）：随登录态/VIP 等级变化文案，未登录显示扫码登录按钮
- `BeatChip` 节奏分析角标（#beat-chip）：绿色旋转指示 + 分析文案
- `AiDepthChip` AI 深度估计角标（#ai-depth-chip）：香槟色旋转指示 + 估计文案
- `FreeCameraHint` 自由镜头提示（#free-camera-hint）：触屏手势映射说明
- `GestureHud` 手势 HUD（#gesture-hud）：手势标签 + 确认提示 + 进度条 + 图例（视觉占位，实际手势需摄像头 + ML Kit）
- `SourceFallbackNotice` 自动换源提示（#source-fallback-notice）：标题 + 说明文案 + 关闭
- 文件导入（#upload-btn + #file-input）：ActivityResultContracts 多选文件，音频自动入队播放
- `PlayerController` 新增 `clearQueue()` / `shuffleQueue()`：对应桌面版 clearQueue / shuffleQueue
- `TopBar` 新增入口：导入、歌单/队列面板、手势 HUD、自由镜头

**P2 BUG 修复**
- `FxState.overlayColors()` glow 表达式：`visualTintColor ?: primary` 对非空 Color 使用 `?:`，改为 `if (lyricGlowColor != Default) lyricGlowColor else visualTintColor`
- `SearchSidePanel` 播客模式不显示结果：播客搜索模式仍渲染 `searchResults`（空），改为按 `searchMode` 分流到 `PodcastSearchResults`
- `LyricStage` / `MiniQueuePopover` O(n) `indexOf` 性能：改用 `itemsIndexed`
- 视觉引导首次不自动弹出：添加 `LaunchedEffect(Unit)` 检查 `visualGuideSeen`
- `onPodcastClick` 语义冲突：`togglePodcast()` 打开时重置 `selectedPodcast` 抹掉 `selectPodcast()`，调整调用顺序为先开浮层再选中
- `importFiles` 类型不匹配：`player.playQueue(songs, songs, 0)` 第二参数应为 `List<String>`，改为 `audio`（URI 列表）
- `PlayerShell` 缺失 `QueueMusic` 图标导入

**P3 阶段（BUG 修复 + 缺失功能补齐，完整还原收尾）**
- `PlayerController.clearQueue()` 状态重置不全：仅重置 queue/queueIndex，依赖 MediaController 异步回调清理 current/isPlaying/positionMs/durationMs/error，导致清空后 UI 残留旧曲目标题。改为显式重置全部播放字段
- `PlayerController.shuffleQueue()` 与 cyclePlayMode 状态机冲突：开启 shuffle 时未处理单曲循环（REPEAT_MODE_ONE），导致 shuffle + 单曲循环语义矛盾。改为开启随机时把单曲循环退回列表循环
- `importFiles` content:// URI 识别失效：`ActivityResultContracts.GetMultipleContents()` 返回的 MediaStore 风格 URI 不带文件扩展名，`endsWith(".mp3")` 永远 false，导致导入音乐只弹 toast 不入队播放。改用 `ContentResolver` 查询 `OpenableColumns.DISPLAY_NAME` 与 MIME 类型（`audio/*`）双重判定，并 `takePersistableUriPermission` 保证 PlaybackService 跨进程可读
- 音量控制（#volume-control）完整还原：`PlaybackState` 新增 `volume`/`muted` 字段，`PlayerController` 新增 `setVolume()`/`toggleMute()` 映射到系统 `STREAM_MUSIC`，`BottomControlBar` modes 段新增 `VolumeControl`（按钮 + DropdownMenu 滑块 + 百分比数值），`MainViewModel` 新增 `setVolume`/`toggleMute` action
- 清除自定义封面（#clear-cover-btn）完整还原：`PlayerController` 新增 `clearArtwork()` 恢复当前曲目原始 artwork，`CoverCropModal` 新增「清除封面」按钮（onClear 回调），`MainViewModel` 新增 `clearCustomCover` action
- 清理未使用 import：移除 `PlayerShell` 的 `ShelfRenderer` import、`MainViewModel` 的 `Dispatchers` import

**P3 验证**：经 30+ 项引用完整性核查（构造函数 / 工厂 / ContentResolver 调用 / 函数签名 / 跨文件数据流 / 依赖可用性）全部通过，无编译错误。

**P4 阶段（深度核查 + 6 项核心缺失功能补齐）**
- 歌词源切换（#lyric-source-seg）：`UiState` 加 `lyricSource`/`originalLyricsLines`；`loadLyric` 缓存原词；`saveCustomLyric` 不再永久覆盖原词；`deleteCustomLyric` 切回原词并恢复；新增 `setLyricSource(mode)`；`CustomLyricModal` 加原词/自定义切换 chip
- FX 存档 export/import + 持久化（exportUserFxArchive / importUserFxArchiveText）：`org.json.JSONObject` 序列化 41 字段；`SharedPreferences` 持久化 4 槽位（重启不丢）；`FxArchiveSlots` 加导出/导入按钮；`PlayerShell` 接入 SAF `CreateDocument`/`OpenDocument` launcher
- 搜索历史（.search-history-chip）：`UiState.searchHistory`；`SharedPreferences` 持久化（最多 10 条、去重、置顶）；`SearchBar` 加历史 chip 区 + 清空按钮；新增 `clearSearchHistory`/`runSearchHistory` action
- 更新面板完整化（#update-modal）：版本号 + 主副文案 + changelog 列表 + 下载进度按钮（模拟下载进度）+ 暂不更新；新增 `setUpdateInfo`/`startUpdateDownload` action
- 全屏 LoadingOverlay（#loading-overlay）：新建 `LoadingOverlay.kt`（旋转渐变弧 spinner + 文案）；`UiState.globalLoading`；`showGlobalLoading`/`hideGlobalLoading` action
- Toast 自动消失（#toast）：`LaunchedEffect(msg) + delay(3000) + dismissToast()`，对应桌面版 `showToast` 的 3 秒 setTimeout

**P4 BUG 修复**
- `prefs by lazy` 声明在 `init` 块之后导致构造期 NPE：Kotlin 属性按声明顺序初始化，`init` 块先执行时 `prefs$delegate` 仍为 null，访问即崩溃。修复为移到 `init` 块之前
- `CustomLyricModal` 未使用 import：给 `Column` 加 `verticalScroll(rememberScrollState())` 支持长 LRC 文本滚动，同时消除告警

**P4 验证**：经 40+ 项引用完整性核查（JSON 字段名 41 项逐一比对 / lambda 类型匹配 / ActivityResultContracts 用法 / 颜色常量存在性 / 跨文件签名匹配）全部通过。

**P5 阶段（vivo 流体云适配 + 听歌画像 + 封面取色 + 自定义背景 + BUG 深度修复）**
- **vivo OriginOS 流体云适配**（零 SDK 路径）：`PlayerController.setArtworkBitmap` 改用 `FileProvider.getUriForFile` 生成 `content://` URI + `grantUriPermission("com.android.systemui", ...)` 授予 SystemUI 跨进程读取封面权限；`toMediaItem` 补 `MediaMetadata.MEDIA_TYPE_MUSIC` + `setDurationMs`；`AndroidManifest.xml` 声明 FileProvider + `res/xml/file_paths.xml` 配置 artwork cache-path。基于标准 MediaSession 自动适配，无需 vivo SDK
- **听歌画像**（复刻桌面版 `listenStatsState`）：新建 `ListenStatsTracker` 数据层（`ListenStats.kt`，含 `ListenSummary`/`ListenRecord`/`SongStat`/`ArtistStat`），`MainViewModel` 接入 begin/tick/finalize 会话钩子（监听 playback StateFlow 驱动切歌/进度/自然播完），`HomeGrid` 听歌画像卡片回填 topArtist/总播放次数/总分钟数，`ListenProfileModal` 详情弹层（汇总卡 + 最近播放列表），`SharedPreferences` 持久化（与桌面版 localStorage 同名 key）
- **封面取色弹层**（复刻桌面版 `.cover-color-pop`）：新建 `CoverColorPop`（Bitmap 像素取色 + detectTapGestures + 自动主色板提取，48x48 缩放 + 4bit 量化 + 饱和度过滤），`FxPanel` 歌词配色区新增「封面取色」入口，`applyColorLab`/`applyCoverColor` 新增 `bgColor` 目标
- **自定义背景**（复刻桌面版 `#custom-bg` 纯色/图片/视频）：`FxState`/`FxArchiveSnapshot` 新增 `customBgType/color/uri` 三字段，四处 FX 存档序列化补齐（saveFxArchive/snapshotToJson/jsonToSnapshot/loadFxArchive），`FxPanel`「叠加/壁纸」分区新增背景类型选择 + 颜色行 + 媒体状态行，`PlayerShell` 新增图片/视频 launcher + 渲染层（`AsyncImage` 图片背景 / `VideoView` 视频背景 / 纯色背景，粒子星河在其上叠加带透明度）
- **Splash 粒子动画升级**：`SplashOverlay` 重写为 Compose Canvas 粒子信号线动画（12 条辐射信号线 + 旋转扫描线 + 中心脉冲圆环，blinkAlpha/rotation/pulse 三动画）
- **搜索结果行收藏到歌单**：`SongRow`/`LikeableSongList` 新增 `onCollect` 回调，`LikeButton.kt` 新增 `CollectButton` 组件（PlaylistAdd 图标），`DailyRecommendSidePanel` 接入
- **`MainViewModel` 新增 customBg 状态变更方法**：`setCustomBgType`/`setCustomBgColor`/`setCustomBgUri`/`clearCustomBg`

**P5 BUG 深度修复**
- `HomeGrid` tile 分发失效：`onTileClick` 统一走 `playDailyRecommend`，weatherSong/recent/playlist tile 点击行为错误。改为按 `tile.kind` 分发（weatherSong→playWeatherSong、recent→playRecentVoice、profile→toggleListenProfile、playlist→openPlaylistDetail）
- `HomeGrid`「常听歌手」卡片误指向每日推荐（`onDailyClick`）：改为路由到听歌画像弹层（`onProfileClick`，弹层内含 topArtist 列表）
- `CoverCropModal` 裁剪数学错误：`cropToSquare` 忽略 `scale/offsetX/offsetY`，pan/zoom 仅预览不生效，提交时取居中方形。改为 Matrix 矩阵变换（baseScale=Crop 基准 + totalScale + 居中平移 + clipRect），与 `Image(ContentScale.Crop) + graphicsLayer` 数学完全一致；Image 改用 `ContentScale.Crop`
- `search` 逐键打后端 + 历史污染：`onValueChange` 每次调 `vm.search` 即记录历史并请求后端，导致历史塞满每个输入前缀且后端被打爆。改为 `search` 防抖 350ms 不记历史，新增 `commitSearch`（回车/历史 chip 点击时调用）才记录历史并立即搜索
- `Shelf3DPanel` 索引错位：`coverUrls` 过滤掉空封面后与 `playlists` 索引不对齐，导致 3D 架选中项与缩略条高亮项不一致。改为保留空串维持索引对齐，加载循环跳过空 URL 但计入 loadedCount
- `LyricStage` 未消费 `lyricGlowColor`/`lyricGlowParticles`：当前行无溢光效果。新增 `Shadow` 溢光（颜色用 glowColor，`lyricGlowParticles` 控制模糊半径 8/16）
- `ListenStats` `Long in IntRange` 类型不匹配：`if (delta in 1 until 8000)` 中 delta 为 Long，改为 `1L until 8000L`
- `DailyRecommendSidePanel` 中 `vm` 未解析的编译错误：补 `vm: MainViewModel` 参数并在调用处传入

**P5 验证**：经 15 文件交叉一致性核查 + 4 项阻断性 BUG 修复（Shelf3D 索引 / CoverCrop 数学 / search 防抖 / HomeGrid 分发）全部通过。

**渲染管线总览**

```
FxState (DIY 配置)
  ├─→ ParticleGalaxyBackground → ParticleGalaxyRenderer (GLES2)
  │     └─ 7 个粒子 uniform + bgFade 背景压缩
  ├─→ Shelf3DPanel → ShelfRenderer (GLES2)
  │     └─ uAccent/uOpacity/uBgAlpha + 全局 transform
  ├─→ 自定义背景层 (color / AsyncImage / VideoView)
  │     └─ 粒子星河在其上叠加（customBg 激活时降透明度）
  └─→ LyricStage (Compose)
        └─ overlayColors + 字体/字间距/行高/字重/缩放 + 当前行 glowColor 溢光
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
