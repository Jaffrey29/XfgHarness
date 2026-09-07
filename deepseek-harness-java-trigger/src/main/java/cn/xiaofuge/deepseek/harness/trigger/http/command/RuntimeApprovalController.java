package cn.xiaofuge.deepseek.harness.trigger.http.command;

import cn.xiaofuge.deepseek.harness.domain.agent.adapter.port.IRuntimeApprovalBroker;
import cn.xiaofuge.deepseek.harness.domain.agent.adapter.port.IRuntimeApprovalBroker.PendingApprovalView;
import cn.xiaofuge.deepseek.harness.domain.tool.service.RuntimeApprovalGate.ApprovalVerdict;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 运行期工具审批 REST 端点。
 * <p>
 * agent 执行到需审批工具时实时挂起；前端轮询 {@code GET /runtime/pending}
 * 拿到待决条目，用户选择后 {@code POST /runtime/{approvalId}/resolve} 提交裁决，
 * 挂起的 agent 随即继续。对齐原版 TS 的 allow-once / allow-session / deny / cancel。
 * <p>调用流程：Controller → API 适配器 → 用例/领域命令服务 → 返回响应。</p>
 * <p>示例：POST 请求携带 JSON，命令服务更新状态后返回最新视图。</p>
 */
@RestController
@RequestMapping("/api/harness/approvals/runtime")
public class RuntimeApprovalController {

    private final IRuntimeApprovalBroker broker;

    /**
     * 运行time Approval Controller。
     */
    public RuntimeApprovalController(IRuntimeApprovalBroker broker) {
        this.broker = broker;
    }

    /** 当前待决的运行期审批列表。 */
    @GetMapping("/pending")
    public List<PendingApprovalView> listPending() {
        return broker.listPending();
    }

    /**
     * 提交裁决。
     * <p>body: {@code {"verdict": "ALLOW_ONCE" | "ALLOW_SESSION" | "DENY" | "CANCEL"}}
     */
    @PostMapping("/{approvalId}/resolve")
    public ResponseEntity<Map<String, Object>> resolve(
            @PathVariable String approvalId,
            @RequestBody Map<String, String> body
    ) {
        String raw = body.getOrDefault("verdict", "DENY");
        ApprovalVerdict verdict;
        try {
            verdict = ApprovalVerdict.valueOf(raw.trim().toUpperCase().replace('-', '_'));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "resolved", false,
                    "error", "unknown verdict: " + raw));
        }
        boolean accepted = broker.resolve(approvalId, verdict);
        if (!accepted) {
            return ResponseEntity.status(404).body(Map.of(
                    "resolved", false,
                    "error", "approval not found or already resolved: " + approvalId));
        }
        return ResponseEntity.ok(Map.of("resolved", true, "verdict", verdict.name()));
    }
}
