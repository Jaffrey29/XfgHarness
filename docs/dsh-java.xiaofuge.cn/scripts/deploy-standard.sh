#!/usr/bin/env bash
set -euo pipefail

HARNESS_CONTAINER="dsh-java-web"
MALL_CONTAINER="2d-weekend-mall"
HARNESS_IMAGE="registry.cn-hangzhou.aliyuncs.com/xfg-studio/deepseek-harness-java:0.1.6.4"
MALL_IMAGE="registry.cn-hangzhou.aliyuncs.com/fuzhengwei/2d-weekend-mall:0.1.1"
MALL_SERVICE_TOKEN="${MALL_SERVICE_TOKEN:-mall-internal-demo-token}"
HARNESS_URL="http://127.0.0.1:8090"
MALL_URL="http://127.0.0.1:18080"
PLUGIN_ID="mall-weekend-assistant"

command -v docker >/dev/null 2>&1 || { echo "未找到 docker，请先安装 Docker。" >&2; exit 1; }
command -v curl >/dev/null 2>&1 || { echo "未找到 curl，请先安装 curl。" >&2; exit 1; }

echo "删除旧容器..."
docker rm -f "$MALL_CONTAINER" "$HARNESS_CONTAINER" >/dev/null 2>&1 || true

echo "拉取镜像..."
docker pull "$HARNESS_IMAGE"
docker pull "$MALL_IMAGE"

echo "启动 Harness..."
docker run -d \
  --name "$HARNESS_CONTAINER" \
  --restart unless-stopped \
  --network host \
  -v dsh-web-data:/app/data \
  -v dsh-web-storage:/app/storage \
  -v dsh-web-plugins:/app/plugins \
  "$HARNESS_IMAGE" >/dev/null

echo "启动 Mall..."
docker run -d \
  --name "$MALL_CONTAINER" \
  --restart unless-stopped \
  --network host \
  -e MALL_ASSISTANT_HARNESS_BASE_URL="$HARNESS_URL" \
  -e MALL_SERVICE_TOKEN="$MALL_SERVICE_TOKEN" \
  "$MALL_IMAGE" >/dev/null

echo "等待 Harness 启动..."
READY=false
for _ in $(seq 1 60); do
  if curl -fsS "$HARNESS_URL/api/harness/plugins" >/dev/null 2>&1; then
    READY=true
    break
  fi
  sleep 2
done

if [[ "$READY" != true ]]; then
  echo "Harness 启动失败，请查看日志：" >&2
  echo "docker logs $HARNESS_CONTAINER" >&2
  exit 1
fi

echo "检查商城插件..."
PLUGIN_LIST="$(curl -fsS "$HARNESS_URL/api/harness/plugins")"
if grep -q "$PLUGIN_ID" <<<"$PLUGIN_LIST"; then
  echo "自动配置商城插件..."
  TOKEN_JSON="$(printf '%s' "$MALL_SERVICE_TOKEN" | sed 's/\\/\\\\/g; s/"/\\"/g')"
  curl -fsS -X POST \
    "$HARNESS_URL/api/harness/plugins/$PLUGIN_ID/config" \
    -H "Content-Type: application/json" \
    -d "{\"configs\":[{\"key\":\"mall.base-url\",\"value\":\"$MALL_URL\"},{\"key\":\"mall.service-token\",\"value\":\"$TOKEN_JSON\"}]}" \
    >/dev/null
  curl -fsS -X POST \
    "$HARNESS_URL/api/harness/plugins/$PLUGIN_ID/disable" \
    >/dev/null 2>&1 || true
  curl -fsS -X POST \
    "$HARNESS_URL/api/harness/plugins/activate" \
    -H "Content-Type: application/json" \
    -d "{\"pluginId\":\"$PLUGIN_ID\"}" \
    >/dev/null
else
  echo "商城插件尚未安装，请按页面提示下载 JAR 并在 Harness 插件管理中上传。"
fi

echo
echo "部署完成："
echo "Harness: $HARNESS_URL"
echo "Mall:    $MALL_URL"
echo
echo "商城插件 JAR："
echo "https://github.com/fuzhengwei/2d-weekend-mall/blob/main/docs/mall-agent-plugin-1.0.0-SNAPSHOT.jar"
echo
echo "打开 Harness -> 设置 -> 插件管理 -> 选择 JAR -> 安装并启用。"
echo "打开 Harness -> 设置 -> 模型设置 -> 添加模型，填写模型服务地址、模型名称和 API Key。"
echo "商城服务令牌：$MALL_SERVICE_TOKEN"
