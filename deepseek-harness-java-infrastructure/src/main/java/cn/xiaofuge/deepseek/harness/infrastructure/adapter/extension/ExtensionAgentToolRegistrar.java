package cn.xiaofuge.deepseek.harness.infrastructure.adapter.extension;

import cn.xiaofuge.deepseek.harness.domain.extension.IExtensionManagementPort;
import cn.xiaofuge.deepseek.harness.domain.tool.adapter.ToolRegistry;
import cn.xiaofuge.deepseek.harness.domain.tool.extension.ExtensionTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 扩展管理 Agent 工具注册器：把 extension_* 工具族注册进共享 ToolRegistry，
 * 让 Agent 在对话中即可安装技能（含自拉代码）、配置 MCP server。
 *
 * <p>与 MCP/插件工具同层：所有 Agent 会话共享这些工具。</p>
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
@Component
@Order(40) // 晚于 MCP bootstrap(30)，保持扩展工具最后就绪
public class ExtensionAgentToolRegistrar implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ExtensionAgentToolRegistrar.class);

    private final ToolRegistry toolRegistry;
    private final IExtensionManagementPort extensionManagement;

    public ExtensionAgentToolRegistrar(ToolRegistry toolRegistry, IExtensionManagementPort extensionManagement) {
        this.toolRegistry = toolRegistry;
        this.extensionManagement = extensionManagement;
    }

    @Override
    public void run(ApplicationArguments args) {
        toolRegistry.register(new ExtensionTools.SkillListTool(extensionManagement));
        toolRegistry.register(new ExtensionTools.SkillInstallTool(extensionManagement));
        toolRegistry.register(new ExtensionTools.SkillManageTool(extensionManagement));
        toolRegistry.register(new ExtensionTools.McpListTool(extensionManagement));
        toolRegistry.register(new ExtensionTools.McpConfigureTool(extensionManagement));
        toolRegistry.register(new ExtensionTools.McpRemoveTool(extensionManagement));
        log.info("[extension] agent tools registered: extension_skill_list/install/manage, extension_mcp_list/configure/remove");
    }
}
