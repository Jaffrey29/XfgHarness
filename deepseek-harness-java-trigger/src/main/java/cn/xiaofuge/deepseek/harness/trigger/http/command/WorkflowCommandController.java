package cn.xiaofuge.deepseek.harness.trigger.http.command;

import cn.xiaofuge.deepseek.harness.domain.workflow.IWorkflowService;
import cn.xiaofuge.deepseek.harness.domain.workflow.model.entity.WorkflowRun;
import cn.xiaofuge.deepseek.harness.domain.workflow.model.entity.WorkflowStartRequest;
import cn.xiaofuge.deepseek.harness.domain.workflow.model.valobj.WorkflowMeta;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * WorkflowCommandController 负责 HTTP 协议接入、参数转换和应用用例调用。
 * <p>调用流程：Controller → API 适配器 → 用例/领域命令服务 → 返回响应。</p>
 * <p>示例：POST 请求携带 JSON，命令服务更新状态后返回最新视图。</p>
 */
@RestController
@RequestMapping("/api/workflow")
public class WorkflowCommandController {

    private final IWorkflowService workflowService;
    private final ObjectMapper objectMapper;

    /**
     * 处理工作流 命令 Controller。
     * 流程：解析请求参数 → 调用应用门面 → 返回响应。
     * @param workflowService 工作流 Service参数。
     * @param objectMapper object Mapper参数。
     * @return 方法执行结果。
     */
    public WorkflowCommandController(IWorkflowService workflowService, ObjectMapper objectMapper) {
        this.workflowService = workflowService;
        this.objectMapper = objectMapper;
    }

    /**
     * StartWorkflowRequest 负责 HTTP 协议接入、参数转换和应用用例调用。
     * <p>用途：接收启动工作流请求体。</p>
     * <p>示例：{@code {"script":"run.sh","args":{"env":"dev"}}}。</p>
     */
    public record StartWorkflowRequest(
            String script,
            String name,
            String parentCwd,
            Map<String, Object> args
    ) {}

    /**
     * 启动目标对象。
     * 流程：解析请求参数 → 调用应用门面 → 返回响应。
     * @param request 请求对象。
     * @return 方法执行结果。
     */
    @PostMapping("/start")
    public ResponseEntity<Map<String, Object>> start(@RequestBody StartWorkflowRequest request) {
        var meta = new WorkflowMeta(
                request.name() != null ? request.name() : "adhoc-workflow",
                null, java.util.Optional.empty(), java.util.List.of()
        );
        var startRequest = new WorkflowStartRequest(
                request.script(),
                meta,
                Optional.ofNullable(request.args()),
                Optional.empty(),
                Optional.empty(),
                null,
                request.parentCwd()
        );
        WorkflowRun run = workflowService.start(startRequest);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("runId", run.id());
        response.put("status", "started");
        return ResponseEntity.ok(response);
    }

    /**
     * 处理流。
     * 流程：解析请求参数 → 调用应用门面 → 返回响应。
     * @param request 请求对象。
     * @return 方法执行结果。
     */
    @PostMapping("/{runId}/cancel")
    public ResponseEntity<Map<String, Object>> cancel(
            @PathVariable("runId") String runId,
            @RequestParam(value = "reason", required = false) String reason
    ) {
        workflowService.cancel(runId, reason);
        return ResponseEntity.ok(Map.of("runId", runId, "status", "cancelled"));
    }

    /**
     * 启动工作流并建立 SSE 输出通道。
     * <p>流程：解析请求体 → 构造 WorkflowStartRequest → 启动 WorkflowRun → 推送完成或错误事件。</p>
     * <p>示例：POST {@code /api/workflow/stream}，脚本执行完成后前端收到 done 事件。</p>
     *
     * @param request 工作流启动请求。
     * @return SSE 输出通道。
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@RequestBody StartWorkflowRequest request) {
        SseEmitter emitter = new SseEmitter(300_000L);
        var meta = new WorkflowMeta(
                request.name() != null ? request.name() : "adhoc-workflow",
                null, java.util.Optional.empty(), java.util.List.of()
        );
        var startRequest = new WorkflowStartRequest(
                request.script(), meta,
                Optional.ofNullable(request.args()), Optional.empty(),
                Optional.empty(), null, request.parentCwd()
        );
        WorkflowRun run = workflowService.start(startRequest);
        run.result().whenComplete((result, error) -> {
            try {
                if (error != null) {
                    emitter.send(SseEmitter.event().name("error").data(error.getMessage()));
                } else {
                    Map<String, Object> data = new LinkedHashMap<>();
                    data.put("runId", run.id());
                    data.put("status", result.stopReason() != null ? result.stopReason().toString() : "COMPLETED");
                    data.put("output", result.value() != null ? result.value().toString() : "");
                    emitter.send(SseEmitter.event().name("done").data(objectMapper.writeValueAsString(data)));
                }
                emitter.complete();
            } catch (Exception ignored) {
            }
        });
        return emitter;
    }
}
