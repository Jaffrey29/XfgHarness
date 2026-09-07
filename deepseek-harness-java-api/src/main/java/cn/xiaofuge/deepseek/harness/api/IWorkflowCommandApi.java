package cn.xiaofuge.deepseek.harness.api;

import cn.xiaofuge.deepseek.harness.api.dto.StartWorkflowRequestDTO;
import cn.xiaofuge.deepseek.harness.api.dto.WorkflowEventResponseDTO;
import cn.xiaofuge.deepseek.harness.api.dto.WorkflowStartResponseDTO;
import cn.xiaofuge.deepseek.harness.api.response.Response;
import java.util.function.Consumer;

public interface IWorkflowCommandApi {

    Response<WorkflowStartResponseDTO> start(StartWorkflowRequestDTO request);

    Response<WorkflowStartResponseDTO> startStreaming(
            StartWorkflowRequestDTO request,
            Consumer<WorkflowEventResponseDTO> eventSink
    );

    Response<Void> cancel(String runId, String reason);
}
