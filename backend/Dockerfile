# ====================================================================
#  Mineradio 后端镜像
#
#  把仓库根目录的 server.js + dj-analyzer.js + public/ 打包成可被
#  iOS / Android 移动端连接的 API 后端。原 server.js 零改动，仅通过
#  环境变量把 Windows 专属路径（D:\MineradioCache\beatmaps）重映射到容器内。
#
#  构建： docker build -t mineradio-backend .
#  运行： docker run -d --name mineradio-backend -p 3000:3000 \
#           -v mineradio-data:/data mineradio-backend
# ====================================================================
FROM node:20-alpine

# 网络工具（部分音乐源代理需要 ca-certificates；curl 仅用于健康检查）
RUN apk add --no-cache ca-certificates curl tini

WORKDIR /app

# 先拷依赖清单，利用 Docker 层缓存
COPY package.json package-lock.json* ./

# 只装运行时依赖（不装 electron / electron-builder 等桌面构建工具）
# NeteaseCloudMusicApi / mpg123-decoder / gsap 是运行依赖。
RUN npm install --omit=dev --no-audit --no-fund --ignore-scripts

# 拷贝后端与前端静态资源（public/ 由 server.js 静态托管）
COPY server.js dj-analyzer.js ./
COPY public ./public

# 容器内数据卷：cookie / QQ cookie / 节奏缓存
# 重写 Windows 专属默认路径到 /data，让 server.js 在 Linux 下正常运行
ENV NODE_ENV=production \
    HOST=0.0.0.0 \
    PORT=3000 \
    COOKIE_FILE=/data/.cookie \
    QQ_COOKIE_FILE=/data/.qq-cookie \
    MINERADIO_BEAT_CACHE_DIR=/data/beatmaps \
    MINERADIO_UPDATE_DIR=/data/updates \
    MINERADIO_UPDATE_DOWNLOAD_DIR=/data/updates/downloads

# 健康检查：server.js 的 /api/app/version 永远返回 JSON
HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
  CMD curl -fsS http://127.0.0.1:${PORT}/api/app/version || exit 1

VOLUME ["/data"]

EXPOSE 3000

ENTRYPOINT ["/sbin/tini", "--"]
CMD ["node", "server.js"]
