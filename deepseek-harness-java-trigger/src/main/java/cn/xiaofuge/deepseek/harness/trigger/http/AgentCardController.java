package cn.xiaofuge.deepseek.harness.trigger.http;

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Agent Card：本服务对外的能力自描述。
 *
 * <p>同时提供两套卡片：</p>
 * <ul>
 *   <li><b>DSH Agent Card</b>（{@code /.well-known/dsh-agent-card}）：
 *       本服务作为「数字人」被其他 DSH 实例接入时的自描述，{@code protocol: dsh.v1}。</li>
 *   <li><b>A2A Agent Card</b>（{@code /.well-known/agent-card.json}，兼容旧版路径
 *       {@code /.well-known/agent.json}）：与 A2A 协议标准 Agent Card 结构一致，
 *       供任意标准 A2A 客户端发现与接入。JSON-RPC 端点见 {@link A2AController}。</li>
 * </ul>
 *
 * <p>可通过 harness.yml 的 harness.agent-card.* 覆盖默认值，
 * 让每个部署实例呈现不同的数字人身份（如「服务器管家」）。</p>
 */
@RestController
public class AgentCardController {

    /** A2A 协议版本（Agent Card.protocolVersion）：0.3.x 系列的 JSON-RPC 绑定 */
    private static final String A2A_PROTOCOL_VERSION = "0.3.0";

    private final String displayName;
    private final String avatarUrl;
    private final String purpose;
    private final List<String> roleTags;
    private final List<String> tools;
    private final String approvalPolicy;
    private final int maxConcurrentTasks;
    private final String configuredApiKeys;

    public AgentCardController(
            @Value("${harness.agent-card.display-name:本地助手}") String displayName,
            @Value("${harness.agent-card.avatar-url:}") String avatarUrl,
            @Value("${harness.agent-card.purpose:通用智能体服务，可承担开发、分析与文档任务}") String purpose,
            @Value("${harness.agent-card.role-tags:general}") List<String> roleTags,
            @Value("${harness.agent-card.tools:fs.read,web.search}") List<String> tools,
            @Value("${harness.agent-card.approval-policy:WRITE_REQUIRES_APPROVAL}") String approvalPolicy,
            @Value("${harness.agent-card.max-concurrent-tasks:1}") int maxConcurrentTasks,
            @Value("${harness.auth.api-keys:}") String configuredApiKeys) {
        this.displayName = displayName;
        this.avatarUrl = avatarUrl;
        this.purpose = purpose;
        this.roleTags = roleTags;
        this.tools = tools;
        this.approvalPolicy = approvalPolicy;
        this.maxConcurrentTasks = maxConcurrentTasks;
        this.configuredApiKeys = configuredApiKeys;
    }

    // ── DSH Agent Card（DSH 实例互 Discover） ─────────────

    @GetMapping("/.well-known/dsh-agent-card")
    public Map<String, Object> agentCard() {
        Map<String, Object> card = new LinkedHashMap<>();
        card.put("schemaVersion", "1.0");
        card.put("digitalHumanId", "local");
        card.put("displayName", displayName);
        if (avatarUrl != null && !avatarUrl.isBlank()) {
            card.put("avatarUrl", avatarUrl);
        }
        card.put("purpose", purpose);
        card.put("roleTags", roleTags);
        card.put("capabilities", List.of(Map.of(
                "name", "agent-task",
                "description", purpose,
                "input", "objective",
                "output", "report")));
        card.put("tools", tools);
        card.put("approvalPolicy", approvalPolicy);
        card.put("maxConcurrentTasks", maxConcurrentTasks);
        card.put("protocol", "dsh.v1");
        card.put("endpoints", Map.of(
                "stream", "/api/agent/stream",
                "approvals", "/api/harness/approvals"));
        return card;
    }

    // ── A2A Agent Card（标准 A2A 发现，JSON-RPC 绑定） ─────

    /** A2A 标准 Agent Card 发现路径（spec §5.3，RFC 8615 well-known URI）。 */
    @GetMapping("/.well-known/agent-card.json")
    public Map<String, Object> a2aAgentCard(HttpServletRequest request) {
        return buildA2aCard(request);
    }

    /** 兼容旧版 A2A 规范（≤0.2.x）的发现路径；部分客户端仍探测该路径。 */
    @GetMapping("/.well-known/agent.json")
    public Map<String, Object> a2aAgentCardLegacy(HttpServletRequest request) {
        return buildA2aCard(request);
    }

    private Map<String, Object> buildA2aCard(HttpServletRequest request) {
        Map<String, Object> card = new LinkedHashMap<>();
        card.put("name", displayName);
        card.put("description", purpose);
        card.put("url", serviceBaseUrl(request) + "/a2a");
        card.put("preferredTransport", "JSONRPC");
        card.put("protocolVersion", A2A_PROTOCOL_VERSION);
        card.put("version", "0.1.7");
        if (avatarUrl != null && !avatarUrl.isBlank()) {
            card.put("iconUrl", avatarUrl);
        }
        card.put("provider", Map.of(
                "organization", "xiaofuge",
                "url", "https://github.com/fuzhengwei/deepseek-harness-java"));
        card.put("capabilities", Map.of(
                "streaming", true,
                "pushNotifications", false,
                "stateTransitionHistory", false));
        card.put("defaultInputModes", List.of("text/plain"));
        card.put("defaultOutputModes", List.of("text/plain"));
        card.put("skills", List.of(buildAgentTaskSkill()));

        // 配置了 API Key 时按标准声明安全方案，客户端凭 card.security 自行携带凭据
        boolean hasApiKeys = configuredApiKeys != null && !configuredApiKeys.isBlank();
        if (hasApiKeys) {
            card.put("securitySchemes", Map.of(
                    "bearer", Map.of("type", "http", "scheme", "bearer")));
            card.put("security", List.of(Map.of("bearer", List.of())));
        }
        return card;
    }

    /** 唯一技能：通用 agent 任务（目标进、报告出），标签取角色标签配置。 */
    private Map<String, Object> buildAgentTaskSkill() {
        Map<String, Object> skill = new LinkedHashMap<>();
        skill.put("id", "agent-task");
        skill.put("name", displayName);
        skill.put("description", purpose);
        List<String> tags = new ArrayList<>(roleTags);
        if (tags.isEmpty()) {
            tags.add("general");
        }
        skill.put("tags", tags);
        return skill;
    }

    /** 由请求推导服务对外 Base URL（scheme://host[:port]），供卡片 url 字段指向 JSON-RPC 端点。 */
    private String serviceBaseUrl(HttpServletRequest request) {
        String scheme = request.getScheme() == null ? "http" : request.getScheme();
        String host = request.getServerName();
        int port = request.getServerPort();
        boolean defaultPort = ("http".equals(scheme) && port == 80)
                || ("https".equals(scheme) && port == 443);
        StringBuilder builder = new StringBuilder(scheme).append("://").append(host);
        if (!defaultPort && port > 0) {
            builder.append(':').append(port);
        }
        return builder.toString();
    }
}
