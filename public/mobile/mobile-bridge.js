/*!
 * Mineradio mobile bridge — polyfills window.desktopWindow on Capacitor (iOS / Android).
 *
 * 设计原则：
 *  - isDesktop 保持为 true，让桌面版完整 UI 与全部入口照常渲染（仅做移动端适配，不改 UI）。
 *  - 能在移动端原生实现的（窗口控制、登录、文件导入导出）走 Capacitor 插件。
 *  - 桌面专属能力（系统壁纸、系统级桌面歌词悬浮窗、全局快捷键、自更新安装包、重启）
 *    在移动端改为「应用内等效实现」或「诚实回退」，绝不删除入口或精简功能。
 *  - 在非 Capacitor 环境（Electron 桌面 / 普通浏览器）下完全 no-op，不影响原桌面版。
 *
 * 对应 desktop/preload.js 暴露的全部 22 个方法 + isDesktop 标志位。
 */
(function () {
  'use strict';

  // 已经被 Electron preload 注入，或已被本脚本注入过 —— 直接退出，绝不覆盖桌面版 bridge。
  if (window.desktopWindow && window.desktopWindow.__isMobileBridge !== true) return;
  if (window.desktopWindow && window.desktopWindow.__isMobileBridge === true) return;

  // 没有 Capacitor 运行时（Electron 桌面 / 普通网页）—— 不注入，让原版逻辑自行处理。
  var hasCapacitor = !!(window.Capacitor && (window.Capacitor.isNative || (window.Capacitor.Plugins && Object.keys(window.Capacitor.Plugins).length)));
  if (!hasCapacitor) return;

  var Capacitor = window.Capacitor;
  var Plugins = Capacitor.Plugins || {};
  var isNative = !!(Capacitor.isNative && Capacitor.isNative.platform && Capacitor.isNative.platform !== 'web');
  var platform = (Capacitor.getPlatform && Capacitor.getPlatform()) || (isNative ? 'unknown' : 'web');

  // ----------------------------------------------------------------
  //  事件订阅池
  // ----------------------------------------------------------------
  var windowStateListeners = new Set();
  var hotkeyListeners = new Set();
  var lyricsLockListeners = new Set();
  var lyricsEnabledListeners = new Set();

  function emit(set) {
    var args = Array.prototype.slice.call(arguments, 1);
    set.forEach(function (cb) { try { cb.apply(null, args); } catch (e) {} });
  }

  // ----------------------------------------------------------------
  //  窗口状态（移动端永远是全屏单窗口）
  // ----------------------------------------------------------------
  function currentWindowState() {
    return {
      isMaximized: false,
      isNativeFullScreen: true,
      isHtmlFullScreen: false,
      isWindowFullScreen: true,
      isFullScreen: true,
      isMinimized: false,
      isVisible: document.visibilityState !== 'hidden',
      isFocused: document.hasFocus(),
      isPrimaryDisplay: true,
      hasDisplayOnLeft: false,
      hasDisplayOnRight: false,
      displayBounds: null,
    };
  }

  // 可见性变化时推送状态，让前端 visibility 钩子与桌面版一致。
  document.addEventListener('visibilitychange', function () {
    emit(windowStateListeners, currentWindowState());
  });
  window.addEventListener('focus', function () { emit(windowStateListeners, currentWindowState()); });
  window.addEventListener('blur', function () { emit(windowStateListeners, currentWindowState()); });

  // ----------------------------------------------------------------
  //  状态栏 / 沉浸式
  // ----------------------------------------------------------------
  var immersiveHidden = false;
  function setImmersive(hidden) {
    immersiveHidden = hidden;
    try {
      var StatusBar = Plugins.StatusBar;
      if (!StatusBar) return;
      if (hidden) {
        if (StatusBar.setStyle) StatusBar.setStyle({ style: 'DARK' }).catch(function () {});
        if (StatusBar.hide) StatusBar.hide().catch(function () {});
      } else {
        if (StatusBar.show) StatusBar.show().catch(function () {});
        if (StatusBar.setStyle) StatusBar.setStyle({ style: 'DARK' }).catch(function () {});
        if (StatusBar.setBackgroundColor) StatusBar.setBackgroundColor({ color: '#050608' }).catch(function () {});
      }
    } catch (e) {}
  }

  // ----------------------------------------------------------------
  //  InAppBrowser 登录（网易云 / QQ 音乐）
  //  优先使用 cordova-plugin-inappbrowser（Capacitor 经 @capacitor-community/cordova 兼容）。
  //  不可用时返回未完成，让前端自动回退到二维码 / 手动 cookie 流程（已在 index.html 实现）。
  // ----------------------------------------------------------------
  function getCordovaInAppBrowser() {
    try {
      if (typeof window.cordova !== 'undefined' && window.cordova.InAppBrowser) return window.cordova.InAppBrowser;
    } catch (e) {}
    try {
      if (typeof window.InAppBrowser !== 'undefined') return window.InAppBrowser;
    } catch (e) {}
    return null;
  }

  function cookieStringFromPairs(pairs) {
    return pairs.filter(function (p) { return p[0] && p[1] != null && String(p[1]) !== ''; })
      .map(function (p) { return p[0] + '=' + p[1]; })
      .join('; ');
  }

  function extractCookiesFromDocument(inAppBrowserRef, cookieNames) {
    return new Promise(function (resolve) {
      if (!inAppBrowserRef || typeof inAppBrowserRef.executeScript !== 'function') {
        resolve('');
        return;
      }
      var script = "(function(){ try { return document.cookie || ''; } catch(e){ return ''; } })();";
      try {
        inAppBrowserRef.executeScript({ code: script }, function (raw) {
          var cookieText = '';
          if (Array.isArray(raw)) cookieText = raw.length ? String(raw[0] || '') : '';
          else if (typeof raw === 'string') cookieText = raw;
          // 保留所有可见 cookie，由后端做域名优先级与有效性校验（与桌面版一致）。
          resolve(String(cookieText || ''));
        });
      } catch (e) {
        resolve('');
      }
    });
  }

  function cookiesContainLogin(cookieText, requiredNames) {
    if (!cookieText) return false;
    var lower = cookieText.toLowerCase();
    return requiredNames.some(function (n) {
      return lower.indexOf(String(n).toLowerCase() + '=') !== -1;
    });
  }

  /**
   * 通用网页登录流程。
   * @param {object} opts
   *   url              登录页地址
   *   requiredCookies  判定登录成功的 cookie 名数组（任一命中即可）
   *   postEndpoint     抓到 cookie 后 POST 到后端的路径
   *   providerLabel    用于状态提示
   */
  function openWebLogin(opts) {
    return new Promise(function (resolve) {
      var IAB = getCordovaInAppBrowser();
      if (!IAB) {
        // 没有 InAppBrowser —— 诚实回退，让前端走二维码 / 手动 cookie。
        resolve({ ok: false, cancelled: true, message: '移动端未安装 InAppBrowser，已切换到二维码 / 手动 cookie 流程' });
        return;
      }

      var settled = false;
      var pollTimer = null;
      var ref = null;

      function finish(result) {
        if (settled) return;
        settled = true;
        if (pollTimer) clearInterval(pollTimer);
        try { if (ref && typeof ref.close === 'function') ref.close(); } catch (e) {}
        resolve(result);
      }

      try {
        ref = IAB.open(opts.url, '_blank', 'location=yes,hidden=no,clearcache=no,clearsessioncache=no,hidenavigationbuttons=yes,hideurlbar=yes,zoom=no,hardwareback=yes');
      } catch (e) {
        resolve({ ok: false, error: e && e.message || 'INAPPBROWSER_OPEN_FAILED' });
        return;
      }

      if (!ref) {
        // 退回系统浏览器（@capacitor/browser）—— 无法读取 cookie，仅辅助用户登录。
        var Browser = Plugins.Browser;
        if (Browser && Browser.open) {
          Browser.open({ url: opts.url, windowName: '_blank' }).catch(function () {});
        }
        resolve({ ok: false, cancelled: true, message: '已用系统浏览器打开登录页，请回到 App 使用二维码 / 手动 cookie 完成登录' });
        return;
      }

      // 轮询读取可见 cookie，命中关键 cookie 即收尾。
      pollTimer = setInterval(function () {
        extractCookiesFromDocument(ref, opts.requiredCookies).then(function (cookieText) {
          if (cookiesContainLogin(cookieText, opts.requiredCookies)) {
            finish({ ok: true, cookie: cookieText });
          }
        });
      }, 1400);

      // 关闭事件 —— 用户主动关闭视为取消。
      try {
        ref.addEventListener('exit', function () {
          // 关闭前最后抓一次 cookie，避免错过最后一次写入。
          extractCookiesFromDocument(ref, opts.requiredCookies).then(function (cookieText) {
            if (cookiesContainLogin(cookieText, opts.requiredCookies)) {
              finish({ ok: true, cookie: cookieText });
            } else {
              finish({ ok: false, cancelled: true, message: (opts.providerLabel || '登录') + '窗口已关闭' });
            }
          });
        });
      } catch (e) {}

      // 兜底超时（10 分钟），防止泄漏。
      setTimeout(function () { finish({ ok: false, cancelled: true, message: '登录超时已取消' }); }, 10 * 60 * 1000);
    });
  }

  // ----------------------------------------------------------------
  //  文件导出 / 导入（Capacitor Filesystem + Share）
  // ----------------------------------------------------------------
  function safeFileName(name) {
    return String(name || 'mineradio-export.json').replace(/[\\/:*?"<>|]+/g, '-');
  }

  async function exportJsonFile(payload) {
    try {
      var Filesystem = Plugins.Filesystem;
      var Share = Plugins.Share;
      if (!Filesystem) return { ok: false, error: 'FILESYSTEM_PLUGIN_MISSING' };

      var defaultName = safeFileName(payload && payload.defaultName || 'mineradio-export.json');
      if (!defaultName.toLowerCase().endsWith('.json')) defaultName += '.json';

      var text = typeof (payload && payload.text) === 'string'
        ? payload.text
        : JSON.stringify((payload && payload.data) || {}, null, 2);

      var dir = (Filesystem.Directory && Filesystem.Directory.Cache) || 'CACHE';
      var writeResult = await Filesystem.writeFile({
        path: defaultName,
        data: (typeof btoa !== 'undefined')
          ? btoa(unescape(encodeURIComponent(text)))
          : Buffer.from(text, 'utf8').toString('base64'),
        directory: dir,
        recursive: true,
      });

      // 优先用系统分享面板让用户保存到「文件」App / 下载目录。
      if (Share && Share.share) {
        try {
          await Share.share({
            title: defaultName,
            text: 'Mineradio 存档',
            url: writeResult.uri,
            dialogTitle: '导出 Mineradio 存档',
          });
          return { ok: true, filePath: writeResult.uri, shared: true };
        } catch (shareErr) {
          // 用户取消分享 —— 文件仍已写入缓存目录，返回路径。
          if (shareErr && /cancell?ed/i.test(String(shareErr.message || shareErr))) {
            return { ok: true, filePath: writeResult.uri, shared: false };
          }
          return { ok: true, filePath: writeResult.uri, shared: false };
        }
      }
      return { ok: true, filePath: writeResult.uri, shared: false };
    } catch (e) {
      return { ok: false, error: (e && e.message) || 'EXPORT_FAILED' };
    }
  }

  async function importJsonFile() {
    try {
      var Filesystem = Plugins.Filesystem;
      if (!Filesystem) return { ok: false, error: 'FILESYSTEM_PLUGIN_MISSING' };

      // 优先使用原生文件选择器（@capawesome/capacitor-file-picker 或 cordova-plugin-file-picker）。
      var Picker = Plugins.FilePicker || Plugins['FilePicker'];
      if (Picker && Picker.open) {
        try {
          var picked = await Picker.open({ types: ['application/json'] });
          var pickedPath = picked && (picked.path || picked.uri || picked.files && picked.files[0] && picked.files[0].path);
          if (!pickedPath) return { ok: false, canceled: true };
          var readPicked = await Filesystem.readFile({ path: pickedPath });
          var pickedText = typeof readPicked.data === 'string'
            ? readPicked.data
            : (typeof atob !== 'undefined' ? decodeURIComponent(escape(atob(readPicked.data))) : readPicked.data);
          return { ok: true, filePath: pickedPath, text: pickedText };
        } catch (pErr) {
          if (pErr && /cancell?ed/i.test(String(pErr.message || pErr))) return { ok: false, canceled: true };
        }
      }

      // 退回到隐藏 <input type=file> 选择 —— WebView 原生支持。
      var fileInputText = await new Promise(function (resolve, reject) {
        var input = document.createElement('input');
        input.type = 'file';
        input.accept = '.json,application/json';
        input.style.position = 'fixed';
        input.style.left = '-9999px';
        input.style.top = '0';
        input.style.opacity = '0';
        document.body.appendChild(input);
        input.addEventListener('change', function () {
          var f = input.files && input.files[0];
          if (!f) { document.body.removeChild(input); resolve(null); return; }
          var fr = new FileReader();
          fr.onload = function () { document.body.removeChild(input); resolve(String(fr.result || '')); };
          fr.onerror = function () { document.body.removeChild(input); reject(new Error('READ_FILE_FAILED')); };
          fr.readAsText(f);
        });
        input.addEventListener('cancel', function () { document.body.removeChild(input); resolve(null); });
        input.click();
      });

      if (fileInputText == null) return { ok: false, canceled: true };
      return { ok: true, filePath: '', text: fileInputText };
    } catch (e) {
      return { ok: false, error: (e && e.message) || 'IMPORT_FAILED' };
    }
  }

  // ----------------------------------------------------------------
  //  MobileOverlay —— 桌面歌词 / 壁纸在移动端的应用内等效实现。
  //  由 mobile-init.js 创建 iframe 层并注入 desktopOverlay bridge；
  //  本 bridge 仅作为 desktopWindow <-> overlay 的事件中转。
  // ----------------------------------------------------------------
  function getMobileOverlay() {
    return window.__MineradioMobileOverlay || null;
  }

  // ----------------------------------------------------------------
  //  对外 bridge（对应 desktop/preload.js 全部方法）
  // ----------------------------------------------------------------
  var bridge = {
    __isMobileBridge: true,
    __platform: platform,
    // 保持 true —— 让桌面版完整 UI（标题栏、壁纸入口、桌面歌词入口、全局快捷键设置、更新入口）
    // 全部照常渲染。仅在执行桌面专属动作时由下方方法做移动端适配。
    isDesktop: true,

    // ----- 窗口状态 -----
    onStateChange: function (callback) {
      if (typeof callback !== 'function') return function () {};
      windowStateListeners.add(callback);
      // 立即推一次，让前端初始化拿到移动端全屏状态。
      try { callback(currentWindowState()); } catch (e) {}
      return function () { windowStateListeners.delete(callback); };
    },
    getState: function () { return Promise.resolve(currentWindowState()); },

    // ----- 窗口控制（移动端映射为系统级动作）-----
    minimize: function () {
      // 移动端无法真正最小化 —— 退到后台。
      try {
        var App = Plugins.App;
        if (App && App.minimizeApp) return App.minimizeApp().catch(function () {});
      } catch (e) {}
      return Promise.resolve();
    },
    toggleMaximize: function () {
      // 移动端无「最大化」概念 —— 切换沉浸式状态栏。
      setImmersive(!immersiveHidden);
      emit(windowStateListeners, currentWindowState());
      return Promise.resolve();
    },
    toggleFullscreen: function () {
      setImmersive(!immersiveHidden);
      emit(windowStateListeners, currentWindowState());
      return Promise.resolve();
    },
    exitFullscreenWindowed: function () {
      // 移动端永远全屏，恢复显示状态栏即可。
      setImmersive(false);
      emit(windowStateListeners, currentWindowState());
      return Promise.resolve();
    },
    close: function () {
      try {
        var App = Plugins.App;
        if (App && App.exitApp) return App.exitApp().catch(function () {});
      } catch (e) {}
      return Promise.resolve();
    },

    // ----- 网易云登录 -----
    openNeteaseMusicLogin: function () {
      return openWebLogin({
        url: 'https://music.163.com/#/login',
        requiredCookies: ['MUSIC_U', 'MUSIC_A', '__csrf'],
        providerLabel: '网易云',
      });
    },
    clearNeteaseMusicLogin: function () {
      // 后端负责清 cookie；这里不再操作 InAppBrowser 会话。
      return Promise.resolve({ ok: true });
    },

    // ----- QQ 音乐登录 -----
    openQQMusicLogin: function () {
      return openWebLogin({
        url: 'https://y.qq.com/n/ryqq/profile',
        requiredCookies: ['qm_keyst', 'qqmusic_key', 'p_skey', 'skey', 'uin', 'qqmusic_uin', 'wxuin'],
        providerLabel: 'QQ 音乐',
      });
    },
    clearQQMusicLogin: function () {
      return Promise.resolve({ ok: true });
    },

    // ----- 自更新 / 重启（移动端经商店更新）-----
    openUpdateInstaller: function () {
      // 移动端无法运行安装包；返回明确错误，前端会显示「请前往应用商店更新」提示。
      return Promise.resolve({ ok: false, error: 'MOBILE_UPDATES_VIA_STORE' });
    },
    restartApp: function () {
      // 移动端无法自重启 —— 提示用户手动重开。
      return Promise.resolve({ ok: false, error: 'MOBILE_RESTART_UNSUPPORTED' });
    },

    // ----- 全局快捷键（移动端系统不支持应用级全局键捕获）-----
    // 保留入口与配置 UI，注册结果如实标注「系统保留」，让用户知道为何不可用，而不是删功能。
    configureGlobalHotkeys: function (bindings) {
      var list = Array.isArray(bindings) ? bindings : [];
      var results = list.map(function (item) {
        var action = item && String(item.action || '').trim();
        var accelerator = item && String(item.accelerator || '').trim();
        return {
          action: action,
          accelerator: accelerator,
          ok: false,
          conflict: {
            sourceName: '移动端系统',
            sourceIcon: 'info',
            reason: '移动端不支持应用级全局快捷键，请在桌面版使用',
          },
        };
      });
      return Promise.resolve({ ok: true, results: results });
    },
    onGlobalHotkey: function (callback) {
      if (typeof callback !== 'function') return function () {};
      hotkeyListeners.add(callback);
      return function () { hotkeyListeners.delete(callback); };
    },

    // ----- 文件导出 / 导入 -----
    exportJsonFile: function (payload) { return exportJsonFile(payload || {}); },
    importJsonFile: function () { return importJsonFile(); },

    // ----- 桌面歌词（移动端 → 应用内 iframe 悬浮层，UI 与功能完全保留）-----
    setDesktopLyricsEnabled: function (enabled, payload) {
      var ov = getMobileOverlay();
      if (!ov) return Promise.resolve({ ok: false, error: 'OVERLAY_NOT_READY' });
      return ov.setLyricsEnabled(!!enabled, payload || {}).then(function (ok) {
        if (ok) emit(lyricsEnabledListeners, { enabled: !!enabled });
        return { ok: !!ok };
      });
    },
    updateDesktopLyrics: function (payload) {
      var ov = getMobileOverlay();
      if (!ov) return Promise.resolve({ ok: false, error: 'OVERLAY_NOT_READY' });
      return ov.updateLyrics(payload || {}).then(function () { return { ok: true }; });
    },
    onDesktopLyricsLockState: function (callback) {
      if (typeof callback !== 'function') return function () {};
      lyricsLockListeners.add(callback);
      return function () { lyricsLockListeners.delete(callback); };
    },
    onDesktopLyricsEnabledState: function (callback) {
      if (typeof callback !== 'function') return function () {};
      lyricsEnabledListeners.add(callback);
      return function () { lyricsEnabledListeners.delete(callback); };
    },

    // ----- 壁纸（移动端 → 应用内全屏背景层，UI 与功能完全保留）-----
    setWallpaperMode: function (enabled, payload) {
      var ov = getMobileOverlay();
      if (!ov) return Promise.resolve({ ok: false, error: 'OVERLAY_NOT_READY' });
      return ov.setWallpaperEnabled(!!enabled, payload || {}).then(function () { return { ok: true }; });
    },
    updateWallpaperMode: function (payload) {
      var ov = getMobileOverlay();
      if (!ov) return Promise.resolve({ ok: false, error: 'OVERLAY_NOT_READY' });
      return ov.updateWallpaper(payload || {}).then(function () { return { ok: true }; });
    },
  };

  // 暴露给 overlay 层反向通知主窗口（锁屏状态等）。
  bridge.__notifyLyricsLockState = function (locked) {
    emit(lyricsLockListeners, { locked: !!locked });
  };
  bridge.__notifyLyricsEnabledState = function (enabled) {
    emit(lyricsEnabledListeners, { enabled: !!enabled });
  };

  window.desktopWindow = bridge;

  // 标记移动端环境，供 mobile-init.js / mobile.css 用。
  try {
    document.documentElement.classList.add('mineradio-mobile', 'mineradio-mobile-' + platform);
    document.documentElement.setAttribute('data-mineradio-platform', platform);
  } catch (e) {}
})();
