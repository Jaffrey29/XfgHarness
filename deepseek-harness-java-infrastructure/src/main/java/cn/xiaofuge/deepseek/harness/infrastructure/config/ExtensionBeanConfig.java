package cn.xiaofuge.deepseek.harness.infrastructure.config;

import cn.xiaofuge.deepseek.harness.domain.agent.service.subagent.SubagentRegistry;
import cn.xiaofuge.deepseek.harness.domain.tool.adapter.ToolRegistry;
import cn.xiaofuge.deepseek.harness.infrastructure.adapter.extension.ExtensionConfigStore;
import cn.xiaofuge.deepseek.harness.infrastructure.adapter.extension.ExtensionManagementService;
import cn.xiaofuge.deepseek.harness.infrastructure.adapter.extension.McpRuntime;
import cn.xiaofuge.deepseek.harness.infrastructure.adapter.skill.FilesystemSkillProviderPort;
import cn.xiaofuge.deepseek.harness.infrastructure.adapter.subagent.AcpSubagentProvider;
import cn.xiaofuge.deepseek.harness.infrastructure.adapter.subagent.ClaudeCodeSubagentProvider;
import cn.xiaofuge.deepseek.harness.infrastructure.adapter.subagent.CodexSubagentProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;

/**
 * 扩展管理装配：可写配置存储、MCP 运行时与管理服务。
 *
 * <p>调用流程：触发层/Agent 工具 → ExtensionManagementService → 各运行时。</p>
 * @author 小傅哥
 * @website bugstack.cn
 */
@Configuration
public class ExtensionBeanConfig {

    @Bean
    public ExtensionConfigStore extensionConfigStore(
            ObjectMapper objectMapper,
            @Value("${harness.config.dir:${user.home}/.dsh}") String configDir
    ) {
        ExtensionConfigStore store = new ExtensionConfigStore(objectMapper, Path.of(configDir, "extensions.json"));
        store.init();
        return store;
    }

    @Bean
    public McpRuntime mcpRuntime(ToolRegistry toolRegistry) {
        return new McpRuntime(toolRegistry);
    }

    @Bean
    public ExtensionManagementService extensionManagementService(
            cn.xiaofuge.deepseek.harness.domain.skill.ISkillService skillService,
            FilesystemSkillProviderPort skillProviderPort,
            HarnessExtensionsProperties properties,
            ExtensionConfigStore store,
            McpRuntime mcpRuntime,
            SubagentRegistry subagentRegistry,
            ObjectProvider<ClaudeCodeSubagentProvider> claudeProvider,
            ObjectProvider<CodexSubagentProvider> codexProvider,
            ObjectProvider<AcpSubagentProvider> acpProvider,
            @Value("${harness.acp.command:}") String ymlAcpCommand,
            @Value("${harness.subagent.acp.command:}") String ymlAcpSubagentCommand,
            @Value("${harness.subagent.codex.command:codex}") String ymlCodexCommand,
            @Value("${harness.subagent.claude-code.command:claude}") String ymlClaudeCommand
    ) {
        return new ExtensionManagementService(skillService, skillProviderPort, properties, store,
                mcpRuntime, subagentRegistry, claudeProvider, codexProvider, acpProvider,
                ymlAcpCommand, ymlAcpSubagentCommand, ymlCodexCommand, ymlClaudeCommand);
    }
}
