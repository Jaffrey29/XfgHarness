package cn.xiaofuge.deepseek.harness.trigger.service.stream;

import cn.xiaofuge.deepseek.harness.api.IWorkflowCommandApi;
import cn.xiaofuge.deepseek.harness.api.dto.StartWorkflowRequestDTO;
import cn.xiaofuge.deepseek.harness.api.dto.WorkflowEventResponseDTO;
import cn.xiaofuge.deepseek.harness.api.stream.IWorkflowStreamApi;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class WorkflowStreamApi implements IWorkflowStreamApi {

    private final IWorkflowCommandApi workflowCommandApi;
    private final ObjectMapper objectMapper;

    public WorkflowStreamApi(IWorkflowCommandApi workflowCommandApi, ObjectMapper objectMapper) {
        this.workflowCommandApi = workflowCommandApi;
        this.objectMapper = objectMapper;
    }

    @Override
    public SseEmitter stream(StartWorkflowRequestDTO request) {
        SseEmitter emitter = new SseEmitter(300_000L);
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
}
