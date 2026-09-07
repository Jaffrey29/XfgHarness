package cn.xiaofuge.deepseek.harness.infrastructure.config;

import cn.xiaofuge.deepseek.harness.domain.tool.adapter.ToolRegistry;
import cn.xiaofuge.deepseek.harness.domain.tool.mcp.McpBootstrap;
import cn.xiaofuge.deepseek.harness.domain.tool.mcp.McpServerConfig;
import cn.xiaofuge.deepseek.harness.infrastructure.adapter.mcp.StdioMcpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.core.annotation.Order;

import java.util.List;

/**
 * MCP 启动器：应用启动后按 {@code harness.mcp.servers} 配置连接各 MCP server，
 * 并把远端工具以 {@code mcp__<server>__<tool>} 形式注册进共享 {@link ToolRegistry}。
 * <p>
 * 对齐原版 TS 的 mcp-client「连接 + 工具注入 registry」闭环；当前仅 stdio 传输。
 * 单个 server 连接失败不影响其余 server，错误记录到日志。
 * <p>
 * 应用关闭时统一 {@code disconnectAll} 释放子进程。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
@Configuration
@EnableConfigurationProperties(McpServersProperties.class)
@ConditionalOnProperty(prefix = "harness.mcp", name = "enabled", havingValue = "true", matchIfMissing = true)
public class McpBootstrapConfig implements ApplicationRunner, ApplicationListener<ContextClosedEvent> {

    private static final Logger log = LoggerFactory.getLogger(McpBootstrapConfig.class);

    /** stdio 调用超时（与 McpBootstrap 内部 DEFAULT_CALL_TIMEOUT_MS 对齐） */
    private static final long CALL_TIMEOUT_MS = 60_000;

    private final McpServersProperties properties;
    private final ToolRegistry toolRegistry;
    private volatile McpBootstrap bootstrap;

    /**
     * 处理MCP Bootstrap 配置。
     * 流程：适配外部协议或资源 → 调用底层能力 → 返回结果。
     * @param properties properties参数。
     * @param toolRegistry 工具 Registry参数。
     * @return 方法执行结果。
     */
    public McpBootstrapConfig(McpServersProperties properties, ToolRegistry toolRegistry) {
        this.properties = properties;
        this.toolRegistry = toolRegistry;
    }
    /**
     * 运行。
     */

    @Override
    @Order(30) // 在 PresetPluginLoader(20) 之后运行，确保基础工具链就绪
    public void run(ApplicationArguments args) {
        List<McpServersProperties.McpServer> servers = properties.getServers();
        if (servers == null || servers.isEmpty()) {
            log.info("[mcp] no servers configured (harness.mcp.servers), skipping MCP bootstrap");
            return;
        }

        bootstrap = new McpBootstrap(toolRegistry, config ->
                new StdioMcpClient(config.command(), config.args(), config.env(), config.cwd(), CALL_TIMEOUT_MS));

        List<McpServerConfig> configs = servers.stream()
                .filter(s -> s.getName() != null && !s.getName().isBlank())
                .filter(s -> s.getCommand() != null && !s.getCommand().isBlank())
                .map(s -> new McpServerConfig(s.getName(), s.getCommand(), s.getArgs(), s.getEnv(), s.getCwd()))
                .toList();

        if (configs.isEmpty()) {
            log.warn("[mcp] servers configured but none have valid name+command, skipping");
            return;
        }

        int registered = bootstrap.connectAll(configs);
        log.info("[mcp] bootstrap complete: {} tool(s) registered from {} server(s)", registered, configs.size());
        if (!bootstrap.errors().isEmpty()) {
            log.warn("[mcp] server connection errors: {}", bootstrap.errors());
        }
    }
    /**
     * on Application Event。
     * 流程：适配外部协议或资源 → 调用底层能力 → 返回结果。
     * @param event 领域事件。
     */


    @Override
    public void onApplicationEvent(ContextClosedEvent event) {
        if (bootstrap != null) {
            log.info("[mcp] disconnecting all MCP servers");
            bootstrap.disconnectAll();
        }
    }
}
