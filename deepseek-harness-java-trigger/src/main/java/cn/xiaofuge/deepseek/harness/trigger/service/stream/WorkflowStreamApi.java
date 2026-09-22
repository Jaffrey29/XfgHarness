package cn.xiaofuge.deepseek.harness.trigger.service.stream;

import cn.xiaofuge.deepseek.harness.api.IWorkflowCommandApi;
import cn.xiaofuge.deepseek.harness.api.dto.StartWorkflowRequestDTO;
import cn.xiaofuge.deepseek.harness.api.dto.WorkflowEventResponseDTO;
import cn.xiaofuge.deepseek.harness.api.stream.IWorkflowStreamApi;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class WorkflowStreamApi implements IWorkflowStreamApi {

    /** SSE 心跳间隔：防止代理/网关因空闲超时掐断长工作流连接 */
    private static final long HEARTBEAT_INTERVAL_MS = 15_000L;

    private final IWorkflowCommandApi workflowCommandApi;
    private final ObjectMapper objectMapper;
    private final ScheduledExecutorService heartbeatExecutor;

    public WorkflowStreamApi(IWorkflowCommandApi workflowCommandApi, ObjectMapper objectMapper) {
        this.workflowCommandApi = workflowCommandApi;
        this.objectMapper = objectMapper;
        this.heartbeatExecutor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "workflow-stream-heartbeat");
            thread.setDaemon(true);
            return thread;
        });
    }

    @Override
    public SseEmitter stream(StartWorkflowRequestDTO request) {
        SseEmitter emitter = new SseEmitter(300_000L);
        AtomicBoolean heartbeatStopped = new AtomicBoolean(false);
        ScheduledFuture<?> heartbeat = startHeartbeat(emitter, heartbeatStopped);
        Runnable stopHeartbeat = () -> {
            heartbeatStopped.set(true);
            heartbeat.cancel(false);
        };
        emitter.onCompletion(stopHeartbeat);
        emitter.onTimeout(stopHeartbeat);
        emitter.onError(error -> stopHeartbeat.run());
        workflowCommandApi.startStreaming(request, (WorkflowEventResponseDTO event) -> {
            try {
                emitter.send(SseEmitter.event()
                        .name(event.event())
                        .data(objectMapper.writeValueAsString(event)));
                if ("done".equals(event.event()) || "error".equals(event.event())) {
                    emitter.complete();
                }
            } catch (Exception ignored) {
            }
        });
        return emitter;
    }

    /** 每 15s 发送一条 SSE 注释帧（comment keepalive），不产生业务事件 */
    private ScheduledFuture<?> startHeartbeat(SseEmitter emitter, AtomicBoolean stopped) {
        return heartbeatExecutor.scheduleAtFixedRate(() -> {
            if (stopped.get()) return;
            try {
                emitter.send(SseEmitter.event().comment("keepalive"));
            } catch (Exception exception) {
                stopped.set(true);
            }
        }, HEARTBEAT_INTERVAL_MS, HEARTBEAT_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }
}
