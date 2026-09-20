package cn.xiaofuge.deepseek.harness.infrastructure.config;

import cn.xiaofuge.deepseek.harness.domain.tool.mcp.McpServerConfig;
import cn.xiaofuge.deepseek.harness.infrastructure.adapter.extension.McpRuntime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.core.annotation.Order;

import java.util.List;

/**
 * MCP 启动器：应用启动后按 {@code harness.extensions.mcp.servers} 配置连接各 MCP server，
 * 并把远端工具以 {@code mcp__<server>__<tool>} 形式注册进共享 ToolRegistry。
 * <p>
 * 实际连接工作委托给共享 {@link McpRuntime}，扩展管理在运行期对 server 增删改时
 * 直接复用该运行时做热更新（断开旧连接、按新配置重连、注销/注册工具）。
 * <p>
 * 单个 server 连接失败不影响其余 server，错误记录到日志。
 * 应用关闭时统一 disconnectAll 释放子进程。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
@Configuration
@ConditionalOnProperty(prefix = "harness.extensions.mcp", name = "enabled", havingValue = "true", matchIfMissing = true)
public class McpBootstrapConfig implements ApplicationRunner, ApplicationListener<ContextClosedEvent> {

    private static final Logger log = LoggerFactory.getLogger(McpBootstrapConfig.class);

    private final HarnessExtensionsProperties properties;
    private final McpRuntime mcpRuntime;

    public McpBootstrapConfig(HarnessExtensionsProperties properties, McpRuntime mcpRuntime) {
        this.properties = properties;
        this.mcpRuntime = mcpRuntime;
    }

    @Override
    @Order(30) // 在 PresetPluginLoader(20) 之后运行，确保基础工具链就绪
    public void run(ApplicationArguments args) {
        List<HarnessExtensionsProperties.McpServer> servers = properties.getMcp().getServers();
        if (servers == null || servers.isEmpty()) {
            log.info("[mcp] no servers configured (harness.extensions.mcp.servers), skipping MCP bootstrap");
            return;
        }

        List<McpServerConfig> configs = servers.stream()
                .filter(s -> s.getName() != null && !s.getName().isBlank())
                .map(s -> new McpServerConfig(
                        s.getName(),
                        s.getCommand(),
                        s.getArgs(),
                        s.getEnv(),
                        s.getCwd(),
                        s.getTransport(),
                        s.getUrl(),
                        s.getHeaders()
                ))
                .toList();

        if (configs.isEmpty()) {
            log.warn("[mcp] servers configured but none have a valid name, skipping");
            return;
        }

        int registered = mcpRuntime.connectAll(configs);
        log.info("[mcp] bootstrap complete: {} tool(s) registered from {} server(s)", registered, configs.size());
    }

    @Override
    public void onApplicationEvent(ContextClosedEvent event) {
        log.info("[mcp] disconnecting all MCP servers");
        mcpRuntime.disconnectAll();
    }
}
