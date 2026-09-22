package cn.xiaofuge.deepseek.harness.infrastructure.adapter.extension;

import cn.xiaofuge.deepseek.harness.domain.agent.service.subagent.SubagentProvider;
import cn.xiaofuge.deepseek.harness.domain.agent.service.subagent.SubagentRegistry;
import cn.xiaofuge.deepseek.harness.domain.extension.IExtensionManagementPort;
import cn.xiaofuge.deepseek.harness.domain.skill.ISkillService;
import cn.xiaofuge.deepseek.harness.domain.skill.model.entity.SkillSummary;
import cn.xiaofuge.deepseek.harness.domain.tool.mcp.McpServerConfig;
import cn.xiaofuge.deepseek.harness.infrastructure.adapter.skill.FilesystemSkillProviderPort;
import cn.xiaofuge.deepseek.harness.infrastructure.adapter.subagent.AcpSubagentProvider;
import cn.xiaofuge.deepseek.harness.infrastructure.adapter.subagent.ClaudeCodeSubagentProvider;
import cn.xiaofuge.deepseek.harness.infrastructure.adapter.subagent.CodexSubagentProvider;
import cn.xiaofuge.deepseek.harness.infrastructure.config.HarnessExtensionsProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

/**
 * 扩展管理服务：技能安装/启停/删除、MCP server 增删改与连通性测试、
 * CLI 子智能体命令配置。实现领域端口 {@link IExtensionManagementPort}，
 * 供触发层 REST 与对话内 Agent 工具共用。
 *
 * <p>变更即时生效：技能启停走 {@link FilesystemSkillProviderPort#setDisabledNames}，
 * MCP 变更走 {@link McpRuntime} 重连，CLI 命令热更新子智能体 provider。</p>
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
@Service
public class ExtensionManagementService implements IExtensionManagementPort, ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ExtensionManagementService.class);

    private final ISkillService skillService;
    private final FilesystemSkillProviderPort skillProviderPort;
    private final HarnessExtensionsProperties properties;
    private final ExtensionConfigStore store;
    private final McpRuntime mcpRuntime;
    private final SubagentRegistry subagentRegistry;
    private final ObjectProvider<ClaudeCodeSubagentProvider> claudeProvider;
    private final ObjectProvider<CodexSubagentProvider> codexProvider;
    private final ObjectProvider<AcpSubagentProvider> acpProvider;

    /** yml 预置的 CLI 命令（未做运行时覆盖时的默认值）。 */
    private final String ymlAcpCommand;
    private final String ymlAcpSubagentCommand;
    private final String ymlCodexCommand;
    private final String ymlClaudeCommand;

    public ExtensionManagementService(
            ISkillService skillService,
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
        this.skillService = skillService;
        this.skillProviderPort = skillProviderPort;
        this.properties = properties;
        this.store = store;
        this.mcpRuntime = mcpRuntime;
        this.subagentRegistry = subagentRegistry;
        this.claudeProvider = claudeProvider;
        this.codexProvider = codexProvider;
        this.acpProvider = acpProvider;
        this.ymlAcpCommand = ymlAcpCommand;
        this.ymlAcpSubagentCommand = ymlAcpSubagentCommand;
        this.ymlCodexCommand = ymlCodexCommand;
        this.ymlClaudeCommand = ymlClaudeCommand;
    }

    // ── 启动 ─────────────────────────────────────────────

    @Override
    public void run(ApplicationArguments args) {
        applyDisabledSkills();
        log.info("[extension] management ready (store={})", store.file());
    }

    /** 把持久化的停用名单应用到技能 provider（启动与每次变更后调用）。 */
    private void applyDisabledSkills() {
        skillProviderPort.setDisabledNames(new HashSet<>(store.snapshot().disabledSkills));
    }

    // ── Skills ───────────────────────────────────────────

    @Override
    public List<ExtensionSkillSummary> listSkills(String cwd) {
        Set<String> disabled = new HashSet<>(store.snapshot().disabledSkills);
        Path userRoot = skillProviderPort.dshHome().resolve("skills");
        List<Path> managedRoots = new ArrayList<>();
        managedRoots.add(userRoot);
        managedRoots.addAll(skillProviderPort.customRoots());
        List<ExtensionSkillSummary> result = new ArrayList<>();
        for (SkillSummary skill : skillService.list(cwd)) {
            String path = skill.path().orElse("");
            result.add(toSummary(skill.name(), skill.description(), skill.source(), path, disabled, managedRoots));
        }
        // 停用的技能被 provider 过滤，扫描托管目录把它们补进列表（否则无法重新启用）
        Set<String> listed = new HashSet<>();
        result.forEach(s -> listed.add(s.name()));
        for (String name : disabled) {
            if (listed.contains(name)) continue;
            for (Path root : managedRoots) {
                Path dirSkill = root.resolve(name).resolve("SKILL.md");
                Path fileSkill = root.resolve(name + ".md");
                if (Files.isRegularFile(dirSkill)) {
                    result.add(toSummary(name, readSkillDescription(dirSkill), "CUSTOM(disabled)",
                            dirSkill.toString(), disabled, managedRoots));
                    listed.add(name);
                    break;
                }
                if (Files.isRegularFile(fileSkill)) {
                    result.add(toSummary(name, readSkillDescription(fileSkill), "CUSTOM(disabled)",
                            fileSkill.toString(), disabled, managedRoots));
                    listed.add(name);
                    break;
                }
            }
        }
        return result;
    }

    private ExtensionSkillSummary toSummary(String name, String description, Object source,
                                            String path, Set<String> disabled, List<Path> managedRoots) {
        boolean removable = false;
        if (path != null && !path.isBlank()) {
            Path skillPath = Path.of(path);
            for (Path root : managedRoots) {
                if (skillPath.startsWith(root)) {
                    removable = true;
                    break;
                }
            }
        }
        return new ExtensionSkillSummary(
                name,
                description,
                source == null ? "" : String.valueOf(source),
                path,
                !disabled.contains(name),
                removable
        );
    }

    /** 读取 SKILL.md/技能 md 的 frontmatter description（失败返回空串）。 */
    private String readSkillDescription(Path file) {
        try {
            List<String> lines = Files.readAllLines(file);
            boolean inFrontmatter = false;
            for (int i = 0; i < Math.min(lines.size(), 20); i++) {
                String line = lines.get(i).trim();
                if (i == 0 && "---".equals(line)) {
                    inFrontmatter = true;
                    continue;
                }
                if (inFrontmatter && "---".equals(line)) break;
                if (inFrontmatter && line.startsWith("description:")) {
                    return line.substring("description:".length()).trim();
                }
            }
            return "";
        } catch (IOException e) {
            return "";
        }
    }

    @Override
    public ExtensionSkillInstallResult installSkillFromGit(String gitUrl, String subdir, String name) {
        if (gitUrl == null || gitUrl.isBlank()) {
            return new ExtensionSkillInstallResult(false, "", "", "gitUrl 不能为空");
        }
        Path tmpDir = null;
        try {
            Path workRoot = Files.createTempDirectory("dsh-skill-clone-");
            tmpDir = workRoot;
            ProcessBuilder pb = new ProcessBuilder("git", "clone", "--depth", "1", gitUrl.trim(), workRoot.resolve("repo").toString());
            pb.environment().put("GIT_TERMINAL_PROMPT", "0");
            pb.redirectErrorStream(true);
            Process process = pb.start();
            byte[] output = process.getInputStream().readAllBytes();
            int exit = process.waitFor();
            if (exit != 0) {
                return new ExtensionSkillInstallResult(false, "", "",
                        "git clone 失败（exit " + exit + "）: " + new String(output).trim());
            }
            Path repo = workRoot.resolve("repo");
            Path skillDir = locateSkillDir(repo, subdir);
            if (skillDir == null) {
                return new ExtensionSkillInstallResult(false, "", "",
                        "仓库中未找到 SKILL.md（可指定 subdir 指向技能目录）");
            }
            String skillName = resolveSkillName(name, skillDir, repo, gitUrl);
            Path target = installRoot().resolve(skillName);
            if (Files.exists(target)) {
                ExtensionConfigStore.deleteRecursively(target);
            }
            copyDirectory(skillDir, target);
            String message = "技能已安装: " + skillName;
            log.info("[extension] {}", message);
            return new ExtensionSkillInstallResult(true, skillName, target.toString(), message);
        } catch (Exception e) {
            log.warn("[extension] skill install failed: {}", e.getMessage());
            return new ExtensionSkillInstallResult(false, "", "", "安装失败: " + e.getMessage());
        } finally {
            if (tmpDir != null) {
                try { ExtensionConfigStore.deleteRecursively(tmpDir); } catch (IOException ignored) { }
            }
        }
    }

    @Override
    public void removeSkill(String name, String cwd) {
        ExtensionSkillSummary target = listSkills(cwd).stream()
                .filter(s -> s.name().equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("技能不存在: " + name));
        if (!target.removable()) {
            throw new IllegalArgumentException("该技能来源为 " + target.source() + "，仅允许删除用户目录下安装的技能");
        }
        String path = target.path();
        if (path != null && !path.isBlank()) {
            Path skillPath = Path.of(path);
            // 目录技能删目录；平铺 .md 技能删文件
            if (Files.isDirectory(skillPath)) {
                try { ExtensionConfigStore.deleteRecursively(skillPath); } catch (IOException e) {
                    throw new IllegalStateException("删除技能目录失败: " + e.getMessage(), e);
                }
            } else {
                try { Files.deleteIfExists(skillPath); } catch (IOException e) {
                    throw new IllegalStateException("删除技能文件失败: " + e.getMessage(), e);
                }
            }
        }
        // 从停用名单清理
        store.update(config -> config.disabledSkills.remove(name));
        applyDisabledSkills();
        log.info("[extension] skill removed: {}", name);
    }

    @Override
    public void setSkillEnabled(String name, boolean enabled) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("技能名不能为空");
        store.update(config -> {
            config.disabledSkills.remove(name);
            if (!enabled) config.disabledSkills.add(name);
        });
        applyDisabledSkills();
        log.info("[extension] skill {} -> {}", name, enabled ? "enabled" : "disabled");
    }

    private Path installRoot() throws IOException {
        Path root = skillProviderPort.dshHome().resolve("skills");
        Files.createDirectories(root);
        return root;
    }

    /** 在 clone 的仓库中定位 SKILL.md 所在目录：subdir 优先，其次两层深度自动搜索。 */
    private Path locateSkillDir(Path repo, String subdir) throws IOException {
        if (subdir != null && !subdir.isBlank()) {
            Path candidate = repo.resolve(subdir.trim()).normalize();
            if (Files.isRegularFile(candidate.resolve("SKILL.md"))) return candidate;
            return null;
        }
        if (Files.isRegularFile(repo.resolve("SKILL.md"))) return repo;
        try (Stream<Path> stream = Files.walk(repo, 2)) {
            return stream
                    .filter(p -> Files.isRegularFile(p.resolve("SKILL.md")))
                    .filter(p -> !p.equals(repo))
                    .min(Comparator.comparing(Path::toString))
                    .orElse(null);
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * 技能命名优先级：显式指定 name > SKILL.md frontmatter 的 name 字段 > 目录名。
     * 技能目录为 clone 仓库根目录时回退用 git 仓库名——clone 目录固定叫 repo，
     * 直接取目录名会让所有根目录型技能都叫 "repo"。
     */
    private String resolveSkillName(String requested, Path skillDir, Path repo, String gitUrl) {
        if (requested != null && !requested.isBlank()) {
            return normalizeName(requested, null);
        }
        String frontmatterName = readFrontmatterName(skillDir.resolve("SKILL.md"));
        if (frontmatterName != null && !frontmatterName.isBlank()) {
            return normalizeName(frontmatterName, null);
        }
        boolean atRepoRoot = repo.equals(skillDir);
        String fallback = atRepoRoot ? repoNameFromUrl(gitUrl) : skillDir.getFileName().toString();
        return normalizeName(fallback, "installed-skill");
    }

    /** 读取 SKILL.md frontmatter 的 name 字段（无 frontmatter 或没有 name 返回 null）。 */
    private String readFrontmatterName(Path file) {
        try {
            List<String> lines = Files.readAllLines(file);
            if (lines.isEmpty() || !"---".equals(lines.get(0).trim())) return null;
            for (int i = 1; i < Math.min(lines.size(), 20); i++) {
                String line = lines.get(i).trim();
                if ("---".equals(line)) break;
                if (line.startsWith("name:")) {
                    return line.substring("name:".length()).trim();
                }
            }
            return null;
        } catch (IOException e) {
            return null;
        }
    }

    /** 从 git URL 提取仓库名：去尾部斜杠与 .git 后缀，取最后一段。 */
    private String repoNameFromUrl(String gitUrl) {
        String trimmed = gitUrl.trim();
        while (trimmed.endsWith("/")) trimmed = trimmed.substring(0, trimmed.length() - 1);
        if (trimmed.toLowerCase(Locale.ROOT).endsWith(".git")) {
            trimmed = trimmed.substring(0, trimmed.length() - 4);
        }
        int slash = Math.max(trimmed.lastIndexOf('/'), trimmed.lastIndexOf(':'));
        return slash >= 0 ? trimmed.substring(slash + 1) : trimmed;
    }

    private String normalizeName(String requested, String fallback) {
        String raw = requested != null && !requested.isBlank() ? requested : fallback;
        String cleaned = raw.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-");
        cleaned = cleaned.replaceAll("^-+|-+$", "");
        return cleaned.isEmpty() ? "installed-skill" : cleaned;
    }

    private void copyDirectory(Path from, Path to) throws IOException {
        Files.createDirectories(to);
        try (Stream<Path> stream = Files.walk(from)) {
            for (Path source : stream.toList()) {
                Path dest = to.resolve(from.relativize(source).toString());
                if (Files.isDirectory(source)) {
                    Files.createDirectories(dest);
                } else {
                    Files.createDirectories(dest.getParent());
                    Files.copy(source, dest, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    // ── MCP ──────────────────────────────────────────────

    @Override
    public List<ExtensionMcpServer> listMcpServers() {
        Map<String, ExtensionMcpServer> merged = new LinkedHashMap<>();
        for (HarnessExtensionsProperties.McpServer preset : properties.getMcp().getServers()) {
            if (preset.getName() == null || preset.getName().isBlank()) continue;
            merged.put(preset.getName(), toView(preset, "preset"));
        }
        for (ExtensionConfigStore.StoredMcpServer custom : store.snapshot().mcpServers) {
            if (custom.name() == null || custom.name().isBlank()) continue;
            merged.put(custom.name(), toView(custom, "custom"));
        }
        return new ArrayList<>(merged.values());
    }

    @Override
    public ExtensionMcpApplyResult upsertMcpServer(ExtensionMcpServer server) {
        validateServer(server);
        // 先测试连通性，失败直接返回不给保存
        ExtensionMcpApplyResult probe = testMcpServer(server);
        if (!probe.success()) {
            return probe;
        }
        ExtensionConfigStore.StoredMcpServer stored = new ExtensionConfigStore.StoredMcpServer(
                server.name(), transport(server), server.command(), ExtensionConfigStore.copyList(server.args()),
                ExtensionConfigStore.copyMap(server.env()), server.cwd(), server.url(),
                ExtensionConfigStore.copyMap(server.headers()));
        store.update(config -> {
            config.mcpServers.removeIf(s -> s.name() != null && s.name().equals(server.name()));
            config.mcpServers.add(stored);
        });
        // 热应用：断开旧连接，按新配置重连
        mcpRuntime.disconnect(server.name());
        try {
            int count = mcpRuntime.connect(toConfig(stored));
            List<String> tools = probe.tools();
            return new ExtensionMcpApplyResult(true, server.name(), count, tools,
                    "已连接并注册 " + count + " 个工具");
        } catch (Exception e) {
            return new ExtensionMcpApplyResult(false, server.name(), 0, List.of(),
                    "配置已保存，但连接失败: " + e.getMessage());
        }
    }

    @Override
    public void removeMcpServer(String name) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("name 不能为空");
        store.update(config -> config.mcpServers.removeIf(s -> s.name() != null && s.name().equals(name)));
        mcpRuntime.disconnect(name);
        log.info("[extension] mcp server removed: {}", name);
    }

    @Override
    public ExtensionMcpApplyResult testMcpServer(ExtensionMcpServer server) {
        validateServer(server);
        McpRuntime.ExtensionMcpProbe probe = mcpRuntime.probe(toProbeConfig(server));
        if (probe.success()) {
            return new ExtensionMcpApplyResult(true, server.name(), probe.tools().size(), probe.tools(),
                    "连接成功，发现 " + probe.tools().size() + " 个工具");
        }
        return new ExtensionMcpApplyResult(false, server.name(), 0, List.of(), "连接失败: " + probe.message());
    }

    private void validateServer(ExtensionMcpServer server) {
        if (server == null || server.name() == null || server.name().isBlank()) {
            throw new IllegalArgumentException("MCP server name 不能为空");
        }
        String t = transport(server);
        if (!Set.of("stdio", "sse", "streamable-http").contains(t)) {
            throw new IllegalArgumentException("不支持的 transport: " + t + "（可选 stdio / sse / streamable-http）");
        }
        if ("stdio".equals(t)) {
            if (server.command() == null || server.command().isBlank()) {
                throw new IllegalArgumentException("stdio 传输需要提供 command");
            }
        } else if (server.url() == null || server.url().isBlank()) {
            throw new IllegalArgumentException(t + " 传输需要提供 url");
        }
    }

    private String transport(ExtensionMcpServer server) {
        return server.transport() == null || server.transport().isBlank() ? "stdio" : server.transport().trim();
    }

    private McpServerConfig toConfig(ExtensionConfigStore.StoredMcpServer s) {
        return new McpServerConfig(s.name(), s.command(), ExtensionConfigStore.copyList(s.args()),
                ExtensionConfigStore.copyMap(s.env()), s.cwd(), s.transport(), s.url(), ExtensionConfigStore.copyMap(s.headers()));
    }

    private McpServerConfig toProbeConfig(ExtensionMcpServer s) {
        return new McpServerConfig(s.name(), s.command(), ExtensionConfigStore.copyList(s.args()),
                ExtensionConfigStore.copyMap(s.env()), s.cwd(), transport(s), s.url(), ExtensionConfigStore.copyMap(s.headers()));
    }

    private ExtensionMcpServer toView(HarnessExtensionsProperties.McpServer s, String source) {
        return new ExtensionMcpServer(s.getName(), s.getTransport(), s.getCommand(), s.getArgs(),
                s.getEnv(), s.getCwd(), s.getUrl(), s.getHeaders(), source);
    }

    private ExtensionMcpServer toView(ExtensionConfigStore.StoredMcpServer s, String source) {
        return new ExtensionMcpServer(s.name(), s.transport(), s.command(), s.args(),
                s.env(), s.cwd(), s.url(), s.headers(), source);
    }

    // ── CLI ──────────────────────────────────────────────

    @Override
    public ExtensionCliConfig getCliConfig() {
        Map<String, String> cli = store.snapshot().cli;
        return new ExtensionCliConfig(
                cli.getOrDefault("acpCommand", ymlAcpCommand),
                cli.getOrDefault("acpSubagentCommand", ymlAcpSubagentCommand),
                cli.getOrDefault("codexCommand", ymlCodexCommand),
                cli.getOrDefault("claudeCommand", ymlClaudeCommand),
                ""
        );
    }

    @Override
    public ExtensionCliConfig setCliConfig(ExtensionCliConfig config) {
        if (config == null) throw new IllegalArgumentException("配置不能为空");
        ExtensionCliConfig effective = new ExtensionCliConfig(
                config.acpCommand(), config.acpSubagentCommand(), config.codexCommand(), config.claudeCommand(), "");
        store.update(stored -> {
            stored.cli.put("acpCommand", effective.acpCommand());
            stored.cli.put("acpSubagentCommand", effective.acpSubagentCommand());
            stored.cli.put("codexCommand", effective.codexCommand());
            stored.cli.put("claudeCommand", effective.claudeCommand());
        });
        List<String> notices = new ArrayList<>();
        applyClaude(effective.claudeCommand(), notices);
        applyCodex(effective.codexCommand(), notices);
        applyAcp(effective, notices);
        String notice = String.join("；", notices);
        return new ExtensionCliConfig(effective.acpCommand(), effective.acpSubagentCommand(),
                effective.codexCommand(), effective.claudeCommand(), notice);
    }

    private void applyClaude(String command, List<String> notices) {
        ClaudeCodeSubagentProvider provider = claudeProvider.getIfAvailable();
        String cmd = command.isBlank() ? ymlClaudeCommand : command;
        if (provider != null) {
            provider.setCommand(cmd);
            ensureRegistered(provider);
            notices.add("claude-code 子智能体命令已热更新为 " + cmd);
        }
    }

    private void applyCodex(String command, List<String> notices) {
        CodexSubagentProvider provider = codexProvider.getIfAvailable();
        String cmd = command.isBlank() ? ymlCodexCommand : command;
        if (provider != null) {
            provider.setCommand(cmd);
            ensureRegistered(provider);
            notices.add("codex 子智能体命令已热更新为 " + cmd);
        }
    }

    private void applyAcp(ExtensionCliConfig config, List<String> notices) {
        AcpSubagentProvider provider = acpProvider.getIfAvailable();
        if (provider != null && !config.acpSubagentCommand().isBlank()) {
            provider.setCommand(config.acpSubagentCommand());
            ensureRegistered(provider);
            notices.add("ACP 子智能体命令已热更新为 " + config.acpSubagentCommand());
        } else if (!config.acpSubagentCommand().isBlank()) {
            notices.add("ACP 主通道命令将在服务重启后生效（当前会话仍可用 claude-code/codex 子智能体）");
        }
    }

    /** 保证 provider 已注册进 SubagentRegistry（幂等）。 */
    private void ensureRegistered(SubagentProvider provider) {
        try {
            subagentRegistry.resolve(provider.name());
        } catch (IllegalArgumentException e) {
            subagentRegistry.register(provider);
        }
    }

    /** 诊断：当前生效的工具注册情况（MCP 侧）。 */
    public Set<String> connectedMcpServers() {
        return new TreeSet<>(mcpRuntime.status().keySet());
    }
}
