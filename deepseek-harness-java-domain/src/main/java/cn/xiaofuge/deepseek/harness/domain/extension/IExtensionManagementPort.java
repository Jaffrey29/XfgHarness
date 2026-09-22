package cn.xiaofuge.deepseek.harness.domain.extension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 扩展管理领域端口：统一管理技能（Skills）、MCP 服务与 CLI 子智能体配置。
 * <p>
 * 触发层 REST 接口与对话内 Agent 工具共用本端口，保证「设置页配置」与
 * 「对话里让 Agent 自己装技能/配 MCP」走同一条领域链路。
 *
 * <p>调用流程：触发层/工具 → 领域端口 → 基础设施适配器 → 外部系统。</p>
 * @author 小傅哥
 * @website bugstack.cn
 */
public interface IExtensionManagementPort {

    // ── Skills ───────────────────────────────────────────

    /** 列出全部技能（含启用状态与来源）。 */
    List<ExtensionSkillSummary> listSkills(String cwd);

    /**
     * 从 Git 仓库安装技能：clone → 定位 SKILL.md 所在目录 → 拷入用户技能根目录。
     * @param gitUrl Git 仓库地址（支持 https/ssh）
     * @param subdir 仓库内技能子目录（可选，为空则自动搜索 SKILL.md）
     * @param name 安装后的技能目录名（可选，默认取 SKILL.md 的 name 或仓库名）
     * @return 安装结果（含最终技能名与路径）
     */
    ExtensionSkillInstallResult installSkillFromGit(String gitUrl, String subdir, String name);

    /**
     * 从压缩包安装技能：解压 zip → 定位 SKILL.md 所在目录 → 拷入用户技能根目录。
     * @param zipPath 上传暂存的 zip 文件路径
     * @param subdir 压缩包内技能子目录（可选，为空则自动搜索 SKILL.md）
     * @param name 安装后的技能目录名（可选，默认取 SKILL.md 的 name 或压缩包名）
     * @return 安装结果（含最终技能名与路径）
     */
    ExtensionSkillInstallResult installSkillFromZip(String zipPath, String subdir, String name);

    /** 删除用户根目录下已安装的技能（bundled/project 来源不允许删除）。 */
    void removeSkill(String name, String cwd);

    /** 启用/停用技能（停用后技能不会进入技能目录，且无法被 skill 工具加载）。 */
    void setSkillEnabled(String name, boolean enabled);

    // ── MCP ──────────────────────────────────────────────

    /** 列出全部 MCP server 配置（yml 预置 + 运行时新增）及连接状态。 */
    List<ExtensionMcpServer> listMcpServers();

    /** 新增或更新一个 MCP server 配置，并立即应用（断开旧连接、按新配置重连）。 */
    ExtensionMcpApplyResult upsertMcpServer(ExtensionMcpServer server);

    /** 移除一个 MCP server 配置并断开其连接、注销其工具。 */
    void removeMcpServer(String name);

    /** 连通性测试：临时连接并列出工具，不落盘、不注册。 */
    ExtensionMcpApplyResult testMcpServer(ExtensionMcpServer server);

    // ── CLI（ACP / 外部 CLI 子智能体） ─────────────────────

    /** 读取 CLI 配置（yml/环境变量 + 运行时覆盖合并后的生效值）。 */
    ExtensionCliConfig getCliConfig();

    /** 保存 CLI 配置并即时更新子智能体 provider 命令。 */
    ExtensionCliConfig setCliConfig(ExtensionCliConfig config);

    // ── DTO ──────────────────────────────────────────────

    /** 技能摘要视图。 */
    record ExtensionSkillSummary(
            String name,
            String description,
            String source,
            String path,
            boolean enabled,
            boolean removable
    ) {}

    /** 技能安装结果。 */
    record ExtensionSkillInstallResult(boolean success, String name, String path, String message) {}

    /** MCP server 配置视图（与 harness.extensions.mcp.servers 字段对齐）。 */
    record ExtensionMcpServer(
            String name,
            String transport,
            String command,
            List<String> args,
            Map<String, String> env,
            String cwd,
            String url,
            Map<String, String> headers,
            String presetSource
    ) {}

    /** MCP 应用/测试结果。 */
    record ExtensionMcpApplyResult(boolean success, String name, int toolCount, List<String> tools, String message) {}

    /** CLI 子智能体配置。 */
    record ExtensionCliConfig(
            String acpCommand,
            String acpSubagentCommand,
            String codexCommand,
            String claudeCommand,
            String effectiveNotice
    ) {
        public ExtensionCliConfig {
            acpCommand = acpCommand == null ? "" : acpCommand;
            acpSubagentCommand = acpSubagentCommand == null ? "" : acpSubagentCommand;
            codexCommand = codexCommand == null ? "" : codexCommand;
            claudeCommand = claudeCommand == null ? "" : claudeCommand;
        }
    }

    /** 便捷：按名称查技能路径。 */
    default Optional<String> findSkillPath(String name, String cwd) {
        return listSkills(cwd).stream()
                .filter(s -> s.name().equals(name))
                .map(ExtensionSkillSummary::path)
                .filter(p -> p != null && !p.isBlank())
                .findFirst();
    }
}
