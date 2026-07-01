/*!
 * Mineradio mobile init — 移动端引导层。
 *
 * 职责：
 *  1. 读取 /mobile/config.json，决定后端地址（自建后端 / 局域网 / 本机 nodejs-mobile）。
 *  2. 在 Capacitor 本地壳（capacitor:// 或 https://localhost）下，把同源相对的
 *     /api/... 请求与 /api/audio /api/cover 媒体 src 透明改写到后端地址，让前端代码零改动。
 *  3. 提供移动端后端设置入口（仅移动端可见，不改动任何既有 UI）。
 *  4. 用应用内 iframe 层承载 desktop-lyrics.html / wallpaper.html，并注入 desktopOverlay bridge，
 *     让桌面歌词与壁纸在移动端以「应用内悬浮层 / 背景层」形式完整工作（不修改原始 overlay 文件）。
 *
 * 仅在 Capacitor 原生环境运行；Electron 桌面 / 普通浏览器下完全 no-op。
 */
(function () {
  'use strict';

  var hasCapacitor = !!(window.Capacitor && (window.Capacitor.isNative || (window.Capacitor.Plugins && Object.keys(window.Capacitor.Plugins).length)));
  if (!hasCapacitor) return;
  var isNative = !!(window.Capacitor.isNative && window.Capacitor.isNative.platform && window.Capacitor.isNative.platform !== 'web');
  if (!isNative) return; // web 预览模式不启用改写，避免开发时误改

  var LOG_PREFIX = '[Mineradio Mobile]';
  function log() { try { console.log.apply(console, [LOG_PREFIX].concat(Array.prototype.slice.call(arguments))); } catch (e) {} }
  function warn() { try { console.warn.apply(console, [LOG_PREFIX].concat(Array.prototype.slice.call(arguments))); } catch (e) {} }

  // ----------------------------------------------------------------
  //  1. 后端地址解析（localStorage 覆盖 > config.json > 空）
  // ----------------------------------------------------------------
  var BACKEND_URL = '';       // 例: 'https://mineradio.example.com' （无末尾斜杠）
  var ALLOW_HTTP = true;

  function normalizeBackend(url) {
    if (!url) return '';
    url = String(url).trim().replace(/\/+$/, '');
    if (!url) return '';
    if (!/^https?:\/\//i.test(url)) url = 'https://' + url;
    return url;
  }

  function isApiPath(u) {
    if (typeof u !== 'string') return false;
    if (u === '/api' || u.indexOf('/api/') === 0) return true;
    if (u.indexOf('/api?') === 0) return true;
    // 也覆盖完整 URL 中 path 以 /api 开头的情况（后端已是同源时不变）
    try {
      var p = new URL(u, location.href).pathname;
      return p === '/api' || p.indexOf('/api/') === 0;
    } catch (e) { return false; }
  }

  function rewriteUrl(u) {
    if (!BACKEND_URL) return u;
    if (typeof u !== 'string') return u;
    // 已经是绝对地址且指向后端 —— 不动
    if (u.toLowerCase().indexOf(BACKEND_URL.toLowerCase()) === 0) return u;
    // 跨域绝对地址（如 https://p1.music.126.net/... 封面/音频原始 URL）—— 不动，由后端 /api/audio /api/cover 代理
    if (/^https?:\/\//i.test(u) && u.toLowerCase().indexOf(location.origin.toLowerCase()) !== 0) return u;
    // 同源相对路径 /api/... → 后端绝对路径
    if (u.charAt(0) === '/' || u.toLowerCase().indexOf(location.origin.toLowerCase()) === 0) {
      try {
        var parsed = new URL(u, location.href);
        if (isApiPath(parsed.pathname)) {
          var target = BACKEND_URL + parsed.pathname + parsed.search + parsed.hash;
          return target;
        }
      } catch (e) {}
    }
    return u;
  }

  function applyFetchRewrite() {
    if (!BACKEND_URL) return;
    var origFetch = window.fetch;
    if (!origFetch || origFetch.__mineradioRewritten) return;
    var wrapped = function (input, init) {
      try {
        if (typeof input === 'string') input = rewriteUrl(input);
        else if (input && typeof input === 'object' && typeof input.url === 'string') input = new Request(rewriteUrl(input.url), input);
      } catch (e) {}
      return origFetch.call(this, input, init);
    };
    wrapped.__mineradioRewritten = true;
    window.fetch = wrapped;
    log('fetch 改写已启用 →', BACKEND_URL);
  }

  function applyMediaSrcRewrite() {
    if (!BACKEND_URL) return;

    function rewriteSrcAttribute(el, attr) {
      try {
        var v = el.getAttribute(attr);
        if (!v) return;
        var nv = rewriteUrl(v);
        if (nv !== v) el.setAttribute(attr, nv);
      } catch (e) {}
    }

    function scanNode(root) {
      if (!root || !root.querySelectorAll) return;
      try {
        root.querySelectorAll('audio[src],img[src],source[src],video[src]').forEach(function (el) {
          rewriteSrcAttribute(el, 'src');
        });
        root.querySelectorAll('audio,video').forEach(function (media) {
          if (media.src && typeof media.src === 'string') {
            var nv = rewriteUrl(media.src);
            if (nv !== media.src) media.src = nv;
          }
        });
      } catch (e) {}
    }

    // 原生 src setter 拦截 —— 覆盖 <audio>.src = '...' / <img>.src = '...'
    function patchSrcDescriptor(Ctor, label) {
      if (!Ctor || !Ctor.prototype) return;
      var desc = Object.getOwnPropertyDescriptor(Ctor.prototype, 'src');
      if (!desc || !desc.set || desc.__mineradioPatched) return;
      var origSet = desc.set;
      var origGet = desc.get;
      var patched = {
        configurable: true,
        enumerable: desc.enumerable !== false,
        get: function () { return origGet ? origGet.call(this) : ''; },
        set: function (v) {
          try { v = rewriteUrl(v); } catch (e) {}
          return origSet.call(this, v);
        },
      };
      patched.__mineradioPatched = true;
      Object.defineProperty(Ctor.prototype, 'src', patched);
      log(label + '.src setter 已改写');
    }
    try { patchSrcDescriptor(window.HTMLAudioElement, 'HTMLAudioElement'); } catch (e) {}
    try { patchSrcDescriptor(window.HTMLImageElement, 'HTMLImageElement'); } catch (e) {}
    try { patchSrcDescriptor(window.HTMLMediaElement, 'HTMLMediaElement'); } catch (e) {}
    try { patchSrcDescriptor(window.HTMLSourceElement, 'HTMLSourceElement'); } catch (e) {}

    // setAttribute 拦截
    var origSetAttribute = Element.prototype.setAttribute;
    if (origSetAttribute && !origSetAttribute.__mineradioPatched) {
      var wrappedSetAttr = function (name, value) {
        if (name && typeof name === 'string' && name.toLowerCase() === 'src' && typeof value === 'string') {
          try { value = rewriteUrl(value); } catch (e) {}
        }
        return origSetAttribute.call(this, name, value);
      };
      wrappedSetAttr.__mineradioPatched = true;
      Element.prototype.setAttribute = wrappedSetAttr;
    }

    // MutationObserver 兜底
    try {
      var mo = new MutationObserver(function (muts) {
        for (var i = 0; i < muts.length; i++) {
          var m = muts[i];
          if (m.type === 'attributes' && m.attributeName === 'src' && m.target) {
            rewriteSrcAttribute(m.target, 'src');
          } else if (m.type === 'childList' && m.addedNodes) {
            for (var j = 0; j < m.addedNodes.length; j++) scanNode(m.addedNodes[j]);
          }
        }
      });
      mo.observe(document.documentElement || document, { childList: true, subtree: true, attributes: true, attributeFilter: ['src'] });
    } catch (e) {}

    // 首次扫描
    if (document.readyState !== 'loading') scanNode(document);
    else document.addEventListener('DOMContentLoaded', function () { scanNode(document); });

    log('媒体 src 改写已启用');
  }

  // ----------------------------------------------------------------
  //  2. 后端设置入口（仅移动端可见，不改动既有 UI）
  // ----------------------------------------------------------------
  function ensureSettingsEntry() {
    if (document.getElementById('mineradio-mobile-settings-fab')) return;
    var fab = document.createElement('button');
    fab.id = 'mineradio-mobile-settings-fab';
    fab.type = 'button';
    fab.setAttribute('aria-label', 'Mineradio 移动端设置');
    fab.title = 'Mineradio 移动端设置';
    fab.textContent = '⚙';
    fab.addEventListener('click', openSettingsPanel);
    document.documentElement.appendChild(fab);

    var style = document.createElement('style');
    style.id = 'mineradio-mobile-settings-fab-style';
    style.textContent = [
      '#mineradio-mobile-settings-fab{position:fixed;z-index:99998;right:max(14px,env(safe-area-inset-right));bottom:max(18px,env(safe-area-inset-bottom));width:44px;height:44px;border-radius:50%;border:0;background:rgba(8,12,14,.62);color:#9cffdf;backdrop-filter:blur(14px) saturate(1.6);-webkit-backdrop-filter:blur(14px) saturate(1.6);box-shadow:0 8px 28px rgba(0,0,0,.46),inset 0 1px 0 rgba(255,255,255,.12);font-size:20px;line-height:1;display:flex;align-items:center;justify-content:center;cursor:pointer;opacity:.5;transition:opacity .2s,transform .2s}',
      '#mineradio-mobile-settings-fab:active{transform:scale(.92)}',
      '#mineradio-mobile-settings-fab.show{opacity:1}',
      '#mineradio-mobile-settings-panel{position:fixed;z-index:99999;inset:0;background:rgba(3,6,8,.72);backdrop-filter:blur(18px);-webkit-backdrop-filter:blur(18px);display:none;align-items:center;justify-content:center;padding:max(20px,env(safe-area-inset-top)) max(20px,env(safe-area-inset-right)) max(20px,env(safe-area-inset-bottom)) max(20px,env(safe-area-inset-left))}',
      '#mineradio-mobile-settings-panel.show{display:flex}',
      '#mineradio-mobile-settings-card{width:100%;max-width:440px;background:linear-gradient(112deg,rgba(72,74,76,.62),rgba(24,27,30,.70) 48%,rgba(8,12,14,.74));border:1px solid rgba(0,245,212,.30);border-radius:24px;box-shadow:0 24px 72px rgba(0,0,0,.46),0 0 0 1px rgba(0,245,212,.10);padding:22px 20px;color:#E8ECEF;font-family:inherit}',
      '#mineradio-mobile-settings-card h3{margin:0 0 14px;font-size:16px;font-weight:700;color:#fff;letter-spacing:.02em}',
      '#mineradio-mobile-settings-card label{display:block;font-size:12px;color:#8A9099;margin:12px 0 6px;letter-spacing:.04em;text-transform:uppercase}',
      '#mineradio-mobile-settings-card input[type=text]{width:100%;box-sizing:border-box;padding:12px 14px;border-radius:12px;border:1px solid rgba(255,255,255,.10);background:rgba(4,8,10,.5);color:#fff;font-size:14px;font-family:inherit;outline:none}',
      '#mineradio-mobile-settings-card input[type=text]:focus{border-color:rgba(0,245,212,.5)}',
      '#mineradio-mobile-settings-card .row{display:flex;gap:10px;margin-top:18px}',
      '#mineradio-mobile-settings-card button{flex:1;padding:11px 14px;border-radius:12px;border:0;font-family:inherit;font-size:13px;font-weight:600;cursor:pointer}',
      '#mineradio-mobile-settings-card .save{background:linear-gradient(112deg,#00F5D4,#00E0BE);color:#03160f}',
      '#mineradio-mobile-settings-card .cancel{background:rgba(255,255,255,.08);color:#D2D7DC}',
      '#mineradio-mobile-settings-card .hint{font-size:11px;color:#8A9099;line-height:1.5;margin-top:10px}',
    ].join('\n');
    document.head.appendChild(style);

    // 长按右上角区域唤出（避免遮挡既有 UI 交互）
    var revealTimer = null;
    document.addEventListener('pointerdown', function (e) {
      var inTopRight = e.clientY < 60 && e.clientX > window.innerWidth - 60;
      if (!inTopRight) return;
      revealTimer = setTimeout(function () { fab.classList.add('show'); }, 600);
    });
    document.addEventListener('pointerup', function () { if (revealTimer) clearTimeout(revealTimer); revealTimer = null; });
    document.addEventListener('pointermove', function () { if (revealTimer) clearTimeout(revealTimer); revealTimer = null; });

    // 后端未配置时主动显示一次提示
    if (!BACKEND_URL) {
      setTimeout(function () {
        fab.classList.add('show');
        if (!localStorage.getItem('mineradio-mobile-backend-hint-dismissed')) openSettingsPanel(true);
      }, 1200);
    }
  }

  function openSettingsPanel(firstRun) {
    var existing = document.getElementById('mineradio-mobile-settings-panel');
    if (existing) { existing.classList.add('show'); return; }
    var panel = document.createElement('div');
    panel.id = 'mineradio-mobile-settings-panel';
    panel.innerHTML =
      '<div id="mineradio-mobile-settings-card">' +
      '<h3>Mineradio 移动端设置</h3>' +
      '<label for="mineradio-mobile-backend-input">后端地址 (Backend URL)</label>' +
      '<input id="mineradio-mobile-backend-input" type="text" placeholder="https://你的后端地址" autocomplete="off" autocapitalize="off" autocorrect="off" spellcheck="false">' +
      '<div class="hint">留空则使用本机 nodejs-mobile（如已集成）。填写自建后端地址（如 https://mineradio.example.com），不要带末尾斜杠。所有 /api 请求与音频/封面代理会自动改写到此地址。</div>' +
      '<div class="row">' +
      '<button class="cancel" type="button">稍后</button>' +
      '<button class="save" type="button">保存并刷新</button>' +
      '</div>' +
      '</div>';
    document.body.appendChild(panel);
    var input = panel.querySelector('#mineradio-mobile-backend-input');
    if (input) input.value = BACKEND_URL || '';
    panel.querySelector('.cancel').addEventListener('click', function () {
      panel.classList.remove('show');
      if (firstRun) localStorage.setItem('mineradio-mobile-backend-hint-dismissed', '1');
    });
    panel.querySelector('.save').addEventListener('click', function () {
      var v = normalizeBackend(input.value);
      if (v) { localStorage.setItem('mineradio-mobile-backend-url', v); location.reload(); }
      else { localStorage.removeItem('mineradio-mobile-backend-url'); location.reload(); }
    });
    panel.classList.add('show');
  }

  // ----------------------------------------------------------------
  //  3. 应用内 overlay 层（桌面歌词 / 壁纸）
  //     用 iframe srcdoc 承载原始 desktop-lyrics.html / wallpaper.html，
  //     在 <head> 顶部注入 desktopOverlay bridge，原始 overlay 文件零改动。
  // ----------------------------------------------------------------
  var overlayCache = {};
  function fetchOverlayHtml(name) {
    if (overlayCache[name]) return Promise.resolve(overlayCache[name]);
    // 优先同源获取；后端模式下从后端取，本地壳下从本地资源取。
    var url = (BACKEND_URL ? BACKEND_URL : '') + '/' + name;
    return fetch(url).then(function (r) {
      if (!r.ok) throw new Error('OVERLAY_FETCH_' + r.status);
      return r.text();
    }).then(function (text) {
      overlayCache[name] = text;
      return text;
    });
  }

  function injectBridgeIntoHtml(html, bridgeScript) {
    // 在 <head ...> 之后、第一个内联脚本之前注入；若无 <head> 则在 <html> 之后注入。
    var inject = '<script>(function(){try{window.desktopOverlay=window.parent.__MineradioMobileOverlayBridge("' + bridgeScript + '");}catch(e){}})();<\/script>';
    if (/<head[^>]*>/i.test(html)) return html.replace(/<head[^>]*>/i, function (m) { return m + inject; });
    if (/<html[^>]*>/i.test(html)) return html.replace(/<html[^>]*>/i, function (m) { return m + inject; });
    return inject + html;
  }

  var MobileOverlay = {
    lyricsIframe: null,
    wallpaperIframe: null,
    lyricsState: { enabled: false },
    wallpaperState: { enabled: false },
    lyricsListeners: [],
    wallpaperListeners: [],

    createBridge: function (type) {
      var self = this;
      if (type === 'lyrics') {
        return {
          onLyricsState: function (cb) {
            if (typeof cb !== 'function') return function () {};
            self.lyricsListeners.push(cb);
            try { cb(self.lyricsState); } catch (e) {}
            return function () {
              var i = self.lyricsListeners.indexOf(cb);
              if (i >= 0) self.lyricsListeners.splice(i, 1);
            };
          },
          setLyricsDrag: function (d) { return Promise.resolve({ ok: true }); },
          setLyricsPointerCapture: function (a) {
            // 移动端应用内悬浮层始终可点击，无需穿透控制。
            return Promise.resolve({ ok: true });
          },
          setLyricsHotBounds: function (b) { return Promise.resolve({ ok: true }); },
          setLyricsLockState: function (locked) {
            self.lyricsState.clickThrough = !!locked;
            if (window.desktopWindow && window.desktopWindow.__notifyLyricsLockState) {
              window.desktopWindow.__notifyLyricsLockState(!!locked);
            }
            return Promise.resolve({ ok: true, locked: !!locked });
          },
          moveLyricsBy: function (dx, dy) {
            var f = self.lyricsIframe;
            if (f) {
              var cur = parseFloat(f.style.transform.match(/translate3d\(([-\d.]+)/) && f.style.transform.match(/translate3d\(([-\d.]+)/)[1]) || 0;
              var curY = parseFloat(f.style.transform.match(/,\s*([-\d.]+)/) && f.style.transform.match(/,\s*([-\d.]+)/)[1]) || 0;
              f.style.transform = 'translate3d(' + (cur + (Number(dx) || 0)) + 'px,' + (curY + (Number(dy) || 0)) + 'px,0)';
            }
            return Promise.resolve({ ok: true });
          },
          closeLyrics: function () { return self.setLyricsEnabled(false, {}); },
          onWallpaperState: function () { return function () {}; },
        };
      }
      // wallpaper
      return {
        onWallpaperState: function (cb) {
          if (typeof cb !== 'function') return function () {};
          self.wallpaperListeners.push(cb);
          try { cb(self.wallpaperState); } catch (e) {}
          return function () {
            var i = self.wallpaperListeners.indexOf(cb);
            if (i >= 0) self.wallpaperListeners.splice(i, 1);
          };
        },
      };
    },

    setLyricsEnabled: function (enabled, payload) {
      var self = this;
      self.lyricsState = Object.assign({}, self.lyricsState, payload || {}, { enabled: !!enabled });
      if (!enabled) {
        if (self.lyricsIframe) { self.lyricsIframe.remove(); self.lyricsIframe = null; }
        self.lyricsListeners = [];
        return Promise.resolve(true);
      }
      if (self.lyricsIframe) {
        self.lyricsListeners.forEach(function (cb) { try { cb(self.lyricsState); } catch (e) {} });
        return Promise.resolve(true);
      }
      return fetchOverlayHtml('desktop-lyrics.html').then(function (html) {
        var f = document.createElement('iframe');
        f.id = 'mineradio-mobile-lyrics-overlay';
        f.setAttribute('frameborder', '0');
        f.setAttribute('scrolling', 'no');
        f.setAttribute('allowtransparency', 'true');
        f.style.cssText = 'position:fixed;z-index:99990;left:0;right:0;bottom:0;width:100vw;height:38vh;max-height:340px;border:0;background:transparent;pointer-events:auto;transform:translate3d(0,0,0)';
        f.srcdoc = injectBridgeIntoHtml(html, 'lyrics');
        document.body.appendChild(f);
        self.lyricsIframe = f;
        return true;
      }).catch(function (e) {
        warn('桌面歌词 overlay 加载失败', e && e.message);
        return false;
      });
    },

    updateLyrics: function (payload) {
      var self = this;
      self.lyricsState = Object.assign({}, self.lyricsState, payload || {});
      self.lyricsListeners.forEach(function (cb) { try { cb(self.lyricsState); } catch (e) {} });
      return Promise.resolve(true);
    },

    setWallpaperEnabled: function (enabled, payload) {
      var self = this;
      self.wallpaperState = Object.assign({}, self.wallpaperState, payload || {}, { enabled: !!enabled });
      if (!enabled) {
        if (self.wallpaperIframe) { self.wallpaperIframe.remove(); self.wallpaperIframe = null; }
        self.wallpaperListeners = [];
        return Promise.resolve(true);
      }
      if (self.wallpaperIframe) {
        self.wallpaperListeners.forEach(function (cb) { try { cb(self.wallpaperState); } catch (e) {} });
        return Promise.resolve(true);
      }
      return fetchOverlayHtml('wallpaper.html').then(function (html) {
        var f = document.createElement('iframe');
        f.id = 'mineradio-mobile-wallpaper-overlay';
        f.setAttribute('frameborder', '0');
        f.setAttribute('scrolling', 'no');
        f.style.cssText = 'position:fixed;z-index:-1;left:0;top:0;width:100vw;height:100vh;border:0;background:#050608;pointer-events:none';
        f.srcdoc = injectBridgeIntoHtml(html, 'wallpaper');
        document.body.appendChild(f);
        self.wallpaperIframe = f;
        return true;
      }).catch(function (e) {
        warn('壁纸 overlay 加载失败', e && e.message);
        return false;
      });
    },

    updateWallpaper: function (payload) {
      var self = this;
      self.wallpaperState = Object.assign({}, self.wallpaperState, payload || {});
      self.wallpaperListeners.forEach(function (cb) { try { cb(self.wallpaperState); } catch (e) {} });
      return Promise.resolve(true);
    },
  };

  window.__MineradioMobileOverlay = MobileOverlay;
  window.__MineradioMobileOverlayBridge = function (type) { return MobileOverlay.createBridge(type); };

  // ----------------------------------------------------------------
  //  4. 视口 meta 修正（viewport-fit=cover，让安全区 env() 生效）
  //     不改 index.html 原始 meta，仅移动端动态补充 viewport-fit。
  // ----------------------------------------------------------------
  function ensureViewportFit() {
    var meta = document.querySelector('meta[name="viewport"]');
    if (!meta) {
      meta = document.createElement('meta');
      meta.name = 'viewport';
      (document.head || document.documentElement).insertBefore(meta, (document.head || document.documentElement).firstChild);
    }
    var content = meta.getAttribute('content') || 'width=device-width, initial-scale=1.0';
    if (!/viewport-fit\s*=/.test(content)) {
      content += ', viewport-fit=cover';
    }
    if (!/maximum-scale\s*=/.test(content)) {
      content += ', maximum-scale=1';
    }
    meta.setAttribute('content', content);
    // 阻止浏览器双击缩放（App 化手感）—— 仅在非交互元素上拦截，
    // 不影响封面裁剪等多指交互（.cover-crop-stage 等已在 CSS 中声明自己的 touch-action）。
    try {
      var lastTouchEnd = 0;
      document.addEventListener('touchend', function (e) {
        var t = e.target;
        var onInteractive = t && (t.closest('input,textarea,[contenteditable="true"],.cover-crop-stage,canvas,[data-allow-gesture]'));
        if (onInteractive) { lastTouchEnd = 0; return; }
        var now = Date.now();
        if (now - lastTouchEnd <= 320) { e.preventDefault(); }
        lastTouchEnd = now;
      }, { passive: false });
    } catch (e) {}
  }

  // ----------------------------------------------------------------
  //  5. 引导
  //  关键：网络改写必须在 index.html 内联主脚本发起首个 /api 请求之前完成。
  //  本脚本位于 <head>，readyState 此时为 'loading'，因此把「网络相关」拆出来
  //  立即同步执行，仅把「需要 document.body 的 UI」推迟到 DOMContentLoaded。
  // ----------------------------------------------------------------
  function bootstrapEarly() {
    ensureViewportFit();
    // 后端地址：localStorage > config.json
    var stored = null;
    try { stored = localStorage.getItem('mineradio-mobile-backend-url'); } catch (e) {}
    if (stored) {
      BACKEND_URL = normalizeBackend(stored);
    } else {
      // 同步读取 config.json（用 XHR 保证在 app 首个 API 调用前完成）
      try {
        var xhr = new XMLHttpRequest();
        xhr.open('GET', 'mobile/config.json?t=' + Date.now(), false);
        xhr.send(null);
        if (xhr.status === 200) {
          var cfg = JSON.parse(xhr.responseText || '{}');
          if (cfg.backendUrl) BACKEND_URL = normalizeBackend(cfg.backendUrl);
          if (typeof cfg.allowHttpBackend === 'boolean') ALLOW_HTTP = cfg.allowHttpBackend;
        }
      } catch (e) {
        // config.json 不可达时静默 —— 退回同源 / 本机模式
      }
    }

    if (BACKEND_URL) {
      if (!ALLOW_HTTP && /^http:\/\//i.test(BACKEND_URL)) {
        warn('当前配置禁止 http 后端，已忽略：', BACKEND_URL);
        BACKEND_URL = '';
      }
    }

    if (BACKEND_URL) {
      applyFetchRewrite();
      applyMediaSrcRewrite();
      log('后端地址 =', BACKEND_URL);
    } else {
      log('未配置后端地址 —— 将使用同源（本机 nodejs-mobile 或当前 origin）。如需连接自建后端，请点右下角设置按钮。');
    }
  }

  function bootstrapUI() {
    ensureSettingsEntry();
  }

  // 网络改写：立即同步执行（此时 <head> 还在解析，app 内联脚本尚未运行）。
  bootstrapEarly();
  // UI：推迟到 DOM 可用（FAB / 设置面板需要 document.body）。
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', bootstrapUI, { once: true });
  } else {
    bootstrapUI();
  }
})();
