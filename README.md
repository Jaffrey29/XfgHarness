# DeepSeek Harness · Java Edition

> 一个基于 **Java 17 + Spring Boot 3.3 + DDD 六边形架构** 的 Agent Harness（智能体运行时基座）——不只是 Chat UI 的后端封装，而是把**模型调用、工具执行、会话事件、任务审批、插件生命周期、终端与工作区、工作流与后台作业**组织成可扩展的一体化运行时，并自带可交互的 Web 控制台。

- **仓库**：<https://github.com/fuzhengwei/deepseek-harness-java>
- **端口**：`8090`（启动即开箱）
- **License**：见仓库根目录

---

## 目录

- [1. 项目介绍](#1-项目介绍)
  - [1.1 这是什么](#11-这是什么)
  - [1.2 适用场景](#12-适用场景)
  - [1.3 特性一览](#13-特性一览)
- [2. 快速体验](#2-快速体验)
  - [2.1 环境要求](#21-环境要求)
  - [2.2 构建](#22-构建)
  - [2.3 启动方式一：Standalone（H2，零外部依赖）](#23-启动方式一standaloneh2零外部依赖)
  - [2.4 启动方式二：MySQL（默认 profile）](#24-启动方式二mysql默认-profile)
  - [2.5 启动方式三：Docker Compose](#25-启动方式三docker-compose)
  - [2.6 打开 Web 控制台](#26-打开-web-控制台)
  - [2.7 用 curl 发第一条消息](#27-用-curl-发第一条消息)
- [3. 整体架构设计](#3-整体架构设计)
  - [3.1 总体架构图](#31-总体架构图)
  - [3.2 依赖方向与核心约束](#32-依赖方向与核心约束)
  - [3.3 Maven 模块划分](#33-maven-模块划分)
  - [3.4 请求处理链路（对话主线）](#34-请求处理链路对话主线)
  - [3.5 任务与审批链路（任务主线）](#35-任务与审批链路任务主线)
- [4. 领域设计](#4-领域设计)
  - [4.1 事件风暴全景](#41-事件风暴全景)
  - [4.2 限界上下文](#42-限界上下文)
  - [4.3 核心聚合](#43-核心聚合)
  - [4.4 领域事件机制（Session Event Log）](#44-领域事件机制session-event-log)
  - [4.5 策略树：功能如何串联](#45-策略树功能如何串联)
  - [4.6 端口-适配器对照](#46-端口-适配器对照)
- [5. 核心机制深入](#5-核心机制深入)
  - [5.1 ReactLoopAgent：ReAct 循环](#51-reactloopagentreact-循环)
  - [5.2 意图识别](#52-意图识别)
  - [5.3 会话压缩](#53-会话压缩)
  - [5.4 SSE 流式协议](#54-sse-流式协议)
- [6. 工具系统](#6-工具系统)
- [7. 插件与扩展](#7-插件与扩展)
  - [7.1 Java Native Plugin](#71-java-native-plugin)
  - [7.2 Node Bridge Plugin](#72-node-bridge-plugin)
  - [7.3 MCP 工具适配](#73-mcp-工具适配)
  - [7.4 插件生命周期](#74-插件生命周期)
- [8. REST API 概览](#8-rest-api-概览)
- [9. Web 控制台](#9-web-控制台)
- [10. 关键配置](#10-关键配置)
- [11. 持久化模型](#11-持久化模型)
- [12. 安全与执行边界](#12-安全与执行边界)
- [13. 生产化注意事项](#13-生产化注意事项)
- [14. 当前边界与已知隐患](#14-当前边界与已知隐患)
- [15. 文档索引](#15-文档索引)

---

## 1. 项目介绍

### 1.1 这是什么

`deepseek-harness-java` 是一个**单实例可运行的 Agent Harness**。它提供一个完整的运行时环境，让 LLM 驱动的智能体可以：

- 通过 **ReAct 循环**（Reason → Act → Observe）自主调用工具完成任务；
- 操作**文件系统、Shell、Web 搜索/抓取**等真实环境；
- 通过 **Java SPI 插件 / Node sidecar 插件 / MCP Server** 扩展能力；
- 接受**权限评估与人工审批**的治理约束；
- 将全过程以**事件溯源**方式落库，支持回放与审计。

项目自带一个**原生 JS 实现的 Web 控制台**（无前端构建链），打开浏览器即可对话、管理工作区、审批任务、查看插件与模型。

### 1.2 适用场景

| 场景 | 项目提供的能力 |
| --- | --- |
| 本地智能体工作台 | 浏览器 UI、SSE 对话、Markdown 渲染、会话与工作区管理 |
| Java 原生 Agent 运行时 | Spring Boot 装配、ReAct Loop、工具注册与执行、会话事件 |
| 企业内部扩展平台 | Java SPI 插件、Node sidecar 插件、MCP 工具适配、插件生命周期 API |
| 任务自动化系统 | 任务提交、队列、审批、目标、计划、待办、工作流、后台作业 |
| 可替换基础设施 | Domain 定义端口，Infrastructure 提供数据库、LLM、执行环境适配 |

> **定位说明**：它当前是**单实例 Agent 运行时**，不是开箱即用的分布式多租户 SaaS。生产化部署前仍需根据企业要求补充鉴权策略、审计、集群协调、可观测性与完整集成测试（见 [13. 生产化注意事项](#13-生产化注意事项)）。

### 1.3 特性一览

- 🤖 **Agent 对话**：阻塞式 `POST /api/agent/message` 与 SSE 流式 `POST /api/agent/stream`，支持取消、状态查询、`agentId` 会话复用
- 🔁 **ReAct 循环**：`ReactLoopAgent` 驱动模型输出 → 工具调用 → 结果回填 → 续步，单 turn 上限 50 步
- 🧰 **工具系统**：`fs_*` 文件工具、`shell_execute`、`web_search/web_fetch`、`ask_user_question`，统一 `ToolCallExecutor` + PRE/POST Hook
- 🧩 **双模式插件**：Java Native（进程内 `URLClassLoader` 隔离）与 Node Bridge（sidecar 进程 JSON-RPC），另支持 MCP stdio 工具
- 🛡️ **审批链路**：提交期权限矩阵评估 + 审批决策，高风险工具（Shell、写文件、插件、子进程）默认需人工审批
- 💾 **持久化**：会话/任务/目标/审批/模型设置落库；默认 MySQL，standalone profile 用 H2 文件库
- 🖥️ **Web 控制台**：原生 JS（约 3300 行）+ marked + DOMPurify + highlight.js，本地化加载无外网依赖
- 🐳 **Docker Compose**：一条命令拉起完整环境

---

## 2. 快速体验

### 2.1 环境要求

| 软件 | 版本 | 必需性 |
| --- | --- | --- |
| JDK | 17+ | 必需 |
| Maven | 3.9+ | 构建必需 |
| Node.js | 18+ | 仅使用 Node Bridge Plugin 时必需 |
| MySQL | 5.7+ | 仅默认（MySQL）profile 需要 |
| Docker | 24+ | 可选，用于 Compose 启动 |

### 2.2 构建

```bash
git clone git@github.com:fuzhengwei/deepseek-harness-java.git
cd deepseek-harness-java

# 完整构建（含测试）
mvn clean verify

# 快速打包（跳过测试）
mvn clean package -DskipTests
```

构建产物：

```text
deepseek-harness-java-app/target/deepseek-harness-java-app-1.0.0-SNAPSHOT.jar
```

### 2.3 启动方式一：Standalone（H2，零外部依赖）

适合本机快速体验和演示，无需安装 MySQL——使用内嵌 H2 文件库：

```bash
export DEEPSEEK_API_KEY='your-model-api-key'

java -jar deepseek-harness-java-app/target/deepseek-harness-java-app-1.0.0-SNAPSHOT.jar \
  --spring.profiles.active=standalone
```

H2 数据文件默认写入 `./data/deepseek-harness-java.mv.db`。

如需覆盖默认模型服务：

```bash
java -jar deepseek-harness-java-app/target/deepseek-harness-java-app-1.0.0-SNAPSHOT.jar \
  --spring.profiles.active=standalone \
  --harness.llm.deepseek.base-url='http://127.0.0.1:8777/v1' \
  --harness.llm.deepseek.api-key="$DEEPSEEK_API_KEY" \
  --harness.llm.deepseek.default-model='gpt-5.5'
```

### 2.4 启动方式二：MySQL（默认 profile）

默认 `application.yml` 面向外部 MySQL（默认连 `127.0.0.1:3306/deepseek_harness_java`）。通过环境变量覆盖连接信息：

```bash
export SPRING_DATASOURCE_URL='jdbc:mysql://127.0.0.1:3306/deepseek_harness_java?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true'
export SPRING_DATASOURCE_USERNAME='root'
export SPRING_DATASOURCE_PASSWORD='your-db-password'
export DEEPSEEK_API_KEY='your-model-api-key'

java -jar deepseek-harness-java-app/target/deepseek-harness-java-app-1.0.0-SNAPSHOT.jar
```

应用启动时自动执行 `deepseek-harness-java-app/src/main/resources/schema.sql`（幂等），确保 12 张核心表存在。

### 2.5 启动方式三：Docker Compose

```bash
export DEEPSEEK_API_KEY='your-model-api-key'
docker compose up --build -d
```

Compose 配置：暴露 `8090:8090`；默认 `standalone` Profile；挂载 `./data`、`./plugins`、`./.dsh`、`./workspaces`；通过 `DEEPSEEK_API_KEY` / `OPENAI_API_KEY` 注入模型凭据。

### 2.6 打开 Web 控制台

启动后访问：

```text
http://localhost:8090/
```

控制台支持：新建/切换/重命名/删除工作区、创建和恢复 Agent 会话、流式对话与工具卡片渲染、取消运行、模型设置管理、插件启停、待审批任务处理、历史会话回放。

### 2.7 用 curl 发第一条消息

阻塞式对话：

```bash
curl -X POST http://localhost:8090/api/agent/message \
  -H 'Content-Type: application/json' \
  -d '{
    "agentId": "local-agent",
    "message": "Hello!",
    "provider": "deepseek",
    "model": "gpt-5.5",
    "maxTokens": 8192,
    "cwd": "."
  }'
```

> `agentId` 和 `message` 必填；`provider`、`model`、`maxTokens`、`cwd` 省略后使用默认模型设置。

SSE 流式对话：

```bash
curl -N -X POST http://localhost:8090/api/agent/stream \
  -H 'Content-Type: application/json' \
  -d '{"agentId": "local-agent", "message": "介绍这个项目的架构"}'
```

如果配置了 `harness.auth.api-keys`，需额外携带 `-H 'X-API-Key: your-api-key'` 或 `-H 'Authorization: Bearer your-api-key'`。

---

## 3. 整体架构设计

### 3.1 总体架构图

整体为**六边形架构（端口-适配器）+ DDD 分层**，共 7 个 Maven 模块，依赖严格单向：

```mermaid
flowchart TB
    subgraph Clients["接入层"]
        Browser["浏览器 Web 控制台<br/>(原生 JS + SSE)"]
        Curl["curl / 第三方系统"]
    end

    subgraph Host["deepseek-harness-java-app（Spring Boot 启动器 :8090）"]
        direction TB

        subgraph Trigger["trigger 模块 · 接入适配"]
            Controllers["19 个 REST Controller<br/>(CQRS 拆 command/query)"]
            Auth["ApiKeyAuthInterceptor"]
        end

        subgraph API["api 模块 · 应用门面"]
            Facade["Facade + DTO<br/>(隔离外部协议与内部用例)"]
        end

        subgraph Case["case 模块 · 用例编排"]
            UseCase["AgentUseCase / TaskUseCase ..."]
            Strategy["策略树 = 责任链<br/>AbstractStrategyRouter"]
        end

        subgraph Domain["domain 模块 · 27 个限界上下文"]
            direction LR
            AgentCore["agent<br/>ReactLoopAgent"]
            SessionD["session<br/>HarnessSessionAggregate<br/>SessionLog(事件溯源)"]
            TaskD["task<br/>submission/permission<br/>approval/queue/execution"]
            ToolD["tool / guard / hooks<br/>ToolRegistry · ToolCallExecutor"]
            PluginD["plugin<br/>Registry / Bridge / Runtime"]
            RuntimeD["runtime / llm / credentials<br/>ModelSetting · ILlmRuntimePort"]
            OtherD["goal / plan / todo / workflow<br/>schedule / jobs / terminal / sandbox ..."]
        end

        subgraph Types["types 模块 · SPI 契约（不依赖 Spring）"]
            SPI["JavaHarnessPlugin · AbstractTool<br/>ToolDefinition"]
        end

        subgraph Infra["infrastructure 模块 · 端口实现"]
            DAO["MyBatis DAO<br/>(MySQL / H2)"]
            LLMGW["LLM Gateway<br/>DeepSeekAdapter · OpenAiCompatibleAdapter"]
            ExecGW["LocalShellExecutor · Node sidecar JSON-RPC"]
            MCP["StdioMcpClient + McpToolAdapter"]
        end
    end

    subgraph External["外部世界"]
        DB[("MySQL / H2")]
        LLM["LLM API<br/>(OpenAI 兼容网关)"]
        Node["Node.js sidecar"]
        Shell["/bin/sh"]
        McpSvr["MCP Servers (stdio)"]
    end

    subgraph Plugins["plugins/"]
        JavaJar["Java Native 插件 JAR<br/>(URLClassLoader 隔离)"]
        NodePlugin["Node 插件包<br/>(dsh-demo-plugin)"]
    end

    Browser -- "HTTP / SSE" --> Controllers
    Curl -- "REST" --> Controllers
    Controllers --> Auth --> Facade
    Controllers --> UseCase
    UseCase --> Strategy --> Domain
    Facade --> Domain
    Infra -.实现端口.-> Domain
    Domain --> Types
    Plugins --> Types
    DAO --> DB
    LLMGW --> LLM
    ExecGW --> Node
    ExecGW --> Shell
    MCP --> McpSvr
    PluginD -.加载.-> Plugins
```

> 完整架构图（draw.io 源文件）见 [`docs/architecture.drawio`](docs/architecture.drawio)，可用 draw.io / diagrams.net 打开编辑。

### 3.2 依赖方向与核心约束

```text
App → Trigger → API/Case → Domain ← Infrastructure
Domain → Types(SPI) ← Plugins
```

| 层 | 约束 |
| --- | --- |
| `Trigger` | 不写业务规则，只负责协议、认证、参数绑定和路由 |
| `API` | 暴露应用层 Facade 和 DTO，隔离外部协议与内部用例 |
| `Case` | 编排跨领域流程，不直接依赖具体数据库、HTTP Client 或进程实现 |
| `Domain` | 持有实体、值对象、领域服务、**端口（Port）**和仓库接口 |
| `Infrastructure` | 实现 Domain 端口，负责 MySQL/H2、LLM、Node、Shell、MCP 等外部依赖 |
| `Types` | 插件与工具的轻量 SPI 契约包，**不依赖 Spring** |

### 3.3 Maven 模块划分

| 模块 | 职责 |
| --- | --- |
| `deepseek-harness-java-app` | Spring Boot 启动器、静态 UI、Profile 配置、预置插件加载 |
| `deepseek-harness-java-trigger` | 19 个 REST Controller、API Key 拦截器、Web MVC 配置 |
| `deepseek-harness-java-api` | 应用层 Facade、Command/Query DTO、响应对象 |
| `deepseek-harness-java-case` | Agent、Session、Task、Plugin 等用例编排（策略树） |
| `deepseek-harness-java-domain` | 27 个限界上下文、实体、值对象、端口与领域服务 |
| `deepseek-harness-java-infrastructure` | Repository、DAO、Gateway、协议和执行环境适配 |
| `deepseek-harness-java-types` | `JavaHarnessPlugin`、`AbstractTool`、`ToolDefinition` 等 SPI 契约 |
| `plugins/*` | 示例插件（Java JAR + Node demo）、插件 Archetype |
| `docs/` | 架构图、领域设计详解、插件开发指南 |

### 3.4 请求处理链路（对话主线）

以 `POST /api/agent/stream` 为例，一条消息穿过系统的完整路径：

```text
浏览器 UI → AgentController → AgentUseCase
  → AgentResolveNode   按 agentId 复用/创建 Agent，解析模型、Token、工作目录
  → AgentIntentNode    正则规则分类 CHAT / CODE_QUESTION / TASK_EXECUTION / CLARIFICATION
  → AgentDispatchNode  构造用户消息，agent.send(userMessage, NEXT_TURN, true)
  → AgentCollectNode   等待空闲，投影 User/Assistant/ToolCall/ToolResult 消息，按 callId 合并
  → ReactLoopAgent     LLM 流式 → append AssistantChunk → ToolCallExecutor → append ToolCall/ToolResult
  → SSE 推送 meta / chunk / step_break / done
```

### 3.5 任务与审批链路（任务主线）

```text
POST /api/harness/tasks/submit
  → SubmissionRootNode → ProfileResolutionNode → PermissionCheckNode → ToolResolutionNode → EnqueueNode
      │                                        │
      │                                        ├─ PermissionPolicyService.assess(tools)   权限矩阵评估
      │                                        └─ ApprovalPolicyService.decide(profile, assessment)
      │
      ├─ 免审批 → executeSession（RUNNING）
      └─ 需审批 → PENDING_APPROVAL
                    │
POST /api/harness/approvals/{sessionId}/approve
                    └─→ 状态 QUEUED 落库 → HarnessExecutionService.executeSession → RUNNING → COMPLETED/FAILED
```

任务状态机：`CREATED → PENDING_APPROVAL → QUEUED → RUNNING → COMPLETED/FAILED`。执行入口对 `PENDING_APPROVAL` 状态直接抛异常兜底，防止绕过审批。

---

## 4. 领域设计

`domain` 模块共 27 个一级子包，遵循事件风暴（Event Storming）建模，六色图例贯穿下文。

### 4.1 事件风暴全景

```mermaid
flowchart LR
    subgraph SESSION["会话领域"]
        direction TB
        s_obj["HarnessSessionAggregate"]:::obj
        s_cmd1["POST /api/agent/stream 发送消息"]:::cmd
        s_evt1["UserMessage 已写入"]:::evt
        s_pol["意图识别策略（CHAT / CODE / TASK）"]:::policy
        s_evt2["TurnStart 开启"]:::evt
        s_cmd2["取消运行"]:::cmd
        s_evt3["TurnEnd / SessionEndSeed"]:::evt
        s_cmd1 -.-> s_evt1
        s_pol -.-> s_evt2
        s_cmd2 -.-> s_evt3
    end

    subgraph AGENT["Agent 领域"]
        direction TB
        a_obj["ReactLoopAgent"]:::obj
        a_cmd1["LLM 流式生成"]:::cmd
        a_evt1["AssistantChunk 追加"]:::evt
        a_cmd2["发起工具调用"]:::cmd
        a_evt2["ToolCall 已记录"]:::evt
        a_pol["上下文压缩策略（保留 1/4）"]:::policy
        a_ext["LLM 网关（DeepSeek / OpenAI）"]:::ext
        a_rm["SSE meta / chunk / step_break / done"]:::read
        a_cmd1 -.-> a_evt1
        a_cmd2 -.-> a_evt2
    end

    subgraph TOOL["工具领域"]
        direction TB
        t_obj["ToolCallExecutor / ToolRegistry"]:::obj
        t_cmd["执行工具"]:::cmd
        t_pol["超时与重复调用守卫"]:::policy
        t_evt["ToolResult 已回填"]:::evt
        t_ext1["本地 Shell（/bin/sh）"]:::ext
        t_ext2["MCP Server（stdio）"]:::ext
        t_ext3["文件系统 / Web"]:::ext
        t_rm["工具卡片（running / success）"]:::read
        t_pol -.-> t_evt
    end

    subgraph PLUGIN["插件领域"]
        direction TB
        p_obj["HarnessPluginEntity"]:::obj
        p_cmd["安装 / 激活 / 运行"]:::cmd
        p_pol["Bridge 识别（Java / Node / MCP）"]:::policy
        p_evt["插件工具注册完成"]:::evt
        p_ext["Node sidecar（JSON-RPC）"]:::ext
        p_rm["插件状态面板"]:::read
        p_pol -.-> p_evt
    end

    subgraph TASK["任务领域"]
        direction TB
        k_obj["HarnessTaskEntity"]:::obj
        k_cmd1["POST /api/harness/tasks/submit"]:::cmd
        k_evt1["任务已创建 CREATED"]:::evt
        k_pol["权限矩阵 + 审批决策"]:::policy
        k_evt2["任务已入队 QUEUED"]:::evt
        k_cmd2["执行会话"]:::cmd
        k_cmd1 -.-> k_evt1
        k_pol -.-> k_evt2
    end

    subgraph APPROVAL["审批领域"]
        direction TB
        v_obj["ApprovalDecisionVO"]:::obj
        v_evt1["待审批 PENDING_APPROVAL"]:::evt
        v_cmd["POST /approvals/{id}/approve"]:::cmd
        v_evt2["审批通过 → 恢复执行"]:::evt
        v_rm["待审批列表"]:::read
        v_cmd -.-> v_evt2
    end

    subgraph EXEC["执行领域"]
        direction TB
        e_obj["RuntimeExecutionPlanVO"]:::obj
        e_cmd["HarnessExecutionService.executeSession"]:::cmd
        e_evt1["RUNNING"]:::evt
        e_evt2["COMPLETED / FAILED"]:::evt
        e_rm["控制台会话回放"]:::read
        e_cmd -.-> e_evt1 -.-> e_evt2
    end

    subgraph RUNTIME["运行时领域"]
        direction TB
        r_obj["ModelSetting / Profile"]:::obj
        r_cmd["模型发现 / 保存设置"]:::cmd
        r_evt["工具目录绑定更新"]:::evt
        r_ext["MySQL 持久化"]:::ext
        r_rm["运行时模型目录"]:::read
        r_cmd -.-> r_evt
    end

    s_evt1 == 驱动 ReAct 循环 ==> a_cmd1
    a_cmd2 ==> t_cmd
    p_evt == 注册进 ToolRegistry ==> t_obj
    k_evt2 ==> v_evt1
    v_evt2 ==> e_cmd
    e_evt1 == 复用 Agent 链路 ==> a_cmd1
    r_ext -. 事件落库供回放 .-> e_rm
    r_obj -. 模型与工具目录支撑 .-> a_obj

    classDef cmd fill:#B5D4F4,stroke:#85B7EB,color:#0C447C
    classDef evt fill:#FAC775,stroke:#EF9F27,color:#633806
    classDef ext fill:#F4C0D1,stroke:#ED93B1,color:#72243E
    classDef policy fill:#D4537E,stroke:#993556,color:#ffffff
    classDef read fill:#9FE1CB,stroke:#5DCAA5,color:#085041
    classDef obj fill:#EF9F27,stroke:#BA7517,color:#412402

    style SESSION stroke-dasharray:5 4
    style AGENT stroke-dasharray:5 4
    style TOOL stroke-dasharray:5 4
    style PLUGIN stroke-dasharray:5 4
    style TASK stroke-dasharray:5 4
    style APPROVAL stroke-dasharray:5 4
    style EXEC stroke-dasharray:5 4
    style RUNTIME stroke-dasharray:5 4
```

两条业务主线：**对话主线**（会话 → Agent ReAct 循环 → 工具执行 → 插件扩展）与**任务主线**（任务提交 → 权限/审批 → 排队 → 执行，执行阶段复用 Agent 链路）；运行时领域横向提供模型 Profile 与工具目录支撑，过程事件落库供控制台回放。

### 4.2 限界上下文

| 能力面 | Bounded Contexts | 核心对象 |
| --- | --- | --- |
| Agent 执行核心 | `agent`, `llm`, `session` | `ReactLoopAgent`、`Phase`（Idle/Running/Maintenance）、`ILlmRuntimePort`、`HarnessSessionAggregate`、`SessionLog` |
| 任务与目标 | `task`, `goal`, `plan`, `todo`, `workflow`, `schedule`, `jobs` | `HarnessTaskEntity`、`GoalAggregate`、`WorkflowRun` |
| 工具与插件 | `tool`, `plugin`, `typert`, `sdk` | `ToolRegistry`、`ToolCallExecutor`、`RuntimeApprovalGate` |
| 运行时与环境 | `runtime`, `credentials`, `coderuntime`, `terminal`, `lsp`, `sandbox`, `e2b`, `acp` | `ModelSetting`、`Profile`、各类端口 |
| 治理与存储 | `guard`, `hooks`, `storage` | 超时策略、Hook 引擎、KV 存储 |
| 共享与技能 | `shared`, `skill` | `HarnessStatusEnumVO` 等跨域枚举 |

`task` 域按职责拆为五个子域：

| 子域 | 职责 | 关键对象 |
| --- | --- | --- |
| `submission` | 提交参数校验与过滤 | `TaskSubmissionVO` |
| `permission` | 权限矩阵评估 | `PermissionAssessmentVO`、`IPermissionMatrixPort` |
| `approval` | 审批决策与命令 | `ApprovalDecisionVO`、`ApprovalPolicyService`、`ApprovalCommandService` |
| `queue` | 任务队列持久化 | `IHarnessTaskQueueRepository` |
| `execution` | 会话执行 | `HarnessExecutionService`、`IModelExecutionPort` |

`plugin` 域拆为三条边界：**Registry**（`PluginRegistryService`，安装/元数据/启停）、**Bridge**（`PluginBridgeService`，识别 `JAVA_NATIVE`/`DSH_NODE_BRIDGE`/`CODEX`/`CORDIS` 并生成绑定计划）、**Runtime**（`PluginRuntimeService`，启动/停止/查询实例）。

### 4.3 核心聚合

- **Agent**：聚合根服务 `ReactLoopAgent`，持有 `SessionLog`、`Inbox`、`ILlmRuntimePort`、`ToolRegistry`、`ToolCallExecutor`、`SystemPromptAssembler`。单 turn 上限 50 步，max_tokens 受控续写最多 4 次，工具调用后经 `midTurnContinuation` 标志跳过 inbox 检查直接续步；取消用 `AtomicBoolean` + 100ms 轮询。
- **会话**：聚合根 `HarnessSessionAggregate`；`SessionLog` 为内存追加式 WAL，`SurfaceProjector` 将事件投影为 `SurfaceOp`（Append/Replace）；`BasicCompactionEngine` 保留最近 1/4 事件，其余由 LLM 总结。
- **任务**：`HarnessTaskEntity`，状态机 `CREATED → PENDING_APPROVAL → QUEUED → RUNNING → COMPLETED/FAILED`。
- **目标**：`GoalAggregate`（`GoalAction`、`GoalSnapshotVO`、`GoalStatus`），`IGoalRepository` 持久化。

### 4.4 领域事件机制（Session Event Log）

无通用 `DomainEvent` 基类，**事件机制即会话事件日志的事件溯源**。`SessionEvent` 为 sealed 接口：

| 类别 | 事件 |
| --- | --- |
| 轮/步边界 | `TurnStart`、`TurnEnd(reason)`、`StepStart`、`StepEnd` |
| 消息 | `UserMessage`、`AssistantChunk`、`AssistantMessage` |
| 工具 | `ToolCall(callId, name, arguments)`、`ToolResult(callId, ...)` |
| 其他 | `TodoWrite`、`RequestHeader`、`RequestContext`、`SessionEndSeed`、`PlanModeChange`、`AgentInboxSpliced` |

流式对话的 SSE 事件与领域事件对应：`meta`（起始帧）、`chunk`（AssistantChunk 文本增量）、`step_break`（工具调用边界，前端据此插入工具卡片并重置消息气泡）、`done`（终态 + 全量 messages 含 tool 角色，供前端重建工具卡片状态）、`error`。

### 4.5 策略树：功能如何串联

跨领域编排在 `case` 模块完成，统一用 `AbstractStrategyRouter` 责任链（`doApply` 处理当前节点，`getNext` 返回后继）。

- **Agent 消息链**：`AgentResolveNode → AgentIntentNode → AgentDispatchNode → AgentCollectNode`（见 [3.4](#34-请求处理链路对话主线)）。
- **任务提交链**：`SubmissionRootNode → ProfileResolutionNode → PermissionCheckNode → ToolResolutionNode → EnqueueNode`（见 [3.5](#35-任务与审批链路任务主线)）。
- **审批恢复**：`approve(sessionId)` → 状态 QUEUED 落库 → `HarnessExecutionService.executeSession` → RUNNING → COMPLETED/FAILED。
- **插件生命周期**：`install`（落 `HarnessPluginEntity`）→ `activate`（Bridge 识别 + recordBinding）→ `run`（`PluginRuntimeService.start`）。插件工具以 `plugin__<pluginId>__<toolName>` 注册进 `ToolRegistry`，之后与内置工具走同一套 Hook/守卫/事件回填链路。
- **工作流**：`IWorkflowService.start` → `IWorkflowEnginePort`（`LocalWorkflowEnginePort`）→ `WorkflowRun`（CompletableFuture），流式复用 `ReactLoopAgent` 的 deltaSink/toolCallSink 推 SSE。

### 4.6 端口-适配器对照

Domain 定义端口，Infrastructure 实现。关键端口：

| Domain 端口 | Infrastructure 实现 |
| --- | --- |
| `ILlmRuntimePort` | `InMemoryLlmRuntimePort`（流式生成，重试在此层） |
| `IModelExecutionPort` | `ModelExecutionPort` |
| `IModelProviderPort` | `ModelProviderPort` |
| `IRuntimeApprovalBroker` | `RuntimeApprovalBroker` |
| `IPermissionMatrixPort` / `IApprovalPolicyPort` | `PermissionMatrixPort` / `ApprovalPolicyPort` |
| `IToolCatalogPort` | `ToolCatalogPort` |
| `IPluginArtifactInstallerPort` / `IPluginRuntimeBridgePort` / `IPluginProcessPort` | `PluginArtifactInstallerPort` / `PluginRuntimeBridgePort` / `PluginProcessPort` |
| `IHarnessSessionRepository` / `ISessionEventStore` / `IHarnessTaskQueueRepository` / `IGoalRepository` | MyBatis Repository 对应实现 |
| `IWorkflowEnginePort` | `LocalWorkflowEnginePort` |
| `AgentFactoryPort` | `AgentFactory`（组合根：装配 `ReactLoopAgent` + `ToolCallExecutor`） |

> 更完整的领域说明（事件清单、全部端口、子域细节）见 [`docs/domain-design.md`](docs/domain-design.md)。

---

## 5. 核心机制深入

### 5.1 ReactLoopAgent：ReAct 循环

- **turn 循环**：每个 turn 最多 50 步；每步 = 一次 LLM 流式生成 + 可选的工具调用。
- **max_tokens 续写**：输出被截断时受控续写最多 4 次。
- **工具续步**：工具执行后经 `midTurnContinuation` 标志跳过 inbox 空检查直接续步，保证工具结果一定回到模型上下文。
- **Phase 状态机**：`Idle / Running / Maintenance`；取消用 `AtomicBoolean` + 100ms 轮询，协作式中断。
- **超时**：LLM HTTP 总超时 120s；工具超时用 `orTimeout` 协作式取消。

### 5.2 意图识别

`AgentIntentNode.classify()` 用**纯正则规则**分类（无额外模型调用）：

| 意图 | 说明 | 处理方式 |
| --- | --- | --- |
| `CHAT` | 闲聊 | 可经 `[no-tools]` 前缀跳过工具，直接对话 |
| `CODE_QUESTION` | 代码问答 | 进入工具路径 |
| `TASK_EXECUTION` | 任务执行 | 注入"先调工具"指令后进入工具路径 |
| `CLARIFICATION` | 澄清 | 可跳过工具 |

### 5.3 会话压缩

`SessionLog` 是内存追加式 WAL；`SurfaceProjector` 把事件投影为 `SurfaceOp`（Append/Replace）供 UI 渲染。上下文过长时 `BasicCompactionEngine` 启动压缩：**保留最近 1/4 事件原文，其余由 LLM 总结**为摘要事件，控制 token 占用（context-window 默认 128000）。

### 5.4 SSE 流式协议

`POST /api/agent/stream` 的事件序列：

| 事件 | 含义 |
| --- | --- |
| `meta` | 起始帧（会话、模型、时间信息） |
| `chunk` | 文本增量（对应 `AssistantChunk`） |
| `step_break` | 工具调用边界：携带 `toolName/callId/args/status`，前端据此完成当前气泡、插入工具卡片、开新气泡 |
| `done` | 终态 + 全量 messages（含 `tool` 角色），前端重建工具卡片最终状态 |
| `error` | 错误帧 |

> 设计要点：`step_break` 是**独立事件**而非混在 chunk 文本里的字符串标记，避免多步工具调用时文本拼接到同一气泡产生乱码。

---

## 6. 工具系统

内置工具覆盖常见 Agent 操作面：

| 类别 | 工具 / 机制 | 说明 |
| --- | --- | --- |
| 文件系统 | `fs_read`, `fs_search`, `fs_write`, `str_replace_editor` | 读取、搜索、写入和精确替换文件内容 |
| 进程执行 | `shell_execute` | 通过本地 Shell 执行命令，受审批与沙箱策略约束 |
| 网络 | `web_search`, `web_fetch` | 搜索与抓取网页内容 |
| 插件 | `plugin__<pluginId>__<toolName>` | Java Native 或 Node Bridge 插件工具 |
| MCP | `mcp__<serverName>__<toolName>` | MCP Server 工具适配 |
| 交互 | `ask_user_question` | 向用户发起澄清问题 |

工具调用经过统一 `ToolCallExecutor`：**参数解析 → PRE_TOOL_USE Hook → 执行 → POST Hook → 结果回填会话事件**。默认配置中 `shell_execute`、`fs_write`、`plugin.run`、`subprocess.spawn` 被列为需要审批的高风险能力；`web` profile 整体需要审批。

---

## 7. 插件与扩展

### 7.1 Java Native Plugin

Java Native Plugin 以 JAR 方式安装，宿主使用**隔离 `URLClassLoader`** 加载（进程内、类隔离）：

1. 实现 `JavaHarnessPlugin` 或继承 `AbstractHarnessPlugin`。
2. 实现 `AbstractTool`，声明 `name()`、`description()`、`parameters()`、`run()`。
3. 在 `META-INF/plugin.yaml` 中声明插件元数据。
4. 在 `META-INF/services/cn.walioffice.deepseek.harness.domain.plugin.spi.JavaHarnessPlugin` 中注册入口类。
5. 打包为 JAR 并放入 `harness.plugin.install-root` 指向的目录（默认 `./plugins`）。

插件工具注册后的名称格式：`plugin__<pluginId>__<toolName>`。

### 7.2 Node Bridge Plugin

`DSH_NODE_BRIDGE` 用于兼容 Node 侧插件：

- 插件包复制到 `harness.plugin.install-root`；
- 识别 `.codex-plugin/plugin.json`、`package.json`、`cordis.yml` 入口；
- 对可执行的 Node 插件生成受限 `node <entrypoint>` 运行计划，通过 **sidecar 进程 + JSON-RPC** 通信，不经 Shell 拼接；
- 入口限制在插件安装目录内的 `.js`、`.mjs`、`.cjs` 文件。

### 7.3 MCP 工具适配

- `StdioMcpClient`：通过 stdin/stdout 与 MCP Server 做 JSON-RPC 2.0 通信；
- `McpToolAdapter` 把远端工具注册为 `mcp__<serverName>__<toolName>`，与内置工具同链执行；
- 在 `harness.mcp.servers` 中配置 `name/command/args/env/cwd`，启动时自动连接。

### 7.4 插件生命周期

```text
install → activate → run → disable → uninstall
```

预置插件可在 `harness.plugin.preset.plugins` 中声明（支持 `auto-enable`），启动时自动装载。完整 Archetype 命令、代码模板和 HTTP 安装示例见 [`docs/java-plugin-development.md`](docs/java-plugin-development.md)。

---

## 8. REST API 概览

以下是最常用的 API 分组；路径均为 Spring Boot 实际暴露路径。

| 分组 | 端点示例 | 说明 |
| --- | --- | --- |
| Agent | `POST /api/agent/message`, `POST /api/agent/stream` | 普通对话与 SSE 流式对话 |
| Agent 控制 | `GET /api/agent/{agentId}/status`, `POST /api/agent/{agentId}/cancel` | 查询状态和取消运行 |
| 工作区 | `GET /api/agent/workspaces`, `POST /api/agent/workspaces` | 查询、创建、重命名、排序、删除工作区 |
| 任务 | `POST /api/harness/tasks/submit` | 提交 Harness Task |
| 目标 | `GET /api/harness/goals/{sessionId}`, `POST /api/harness/goals/{sessionId}` | 查询与维护目标状态 |
| 审批 | `GET /api/harness/approvals/pending`, `POST /api/harness/approvals/{sessionId}/approve` | 查询待审批项并审批 |
| 模型设置 | `GET /api/harness/settings/models`, `POST /api/harness/settings/models` | 查询、保存、删除模型设置 |
| 模型发现 | `POST /api/harness/settings/models/discover`, `GET /api/harness/runtime/models` | 发现模型和查询运行时目录 |
| 插件 | `GET /api/harness/plugins`, `POST /api/harness/plugins/install` | 插件列表、安装、激活、运行、启停、卸载 |
| 终端 | `POST /api/harness/terminal/sessions`, `GET /api/harness/terminal/sessions/{id}/read` | 打开、发送、读取、关闭终端 |
| 工作流 | `POST /api/workflow/start`, `POST /api/workflow/stream` | 启动工作流、取消和 SSE 输出 |
| 控制台 | `GET /api/harness/console/sessions`, `GET /api/harness/console/sessions/{id}/messages` | 会话与消息回放 |

---

## 9. Web 控制台

启动后访问 `http://localhost:8090/`。内置静态前端位于 `deepseek-harness-java-app/src/main/resources/static`，原生 JavaScript 实现（约 3300 行，无构建框架），本地化加载以下渲染库：

- `marked.min.js`：Markdown 解析；
- `purify.min.js`：HTML 安全过滤；
- `highlight-core.min.js` / `highlight-common.min.js`：代码高亮。

控制台覆盖的主要操作：

- 新建、切换、重命名、删除工作区；
- 创建和恢复 Agent 会话；
- 发送普通消息或流式消息（工具调用渲染为独立工具卡片）；
- 取消正在运行的 Agent；
- 查询、保存、删除和发现模型；
- 查看和启停插件；
- 查看待审批任务并执行审批；
- 查看历史会话与消息。

---

## 10. 关键配置

| 配置项 | 作用 | 说明 |
| --- | --- | --- |
| `server.port` | HTTP 端口 | 默认 `8090` |
| `spring.datasource.*` | JDBC 数据源 | 默认 MySQL；standalone profile 用 H2 |
| `harness.llm.deepseek.base-url` | 模型 API 地址 | 支持 OpenAI 兼容 Chat Completions |
| `harness.llm.deepseek.api-key` | 模型 API Key | **务必用环境变量注入**（`${DEEPSEEK_API_KEY}`），不要硬编码提交 |
| `harness.llm.deepseek.default-model` | 默认模型 | 例如 `gpt-5.5` |
| `harness.llm.deepseek.context-window` | 上下文窗口 | 默认 `128000` |
| `harness.auth.api-keys` | 宿主 API Key 列表 | 多个 Key 用英文逗号分隔；**为空则本地放行** |
| `harness.approval.required-tools` | 需要审批的工具 | 默认含 `shell_execute`、`fs_write`、`plugin.run`、`subprocess.spawn` |
| `harness.approval.required-profiles` | 需要审批的 Profile | 默认含 `web` |
| `harness.approval.runtime-timeout-ms` | 运行期审批等待超时 | 默认 `600000`（10 分钟） |
| `harness.sandbox.default-mode` | 沙箱模式 | `READ_ONLY / WORKSPACE_WRITE / DANGER_FULL_ACCESS` |
| `harness.mcp.servers` | MCP Server 列表 | stdio 传输，启动时连接注册 |
| `harness.plugin.install-root` | 插件安装根目录 | 默认 `./plugins` |
| `harness.plugin.preset.plugins` | 启动预装插件 | 支持设置 `auto-enable` |
| `harness.agent.temperature` | LLM 采样温度 | 系统属性/环境变量；为空则不发送该参数（适配只接受 temperature=1 的网关） |

---

## 11. 持久化模型

`schema.sql` 当前初始化的主要表（共 12 张）包括：

| 表 | 用途 |
| --- | --- |
| `harness_session` | Session 聚合基础状态 |
| `harness_session_header` | 会话请求头与上下文折叠状态 |
| `harness_session_event` / `harness_session_event_log` | 会话与 Agent 过程事件 |
| `harness_task` | 任务提交与执行状态 |
| `harness_goal_state` | 目标状态 |
| `harness_plugin_installation` | 插件安装元数据 |
| `harness_plugin_runtime_binding` | 插件运行绑定信息 |
| `harness_model_setting` | 模型与 Provider 配置 |
| `harness_tool_catalog` / `harness_tool_profile_binding` | 工具目录与 Profile 绑定 |

MySQL 承载结构化状态和事件（默认 profile）；standalone profile 使用 H2 文件库（`MODE=MySQL` 兼容模式），两者共用同一套 schema，`sql.init.mode=always` 幂等重跑。

---

## 12. 安全与执行边界

- `ApiKeyAuthInterceptor` 支持 `X-API-Key` 与 `Authorization: Bearer ...`；**`harness.auth.api-keys` 为空时 API 默认放行**（本地开发模式），静态资源与 `/actuator` 始终放行。
- `guard` 提供工具超时策略和重复调用提醒。
- `hooks` 支持生命周期钩子，阻止类 Hook 输出优先。
- `sandbox` 提供三档模式（`READ_ONLY / WORKSPACE_WRITE / DANGER_FULL_ACCESS`），本地开发默认 `WORKSPACE_WRITE`——写操作限制在工作区内、shell cwd 不得越界；`e2b` 提供可替换的远程沙箱端口。

---

## 13. 生产化注意事项

生产部署前建议完成以下检查：

1. **认证**：配置 `harness.auth.api-keys`，不要将本机服务直接暴露到公网。
2. **密钥管理**：通过环境变量、启动参数或外部密钥管理系统注入模型 API Key（切勿硬编码进 yml 提交仓库）。
3. **审批策略**：确认 Shell、文件写入、插件执行和子进程等高风险工具是否必须审批。
4. **沙箱边界**：限制 `cwd`、`workspaces`、`plugins`、`data` 的目录权限；优先使用独立容器或远程沙箱。
5. **数据库**：使用外部 MySQL，配置连接池、备份和事件表归档策略。
6. **可观测性**：接入日志采集、Metrics、Tracing 和关键业务审计。
7. **横向扩展**：当前设计为单实例 Agent 运行时；多实例部署前需要处理 Agent 内存状态、任务锁和会话亲和性。

---

## 14. 当前边界与已知隐患

- 项目已从早期骨架演进为可运行 Agent Harness，但仍不是承诺企业级 SLA 的完整产品。
- 部分外部能力依赖可选环境：E2B、ACP、MCP、Codex/Claude Code Subagent 等需要对应服务或命令存在。
- Agent 的运行状态和流式执行链路偏向单实例模型；分布式任务调度尚未完整引入。
- API 认证目前是简单 API Key 拦截器，还不是多租户身份体系。
- **运行期审批 gate 默认未启用**：审批只在任务提交期拦截（`PermissionCheckNode`）；`AgentFactory` 默认构造时 `approvalBroker=null`，工具执行器走 allowAll，对话链路里的高风险工具不受运行期拦截。
- **`shell_execute` 零沙箱**：`LocalShellExecutor` 直接 `/bin/sh -c`，无命令白名单，仅靠提交期审批兜底；生产环境务必配合审批策略与容器隔离。
- **`ScheduleService` 无驱动方**：`drive()` 无 `@Scheduled`/Quartz 调用方，且仓储为 InMemory，重启即丢。
- **线程池与阻塞**：`ReactLoopAgent.kick()` 与 `JobRegistryService` 用无界 `newCachedThreadPool`；`AgentCollectNode` 用 `.join()` 阻塞请求线程，高并发下需关注。
- **长会话性能**：`deriveMessages` 全量重建 O(n²)；token 预算用 `length/4` 粗估。
- **Stub/死路径**：E2B 沙箱只有 Stub 实现；CODEX/CORDIS 插件包型识别后 runnable=false；MCP 与插件 Bridge 互不打通；工作流 SSE 只推终态事件。

---

## 15. 文档索引

| 文档 | 内容 |
| --- | --- |
| [`docs/architecture.drawio`](docs/architecture.drawio) | 总体架构图（draw.io 源文件） |
| [`docs/domain-design.md`](docs/domain-design.md) | 领域设计详解：事件风暴、全部事件清单、端口与子域细节 |
| [`docs/java-plugin-development.md`](docs/java-plugin-development.md) | 插件开发指南：Archetype 命令、代码模板、HTTP 安装示例 |
