# syntax=docker/dockerfile:1

# ---------- 依赖层 ----------
FROM node:22-alpine AS deps
WORKDIR /app

# 国内构建可换成镜像源：--registry=https://registry.npmmirror.com
COPY package.json package-lock.json* ./
RUN npm ci --no-audit --no-fund \
    || (echo "[deps] npm ci 失败，回退 npm install" && npm install --no-audit --no-fund)

# ---------- 构建层 ----------
FROM deps AS build
WORKDIR /app

# 构建期注入环境变量（Vite 会优先读取已存在的 process.env）
ARG VITE_USE_MOCK=true
ARG VITE_API_BASE_URL=/api
ARG VITE_APP_TITLE=云商 ERP
ARG VITE_APP_VERSION=0.1.0
ENV VITE_USE_MOCK=${VITE_USE_MOCK} \
    VITE_API_BASE_URL=${VITE_API_BASE_URL} \
    VITE_APP_TITLE=${VITE_APP_TITLE} \
    VITE_APP_VERSION=${VITE_APP_VERSION}

COPY . .
RUN npm run build

# ---------- 运行层 ----------
FROM nginx:1.27-alpine AS runtime
LABEL org.opencontainers.image.title="erp-web" \
      org.opencontainers.image.description="云商 ERP 前端（Vue3 + Element Plus + Tailwind CSS）"

ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

COPY nginx/default.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
    CMD wget -q --spider http://127.0.0.1/ || exit 1

STOPSIGNAL SIGQUIT
CMD ["nginx", "-g", "daemon off;"]
