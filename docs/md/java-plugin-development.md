# Java 插件开发指南

## 概述

Java 版 deepseek-harness 支持纯 Java 编写的原生插件。插件以 JAR 包形式安装，在 harness JVM 内以隔离 ClassLoader 加载运行，无需 Node.js 或任何 sidecar 进程。

## 一键生成工程骨架（Maven Archetype）

最快的方式是用官方 archetype 生成插件工程骨架，自动配好 pom、基类继承、plugin.yaml、ServiceLoader 文件。

### 前置：安装 types 与 archetype 到本地仓库

```bash
cd deepseek-harness-java
mvn -q -pl deepseek-harness-java-types install -DskipTests
mvn -q -pl plugins/plugin-archetype install -DskipTests
```

### 生成插件工程

```bash
mvn archetype:generate \
  -DarchetypeGroupId=cn.xiaofuge \
  -DarchetypeArtifactId=deepseek-harness-plugin-archetype \
  -DarchetypeVersion=1.0.0-SNAPSHOT \
  -DgroupId=com.acme \
  -DartifactId=acme-weather-plugin \
  -Dversion=1.0.0-SNAPSHOT \
  -Dpackage=com.acme.weather \
  -DpluginId=acme-weather \
  -DpluginName="Acme Weather Plugin" \
  -DpluginClass=WeatherPlugin \
  -DpluginDescription="Weather query plugin" \
  -DpluginAuthor="Acme Team" \
  -DinteractiveMode=false
```

### 生成参数说明

| 参数 | 说明 | 默认值 |
|------|------|--------|
| `groupId` / `artifactId` / `version` | 标准 Maven 坐标 | version 默认 1.0.0-SNAPSHOT |
| `package` | Java 包名 | 同 groupId |
| `pluginId` | 插件唯一标识（plugin.yaml 的 id） | 同 artifactId |
| `pluginName` | 插件显示名 | 同 artifactId |
| `pluginClass` | 入口类名 | `Plugin` |
| `pluginDescription` | 插件描述 | A Java harness plugin. |
| `pluginAuthor` | 作者 | Unknown |

### 生成的结构

```
acme-weather-plugin/
├── pom.xml                          ← 已配好 types provided 依赖
└── src/main/
    ├── java/com/acme/weather/
    │   ├── WeatherPlugin.java       ← 继承 AbstractHarnessPlugin
    │   └── ExampleTool.java         ← 继承 AbstractTool 的示例工具
    └── resources/META-INF/
        ├── plugin.yaml              ← 已填好 id/name/entrypoint
        └── services/
            └── ...JavaHarnessPlugin ← 已填好入口类全限定名
```

### 编译打包

```bash
cd acme-weather-plugin
mvn package
# 产出 target/acme-weather-plugin-1.0.0-SNAPSHOT.jar
```

打出的 JAR 直接放入 harness 的 `plugins/` 目录即可使用。

---

## 模块依赖关系

```
deepseek-harness-java-types      ← 插件开发契约包（SPI 接口 + 抽象基类 + 值对象）
        ↑
deepseek-harness-java-domain     ← 领域层（依赖 types）
        ↑
deepseek-harness-java-case / infrastructure / app ...
```

**插件作者只需依赖 `deepseek-harness-java-types`**，这是唯一的轻量契约包，不含 Spring 和任何运行时实现。

## 插件规范

### 1. 目录结构

```
my-plugin/
├── pom.xml
└── src/main/
    ├── java/
    │   └── com/example/
    │       ├── MyPlugin.java          ← 继承 AbstractHarnessPlugin
    │       └── MyTool.java            ← 继承 AbstractTool
    └── resources/
        └── META-INF/
            ├── plugin.yaml
            └── services/
                └── cn.xiaofuge.deepseek.harness.domain.spi.JavaHarnessPlugin
```

### 2. Maven 依赖

```xml
<dependency>
    <groupId>cn.xiaofuge</groupId>
    <artifactId>deepseek-harness-java-types</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <scope>provided</scope>   <!-- 打包时不打入插件 JAR，由宿主 JVM 提供 -->
</dependency>
```

### 3. plugin.yaml 清单

```yaml
id: my-plugin
name: My Plugin
version: 1.0.0
author: Jane Doe
description: A plugin.
entrypoint: com.example.MyPlugin   # 可选，不填则用 ServiceLoader 发现
```

### 4. ServiceLoader 注册

文件：`META-INF/services/cn.xiaofuge.deepseek.harness.domain.spi.JavaHarnessPlugin`

内容为实现类全限定名。

## 标准模板（推荐）

### 插件入口：继承 AbstractHarnessPlugin

```java
public class MyPlugin extends AbstractHarnessPlugin {

    public MyPlugin() {
        super("my-plugin");      // 必须与 plugin.yaml 的 id 一致
    }

    @Override
    public List<ToolDefinition> tools() {
        return List.of(new MyTool());
    }

    // onStart() / onStop() 已有默认实现（带日志），需要时 override
}
```

`AbstractHarnessPlugin` 提供：
- `pluginId()` 由构造参数锁定，不会与 plugin.yaml 漂移
- 内置 `protected final Logger log`
- `onStart()` / `onStop()` 默认日志实现
- `toolList(...)` 辅助方法收集动态工具集

### 工具实现：继承 AbstractTool

```java
public class MyTool extends AbstractTool {

    @Override public String name() { return "my-tool"; }

    @Override public String description() { return "做什么用的"; }

    @Override
    public Map<String, Object> parameters() {
        return objectSchema()
                .prop("input", stringSchema("输入文本"))
                .prop("count", integerSchema("重复次数"))
                .required("input")
                .build();
    }

    @Override
    protected CompletableFuture<ToolExecutionResult> run(Map<String, Object> args, ToolRunContext ctx) {
        String input = str(args, "input");
        int count = intVal(args, "count", 1);

        if (input.isBlank()) {
            return fail("input 不能为空", "MISSING_INPUT");
        }
        return ok("结果: " + input.repeat(count));
    }
}
```

`AbstractTool` 提供：

| 能力 | 方法 |
|------|------|
| 参数解析 | `run(Map, ctx)` — 自动把 Object args 转为类型安全 Map |
| 字符串读取 | `str(args, key)` / `str(args, key, default)` |
| 数值读取 | `dbl(args, key)` / `intVal(args, key[, default])` |
| 布尔读取 | `bool(args, key)` |
| 成功结果 | `ok(text)` / `ok(List<ContentBlock>)` |
| 失败结果 | `fail(message, code)` |
| Schema 构建 | `objectSchema().prop(...).required(...).build()` |
| Schema 属性 | `stringSchema` / `numberSchema` / `integerSchema` / `booleanSchema` / `enumSchema` |
| 并发标记 | override `isConcurrencySafe(args)` 返回 true |
| 超时设置 | override `timeoutMs()` 返回毫秒数 |

不再需要手写的样板：
- `@SuppressWarnings("unchecked")` 强转
- `Map.of("type","object","properties",Map.of(...))` 嵌套拼装
- `ToolExecutionResult.ok(List.of(new TextBlock(...)))` 重复包装

### 完整示例

参考 `plugins/example-java-plugin/`（echo + calculator）和 `plugins/sample-tools-plugin/`（weather_query + file_summary）。

## 安装与使用

### HTTP API

```bash
# 安装
curl -X POST http://localhost:8090/api/harness/plugins/install \
  -H "Content-Type: application/json" \
  -d '{"pluginId":"my-plugin","displayName":"My Plugin","pluginVersion":"1.0.0",
       "runtimeType":"JAVA_NATIVE","sourcePath":"/path/to/my-plugin.jar",
       "entrypoint":"my-plugin.jar"}'

# 激活
curl -X POST http://localhost:8090/api/harness/plugins/activate \
  -H "Content-Type: application/json" -d '{"pluginId":"my-plugin"}'

# 运行
curl -X POST http://localhost:8090/api/harness/plugins/run \
  -H "Content-Type: application/json" -d '{"pluginId":"my-plugin"}'

# 状态
curl http://localhost:8090/api/harness/plugins/my-plugin/status

# 停止 / 卸载
curl -X POST http://localhost:8090/api/harness/plugins/my-plugin/disable
curl -X POST http://localhost:8090/api/harness/plugins/my-plugin/uninstall
```

### 配置文件预装

```yaml
harness:
  extensions:
    plugins:
      install-root: ./plugins
      preset:
        - plugin-id: my-plugin
          display-name: My Plugin
          plugin-version: 1.0.0
          runtime-type: JAVA_NATIVE
          source-path: ./plugins/my-plugin.jar
          entrypoint: my-plugin.jar
          auto-start: true
```

## 工具命名

注册名格式：`plugin__<pluginId>__<toolName>`

## 生命周期

安装(install) → 激活(activate) → 运行(run) → 停止(disable) → 卸载(uninstall)

## JAVA_NATIVE vs DSH_NODE_BRIDGE

| 维度 | JAVA_NATIVE | DSH_NODE_BRIDGE |
|------|-------------|-----------------|
| 运行 | JVM 内进程 | Node sidecar |
| 通信 | 直接方法调用 | JSON-RPC stdio |
| 依赖 | 只需 JDK + types 包 | 需 Node.js |
| 隔离 | URLClassLoader | 进程隔离 |
| 开发 | 继承 AbstractHarnessPlugin / AbstractTool | 按 TS Cordis 插件开发 |
