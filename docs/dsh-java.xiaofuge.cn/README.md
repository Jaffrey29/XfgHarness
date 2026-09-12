# DSH Java 官网

DSH Java 是 `deepseek-harness-java` 的官网静态站点，用于介绍 Java Agent Harness 运行时基座、实现机制、两个实际使用案例和最简便部署方式。站点使用原生 HTML/CSS/JS，不依赖前端构建链。作者：小傅哥。

## 目录结构

```text
dsh-java/
├── index.html              # 首页：能力、机制、案例、部署与社群
├── docs.html               # 使用教程：启动、控制台、插件与案例接入
├── assets/
│   ├── css/style.css       # 响应式视觉系统
│   ├── js/main.js          # 导航交互
│   └── images/             # 小鲸鱼 Logo 与控制台/案例截图
├── deploy/nginx.conf       # 站点 Nginx 配置
├── Dockerfile              # 静态站点容器
└── docker-compose.yml      # 官网 + Harness 一键组合部署
```

## 启动部署与体验

推荐使用部署仓库：<https://github.com/fuzhengwei/dsh-java-deploy>

标准部署包会启动 Harness、DSH MySQL 后台、2D Weekend Mall，并安装两个预置插件：

```bash
git clone https://github.com/fuzhengwei/dsh-java-deploy.git
cd dsh-java-deploy

cp .env.example .env
# 修改 MALL_SERVICE_TOKEN、DEEPSEEK_BASE_URL、DEEPSEEK_API_KEY

docker compose up -d
./scripts/install-plugins.sh
```

启动后访问：

```text
Harness：http://127.0.0.1:8090
DSH MySQL：http://127.0.0.1:8091
2D Weekend Mall：http://127.0.0.1:18080
```

验证插件：

```bash
curl http://127.0.0.1:8090/api/harness/plugins
```

如果只需要看 Harness 界面，可以先跑单容器：

```bash
docker run -d \
  --name dsh-java-web \
  -p 8090:8090 \
registry.cn-hangzhou.aliyuncs.com/xfg-studio/deepseek-harness-java:0.1.5
```

如需注入模型凭据：

```bash
docker rm -f dsh-java-web || true

docker run -d \
  --name dsh-java-web \
  -p 8090:8090 \
  -e DEEPSEEK_API_KEY='your-model-api-key' \
  registry.cn-hangzhou.aliyuncs.com/xfg-studio/deepseek-harness-java:0.1.5
```

## 启动官网

### 本地预览

直接用任意静态服务器打开仓库根目录即可。例如：

```bash
cd /Users/fuzhengwei/DevOps/dsh-java
python3 -m http.server 8092
```

访问：

```text
http://localhost:8092
```

### Docker 构建

```bash
docker build -t dsh-java-site .
docker run -d \
  --name dsh-java-site \
  -p 8091:80 \
  dsh-java-site
```

访问：

```text
http://localhost:8091
```

### 官网 + Harness 组合启动

```bash
cd /Users/fuzhengwei/DevOps/dsh-java

export DEEPSEEK_API_KEY='your-model-api-key'
docker compose up -d
```

访问地址：

```text
官网：http://localhost:8091
Harness：http://localhost:8090
```

Compose 会同时启动静态官网和 Harness 服务。`dsh-java-site` 声明了对 `dsh-java-web` 的启动顺序依赖。完整标准部署请优先使用 [dsh-java-deploy](https://github.com/fuzhengwei/dsh-java-deploy)。

## 部署到 dsh-java.xiaofuge.cn

推荐在生产主机上使用反向代理：

1. 构建并启动官网容器：
   ```bash
   docker build -t dsh-java-site .
   docker run -d --name dsh-java-site --restart unless-stopped -p 127.0.0.1:8091:80 dsh-java-site
   ```
2. 在宿主机反向代理中，将 `dsh-java.xiaofuge.cn` 的 HTTP/HTTPS 请求转发到 `127.0.0.1:8091`。
3. 如需代理控制台或 API，可另外将指定路径转发到 `127.0.0.1:8090`。
4. 生产环境建议启用 HTTPS、访问日志、限流和基础访问控制。
5. 不要把模型 API Key 提交进仓库或打包进公共镜像。

## 使用案例

### 2D Weekend Mall

一个 PC 端虚拟商城，包含商品搜索、购物车、下单、支付、订单查询和物流跟踪。页面右上角的 AI 客服通过 `mall-weekend-assistant` Java Native 插件接入 Harness，注册商品、订单和物流工具。

仓库地址：<https://github.com/fuzhengwei/2d-weekend-mall>

### dsh-java-mysql

MySQL 管理后台与 AI 数据库助手，支持连接管理、表浏览、只读查询、EXPLAIN 执行计划、性能快照和本地 SQL 风险审计。AI 链路全程只读，不提供写 SQL 工具。

仓库地址：<https://github.com/fuzhengwei/dsh-java-mysql>

## 源码与社群

`deepseek-harness-java` 源码维护在：

<https://gitcode.net/KnowledgePlanet/deepseek-harness-java>

目前源码通过加入社群提供，社群包含学习教程、插件开发实践、部署答疑和使用辅助。

## 插件开发与 AI Prompt

`docs.html` 的插件开发教程内置了 4 组提示词：

1. 需求拆解：生成插件边界、工具清单、风险等级和配置项。
2. 骨架生成：生成 Maven、plugin.yaml、SPI 声明和生命周期类。
3. 工具实现：生成输入校验、异常分类、HTTP 超时与日志脱敏。
4. 安装验证：生成打包、install/run 命令和 Agent 调试用例。

## 致谢

感谢并致敬 DeepSeek Harness 的开源设计思想。
