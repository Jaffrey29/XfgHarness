package cn.walioffice.deepseek.harness.trigger.http.command;

import cn.walioffice.deepseek.harness.domain.workflow.IWorkflowService;
import cn.walioffice.deepseek.harness.domain.workflow.model.entity.WorkflowRun;
import cn.walioffice.deepseek.harness.domain.workflow.model.entity.WorkflowStartRequest;
import cn.walioffice.deepseek.harness.domain.workflow.model.valobj.WorkflowMeta;
import cn.walioffice.deepseek.harness.domain.workflow.model.valobj.WorkflowResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/workflow")
public class WorkflowCommandController {

    private final IWorkflowService workflowService;
    private final ObjectMapper objectMapper;

    public WorkflowCommandController(IWorkflowService workflowService, ObjectMapper objectMapper) {
        this.workflowService = workflowService;
        this.objectMapper = objectMapper;
    }

    public record StartWorkflowRequest(
            String script,
            String name,
            String parentCwd,
            Map<String, Object> args
    ) {}

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

    @PostMapping("/{runId}/cancel")
    public ResponseEntity<Map<String, Object>> cancel(
            @PathVariable("runId") String runId,
            @RequestParam(value = "reason", required = false) String reason
    ) {
        workflowService.cancel(runId, reason);
        return ResponseEntity.ok(Map.of("runId", runId, "status", "cancelled"));
    }

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
