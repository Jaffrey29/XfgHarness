package cn.xiaofuge.deepseek.harness.domain.tool.jobs;

import cn.xiaofuge.deepseek.harness.domain.jobs.IJobRegistryService;
import cn.xiaofuge.deepseek.harness.domain.jobs.model.valobj.JobSnapshot;
import cn.xiaofuge.deepseek.harness.domain.model.valobj.TextBlock;
import cn.xiaofuge.deepseek.harness.domain.model.entity.ToolDefinition;
import cn.xiaofuge.deepseek.harness.domain.model.entity.ToolExecutionResult;
import cn.xiaofuge.deepseek.harness.domain.model.entity.ToolRunContext;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * {@code job_output} 工具：读取某个后台任务的状态与输出。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public class JobOutputTool implements ToolDefinition {

    private static final int MAX_OUTPUT = 20_000;

    private final IJobRegistryService jobs;
    private final String sessionId;

    /**
     * 处理Job Output Tool相关逻辑。
     */
    public JobOutputTool(IJobRegistryService jobs, String sessionId) {
        this.jobs = jobs;
        this.sessionId = sessionId;
    }

    @Override public String name() { return "job_output"; }

    @Override
    public String description() {
        return "Read the status and captured output of a background job by id.";
    }

    @Override
    public Map<String, Object> parameters() {
        return Map.of(
                "type", "object",
                "additionalProperties", false,
                "properties", Map.of(
                        "job_id", Map.of("type", "string", "description", "The job id returned when the job was started.")
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
                        "output", Map.of("type", "string")
                )
        );
    }

    @Override
    @SuppressWarnings("unchecked")
    public CompletableFuture<ToolExecutionResult> execute(Object args, ToolRunContext ctx) {
        Map<String, Object> argMap = args instanceof Map ? (Map<String, Object>) args : Map.of();
        String id = argMap.get("job_id") == null ? "" : String.valueOf(argMap.get("job_id")).trim();
        if (id.isEmpty()) {
            return CompletableFuture.completedFuture(
                    ToolExecutionResult.fail("job_id parameter is required", "MISSING_PARAM"));
        }
        Optional<JobSnapshot> job = jobs.read(id, sessionId);
        if (job.isEmpty()) {
            return CompletableFuture.completedFuture(
                    ToolExecutionResult.fail("job not found: " + id, "JOB_NOT_FOUND"));
        }
        JobSnapshot j = job.get();
        StringBuilder sb = new StringBuilder();
        sb.append("Job ").append(j.id()).append(" [").append(j.status()).append("]\n");
        if (j.detail() != null) sb.append("Detail: ").append(j.detail()).append("\n");
        String output = j.output();
        if (output != null) {
            if (output.length() > MAX_OUTPUT) {
                output = output.substring(output.length() - MAX_OUTPUT)
                        + "\n... [truncated, " + j.output().length() + " chars total]";
            }
            sb.append("--- output ---\n").append(output);
        }
        return CompletableFuture.completedFuture(
                ToolExecutionResult.ok(List.of(new TextBlock(sb.toString()))));
    }

    @Override
    public boolean isConcurrencySafe(Object args) { return true; }
}
