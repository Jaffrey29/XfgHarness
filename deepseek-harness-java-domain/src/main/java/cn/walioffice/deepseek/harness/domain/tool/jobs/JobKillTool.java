package cn.walioffice.deepseek.harness.domain.tool.jobs;

import cn.walioffice.deepseek.harness.domain.jobs.IJobRegistryService;
import cn.walioffice.deepseek.harness.domain.llm.model.valobj.TextBlock;
import cn.walioffice.deepseek.harness.domain.tool.model.entity.ToolDefinition;
import cn.walioffice.deepseek.harness.domain.tool.model.entity.ToolExecutionResult;
import cn.walioffice.deepseek.harness.domain.tool.model.entity.ToolRunContext;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * {@code job_kill} 工具：取消一个仍在运行的后台任务。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public class JobKillTool implements ToolDefinition {

    private final IJobRegistryService jobs;
    private final String sessionId;

    public JobKillTool(IJobRegistryService jobs, String sessionId) {
        this.jobs = jobs;
        this.sessionId = sessionId;
    }

    @Override public String name() { return "job_kill"; }

    @Override
    public String description() {
        return "Cancel a running background job owned by the current session.";
    }

    @Override
    public Map<String, Object> parameters() {
        return Map.of(
                "type", "object",
                "additionalProperties", false,
                "properties", Map.of(
                        "job_id", Map.of("type", "string", "description", "The job id to cancel."),
                        "reason", Map.of("type", "string", "description", "Optional cancellation reason.")
                ),
                "required", List.of("job_id")
        );
    }

    @Override
    public Map<String, Object> outputSchema() {
        return Map.of(
                "type", "object",
                "additionalProperties", true,
                "properties", Map.of(
                        "job_id", Map.of("type", "string"),
                        "killed", Map.of("type", "boolean")
                )
        );
    }

    @Override
    @SuppressWarnings("unchecked")
    public CompletableFuture<ToolExecutionResult> execute(Object args, ToolRunContext ctx) {
        Map<String, Object> argMap = args instanceof Map ? (Map<String, Object>) args : Map.of();
        String id = argMap.get("job_id") == null ? "" : String.valueOf(argMap.get("job_id")).trim();
        String reason = argMap.get("reason") == null ? "cancelled by agent" : String.valueOf(argMap.get("reason"));
        if (id.isEmpty()) {
            return CompletableFuture.completedFuture(
                    ToolExecutionResult.fail("job_id parameter is required", "MISSING_PARAM"));
        }
        boolean killed = jobs.cancel(id, sessionId, reason);
        if (!killed) {
            return CompletableFuture.completedFuture(
                    ToolExecutionResult.fail("job not running or not found: " + id, "JOB_NOT_RUNNING"));
        }
        return CompletableFuture.completedFuture(
                ToolExecutionResult.ok(List.of(new TextBlock("Job " + id + " cancelled (" + reason + ")"))));
    }

    @Override
    public boolean isConcurrencySafe(Object args) { return false; }
}
