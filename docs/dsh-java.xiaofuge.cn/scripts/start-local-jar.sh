#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
JAR_PATH="${JAR_PATH:-${SCRIPT_DIR}/deepseek-harness-java.jar}"
DOWNLOAD_URL="https://drive.weixin.qq.com/s?k=ACMA4AfQABUnIBrQ39"

if [[ ! -f "$JAR_PATH" ]]; then
  echo "未找到本地 JAR：$JAR_PATH"
  echo "请先从下面地址下载 deepseek-harness-java.jar，并放到本脚本同目录："
  echo "$DOWNLOAD_URL"
  echo "然后再执行：./start-local-jar.sh"
  exit 1
fi

JAVA_BIN="java"
if [[ -x /usr/libexec/java_home ]]; then
  JAVA_HOME="$(/usr/libexec/java_home -v 17)"
  JAVA_BIN="$JAVA_HOME/bin/java"
fi

JAVA_VERSION="$("$JAVA_BIN" -version 2>&1 | head -1)"
if [[ "$JAVA_VERSION" != *'version "17'* ]]; then
  echo "请使用 JDK 17 启动；当前检测到：$JAVA_VERSION"
  exit 1
fi

cd "$SCRIPT_DIR"
exec "$JAVA_BIN" -jar "$JAR_PATH" --spring.profiles.active=standalone "$@"
