package cn.xiaofuge.deepseek.harness.domain.tool.extension;

import cn.xiaofuge.deepseek.harness.domain.extension.IExtensionManagementPort;
import cn.xiaofuge.deepseek.harness.domain.model.entity.ToolDefinition;
import cn.xiaofuge.deepseek.harness.domain.model.entity.ToolExecutionResult;
import cn.xiaofuge.deepseek.harness.domain.model.entity.ToolRunContext;
import cn.xiaofuge.deepseek.harness.domain.model.valobj.ContentBlock;
import cn.xiaofuge.deepseek.harness.domain.model.valobj.TextBlock;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 扩展管理工具族：让 Agent 在对话里即可完成技能安装（含自拉代码）、
 * 技能启停、MCP server 配置，与设置页共用 {@link IExtensionManagementPort}。
 *
 * <p>工具统一前缀 {@code extension_}，由基础设施层注册进共享 ToolRegistry。</p>
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public final class ExtensionTools {

    private ExtensionTools() { }

    /** 从 Map 参数取字符串。 */
    static String str(Map<String, Object> args, String key) {
        Object value = args.get(key);
        return value == null ? "" : String.valueOf(value).trim();
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> asMap(Object args) {
        return args instanceof Map ? (Map<String, Object>) args : Map.of();
    }

    static CompletableFuture<ToolExecutionResult> ok(String text) {
        List<ContentBlock> content = List.of(new TextBlock(text));
        return CompletableFuture.completedFuture(ToolExecutionResult.ok(content));
    }

    // ── 列出技能 ─────────────────────────────────────────

    public static class SkillListTool implements ToolDefinition {

        public static final String NAME = "extension_skill_list";
        private static final String DESCRIPTION =
                "List all installed skills (name, description, source, enabled). "
                + "Call this before installing or toggling a skill to see what already exists.";

        private final IExtensionManagementPort port;

        public SkillListTool(IExtensionManagementPort port) { this.port = port; }

        @Override public String name() { return NAME; }
        @Override public String description() { return DESCRIPTION; }

        @Override
        public Map<String, Object> parameters() {
            return Map.of("type", "object", "additionalProperties", false, "properties", Map.of());
        }

        @Override
        public CompletableFuture<ToolExecutionResult> execute(Object args, ToolRunContext ctx) {
            List<IExtensionManagementPort.ExtensionSkillSummary> skills = port.listSkills(null);
            if (skills.isEmpty()) {
                return ok("当前没有已安装的技能。可用 extension_skill_install 从 Git 仓库安装。");
            }
            StringBuilder sb = new StringBuilder("已安装技能：\n");
            for (var s : skills) {
                sb.append("- ").append(s.name())
                        .append(" [").append(s.enabled() ? "启用" : "停用").append("]")
                        .append(" 来源:").append(s.source())
                        .append(s.description() == null || s.description().isBlank() ? "" : " · " + s.description())
                        .append('\n');
            }
            return ok(sb.toString());
        }

        @Override public boolean isConcurrencySafe(Object args) { return true; }
    }

    // ── 从 Git 安装技能 ───────────────────────────────────

    public static class SkillInstallTool implements ToolDefinition {

        public static final String NAME = "extension_skill_install";
        private static final String DESCRIPTION =
                "Install a skill from a Git repository: clone the repo, locate SKILL.md, and copy the skill "
                + "into the user skills directory. Use when the user asks to install/add a skill, e.g. "
                + "\"帮我装一个 xxx 技能\". Returns the installed skill name; verify with extension_skill_list.";

        private final IExtensionManagementPort port;

        public SkillInstallTool(IExtensionManagementPort port) { this.port = port; }

        @Override public String name() { return NAME; }
        @Override public String description() { return DESCRIPTION; }

        @Override
        public Map<String, Object> parameters() {
            return Map.of(
                    "type", "object",
                    "additionalProperties", false,
                    "properties", Map.of(
                            "gitUrl", Map.of("type", "string",
                                    "description", "Git repository URL (https or ssh), e.g. https://github.com/user/awesome-skills.git"),
                            "subdir", Map.of("type", "string",
                                    "description", "Optional: path inside the repo to the skill directory containing SKILL.md. Omit to auto-search."),
                            "name", Map.of("type", "string",
                                    "description", "Optional: skill directory name (kebab-case). Defaults to repo/skill directory name.")
                    ),
                    "required", List.of("gitUrl")
            );
        }

        @Override
        public CompletableFuture<ToolExecutionResult> execute(Object args, ToolRunContext ctx) {
            Map<String, Object> map = asMap(args);
            String gitUrl = str(map, "gitUrl");
            if (gitUrl.isEmpty()) {
                return CompletableFuture.completedFuture(
                        ToolExecutionResult.fail("gitUrl is required", "MISSING_PARAM"));
            }
            IExtensionManagementPort.ExtensionSkillInstallResult result =
                    port.installSkillFromGit(gitUrl, str(map, "subdir"), str(map, "name"));
            if (result.success()) {
                return ok(result.message() + "\n路径: " + result.path()
                        + "\n技能已可用（skill 工具按名称加载，或重启会话后自动进入技能目录）。");
            }
            return CompletableFuture.completedFuture(ToolExecutionResult.fail(result.message(), "INSTALL_FAILED"));
        }

        @Override public boolean isConcurrencySafe(Object args) { return false; }
    }

    // ── 技能启停/删除 ─────────────────────────────────────

    public static class SkillManageTool implements ToolDefinition {

        public static final String NAME = "extension_skill_manage";
        private static final String DESCRIPTION =
                "Enable, disable, or remove an installed skill. "
                + "action=enable|disable|remove. Disabled skills are hidden from the skill tool.";

        private final IExtensionManagementPort port;

        public SkillManageTool(IExtensionManagementPort port) { this.port = port; }

        @Override public String name() { return NAME; }
        @Override public String description() { return DESCRIPTION; }

        @Override
        public Map<String, Object> parameters() {
            return Map.of(
                    "type", "object",
                    "additionalProperties", false,
                    "properties", Map.of(
                            "name", Map.of("type", "string", "description", "Skill name (kebab-case)."),
                            "action", Map.of("type", "string",
                                    "description", "One of: enable, disable, remove.",
                                    "enum", List.of("enable", "disable", "remove"))
                    ),
                    "required", List.of("name", "action")
            );
        }

        @Override
        public CompletableFuture<ToolExecutionResult> execute(Object args, ToolRunContext ctx) {
            Map<String, Object> map = asMap(args);
            String name = str(map, "name");
            String action = str(map, "action").toLowerCase();
            if (name.isEmpty()) {
                return CompletableFuture.completedFuture(
                        ToolExecutionResult.fail("name is required", "MISSING_PARAM"));
            }
            try {
                switch (action) {
                    case "enable" -> {
                        port.setSkillEnabled(name, true);
                        return ok("技能 " + name + " 已启用。");
                    }
                    case "disable" -> {
                        port.setSkillEnabled(name, false);
                        return ok("技能 " + name + " 已停用。");
                    }
                    case "remove" -> {
                        port.removeSkill(name, null);
                        return ok("技能 " + name + " 已删除。");
                    }
                    default -> {
                        return CompletableFuture.completedFuture(
                                ToolExecutionResult.fail("action 必须是 enable/disable/remove", "INVALID_PARAM"));
                    }
                }
            } catch (Exception e) {
                return CompletableFuture.completedFuture(ToolExecutionResult.fail(e.getMessage(), "OPERATION_FAILED"));
            }
        }

        @Override public boolean isConcurrencySafe(Object args) { return false; }
    }

    // ── 列出 MCP server ──────────────────────────────────

    public static class McpListTool implements ToolDefinition {

        public static final String NAME = "extension_mcp_list";
        private static final String DESCRIPTION =
                "List configured MCP servers (name, transport, endpoint/command, status, registered tool count).";

        private final IExtensionManagementPort port;

        public McpListTool(IExtensionManagementPort port) { this.port = port; }

        @Override public String name() { return NAME; }
        @Override public String description() { return DESCRIPTION; }

        @Override
        public Map<String, Object> parameters() {
            return Map.of("type", "object", "additionalProperties", false, "properties", Map.of());
        }

        @Override
        public CompletableFuture<ToolExecutionResult> execute(Object args, ToolRunContext ctx) {
            List<IExtensionManagementPort.ExtensionMcpServer> servers = port.listMcpServers();
            if (servers.isEmpty()) {
                return ok("当前没有配置 MCP server。可用 extension_mcp_configure 添加。");
            }
            StringBuilder sb = new StringBuilder("MCP servers：\n");
            for (var s : servers) {
                sb.append("- ").append(s.name())
                        .append(" transport=").append(s.transport())
                        .append(" source=").append(s.presetSource());
                if ("stdio".equals(s.transport())) {
                    sb.append(" command=").append(s.command());
                } else {
                    sb.append(" url=").append(s.url());
                }
                sb.append('\n');
            }
            return ok(sb.toString());
        }

        @Override public boolean isConcurrencySafe(Object args) { return true; }
    }

    // ── 配置 MCP server ──────────────────────────────────

    public static class McpConfigureTool implements ToolDefinition {

        public static final String NAME = "extension_mcp_configure";
        private static final String DESCRIPTION =
                "Add or update an MCP server, test its connection, and register its tools as "
                + "mcp__<server>__<tool>. transport=stdio needs command(+args); sse / streamable-http need url "
                + "(headers/env optional). Use test=true to only validate without saving.";

        private final IExtensionManagementPort port;

        public McpConfigureTool(IExtensionManagementPort port) { this.port = port; }

        @Override public String name() { return NAME; }
        @Override public String description() { return DESCRIPTION; }

        @Override
        public Map<String, Object> parameters() {
            return Map.of(
                    "type", "object",
                    "additionalProperties", false,
                    "properties", Map.of(
                            "name", Map.of("type", "string", "description", "Unique server name."),
                            "transport", Map.of("type", "string",
                                    "description", "stdio | sse | streamable-http. Default stdio.",
                                    "enum", List.of("stdio", "sse", "streamable-http")),
                            "command", Map.of("type", "string",
                                    "description", "stdio: executable, e.g. npx"),
                            "args", Map.of("type", "array", "items", Map.of("type", "string"),
                                    "description", "stdio: command arguments"),
                            "url", Map.of("type", "string",
                                    "description", "sse/streamable-http: server URL"),
                            "env", Map.of("type", "object", "additionalProperties", Map.of("type", "string"),
                                    "description", "stdio: extra environment variables, e.g. API keys"),
                            "headers", Map.of("type", "object", "additionalProperties", Map.of("type", "string"),
                                    "description", "sse/streamable-http: HTTP headers, e.g. Authorization"),
                            "test", Map.of("type", "boolean",
                                    "description", "true = only test connection, do not save")
                    ),
                    "required", List.of("name", "transport")
            );
        }

        @Override
        public CompletableFuture<ToolExecutionResult> execute(Object args, ToolRunContext ctx) {
            Map<String, Object> map = asMap(args);
            String name = str(map, "name");
            String transport = str(map, "transport").isEmpty() ? "stdio" : str(map, "transport");
            if (name.isEmpty()) {
                return CompletableFuture.completedFuture(
                        ToolExecutionResult.fail("name is required", "MISSING_PARAM"));
            }
            IExtensionManagementPort.ExtensionMcpServer server = new IExtensionManagementPort.ExtensionMcpServer(
                    name,
                    transport,
                    str(map, "command"),
                    stringList(map.get("args")),
                    stringMap(map.get("env")),
                    "",
                    str(map, "url"),
                    stringMap(map.get("headers")),
                    "custom"
            );
            try {
                boolean testOnly = Boolean.parseBoolean(str(map, "test")) || Boolean.TRUE.equals(map.get("test"));
                IExtensionManagementPort.ExtensionMcpApplyResult result = testOnly
                        ? port.testMcpServer(server)
                        : port.upsertMcpServer(server);
                String head = testOnly ? "[测试] " : "";
                if (result.success()) {
                    StringBuilder sb = new StringBuilder(head).append(result.message());
                    if (!result.tools().isEmpty()) {
                        sb.append("\n工具: ").append(String.join(", ", result.tools()));
                    }
                    return ok(sb.toString());
                }
                return CompletableFuture.completedFuture(ToolExecutionResult.fail(result.message(), "MCP_CONNECT_FAILED"));
            } catch (Exception e) {
                return CompletableFuture.completedFuture(ToolExecutionResult.fail(e.getMessage(), "MCP_CONFIG_FAILED"));
            }
        }

        @Override public boolean isConcurrencySafe(Object args) { return false; }
    }

    // ── 移除 MCP server ──────────────────────────────────

    public static class McpRemoveTool implements ToolDefinition {

        public static final String NAME = "extension_mcp_remove";
        private static final String DESCRIPTION =
                "Remove a configured MCP server: disconnect it and unregister its tools.";

        private final IExtensionManagementPort port;

        public McpRemoveTool(IExtensionManagementPort port) { this.port = port; }

        @Override public String name() { return NAME; }
        @Override public String description() { return DESCRIPTION; }

        @Override
        public Map<String, Object> parameters() {
            return Map.of(
                    "type", "object",
                    "additionalProperties", false,
                    "properties", Map.of("name", Map.of("type", "string", "description", "Server name to remove.")),
                    "required", List.of("name")
            );
        }

        @Override
        public CompletableFuture<ToolExecutionResult> execute(Object args, ToolRunContext ctx) {
            String name = str(asMap(args), "name");
            if (name.isEmpty()) {
                return CompletableFuture.completedFuture(
                        ToolExecutionResult.fail("name is required", "MISSING_PARAM"));
            }
            try {
                port.removeMcpServer(name);
                return ok("MCP server " + name + " 已移除并断开。");
            } catch (Exception e) {
                return CompletableFuture.completedFuture(ToolExecutionResult.fail(e.getMessage(), "OPERATION_FAILED"));
            }
        }

        @Override public boolean isConcurrencySafe(Object args) { return false; }
    }

    // ── 参数解析辅助 ──────────────────────────────────────

    @SuppressWarnings("unchecked")
    private static List<String> stringList(Object raw) {
        List<String> list = new ArrayList<>();
        if (raw instanceof List<?> l) {
            for (Object item : l) {
                if (item != null) list.add(String.valueOf(item));
            }
        } else if (raw instanceof Map<?, ?> m) {
            // 容错：模型可能把 args 传成 {0: a, 1: b}
            for (Object value : m.values()) {
                if (value != null) list.add(String.valueOf(value));
            }
        }
        return list;
    }

    @SuppressWarnings("unchecked")
    private static java.util.Map<String, String> stringMap(Object raw) {
        java.util.Map<String, String> map = new java.util.LinkedHashMap<>();
        if (raw instanceof Map<?, ?> m) {
            for (Map.Entry<?, ?> entry : m.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    map.put(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
                }
            }
        }
        return map;
    }
}
