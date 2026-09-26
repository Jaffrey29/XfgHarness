#!/bin/bash
# 常驻启动 DSH-Java standalone 服务（8090）
# 用法: ./scripts/start-standalone.sh
cd "$(dirname "$0")/.."
PID=$(lsof -nP -tiTCP:8090 -sTCP:LISTEN 2>/dev/null)
if [ -n "$PID" ]; then echo "8090 已被占用 (pid=$PID)，先停止"; kill -9 $PID; sleep 2; fi
nohup java -jar deepseek-harness-java-app/target/deepseek-harness-java-app.jar \
  --spring.profiles.active=standalone --server.port=8090 \
  > /tmp/dsh-java-standalone.log 2>&1 &
echo "已启动 pid=$! ，日志 /tmp/dsh-java-standalone.log"
