package cn.xiaofuge.deepseek.harness.trigger.http;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * DSH Agent Card：本服务作为「数字人」被其他 DSH 实例接入时的能力自描述。
 *
 * <p>结构对齐 A2A Agent Card 语义，后续可直接映射。</p>
 * <p>可通过 harness.yml 的 harness.agent-card.* 覆盖默认值，
 * 让每个部署实例呈现不同的数字人身份（如「服务器管家」）。</p>
 */
@RestController
public class AgentCardController {

    private final String displayName;
    private final String avatarUrl;
    private final String purpose;
    private final List<String> roleTags;
    private final List<String> tools;
    private final String approvalPolicy;
    private final int maxConcurrentTasks;

    public AgentCardController(
            @Value("${harness.agent-card.display-name:本地助手}") String displayName,
            @Value("${harness.agent-card.avatar-url:}") String avatarUrl,
            @Value("${harness.agent-card.purpose:通用智能体服务，可承担开发、分析与文档任务}") String purpose,
            @Value("${harness.agent-card.role-tags:general}") List<String> roleTags,
            @Value("${harness.agent-card.tools:fs.read,web.search}") List<String> tools,
            @Value("${harness.agent-card.approval-policy:WRITE_REQUIRES_APPROVAL}") String approvalPolicy,
            @Value("${harness.agent-card.max-concurrent-tasks:1}") int maxConcurrentTasks) {
        this.displayName = displayName;
        this.avatarUrl = avatarUrl;
        this.purpose = purpose;
        this.roleTags = roleTags;
        this.tools = tools;
        this.approvalPolicy = approvalPolicy;
        this.maxConcurrentTasks = maxConcurrentTasks;
    }

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
}
