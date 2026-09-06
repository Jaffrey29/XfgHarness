package cn.xiaofuge.deepseek.harness.domain.tool.jobs;

import cn.xiaofuge.deepseek.harness.domain.jobs.IJobRegistryService;
import cn.xiaofuge.deepseek.harness.domain.jobs.model.valobj.JobSnapshot;
import cn.xiaofuge.deepseek.harness.domain.model.valobj.TextBlock;
import cn.xiaofuge.deepseek.harness.domain.model.entity.ToolDefinition;
import cn.xiaofuge.deepseek.harness.domain.model.entity.ToolExecutionResult;
import cn.xiaofuge.deepseek.harness.domain.model.entity.ToolRunContext;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * {@code job_list} 工具：列出当前会话的后台任务。
 * Mirrors TS {@code tool-jobs}.
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public class JobListTool implements ToolDefinition {

    private final IJobRegistryService jobs;
    private final String sessionId;

    public JobListTool(IJobRegistryService jobs, String sessionId) {
        this.jobs = jobs;
        this.sessionId = sessionId;
    }

    @Override public String name() { return "job_list"; }

    @Override
    public String description() {
        return "List background jobs owned by the current session (running, finished, failed, killed).";
    }

    @Override
    public Map<String, Object> parameters() {
        return Map.of("type", "object", "additionalProperties", false, "properties", Map.of());
    }

    @Override
    public Map<String, Object> outputSchema() {
        return Map.of(
                "type", "object",
                "additionalProperties", true,
                "properties", Map.of(
                        "jobs", Map.of("type", "array"),
                        "count", Map.of("type", "integer")
                )
        );
    }

    @Override
    public CompletableFuture<ToolExecutionResult> execute(Object args, ToolRunContext ctx) {
        List<JobSnapshot> list = jobs.list(sessionId);
        if (list.isEmpty()) {
            return CompletableFuture.completedFuture(
                    ToolExecutionResult.ok(List.of(new TextBlock("No background jobs for this session."))));
        }
        StringBuilder sb = new StringBuilder("Background jobs:\n");
        for (JobSnapshot j : list) {
            sb.append("- ").append(j.id())
              .append(" [").append(j.status()).append("] ")
              .append(j.label() == null ? "" : j.label())
              .append(" (started ").append(j.startedAt()).append(")\n");
        }
        return CompletableFuture.completedFuture(
                ToolExecutionResult.ok(List.of(new TextBlock(sb.toString()))));
    }

    @Override
    public boolean isConcurrencySafe(Object args) { return true; }
}
