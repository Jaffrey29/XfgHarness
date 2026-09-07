package cn.xiaofuge.deepseek.harness.trigger.service.command;

import cn.xiaofuge.deepseek.harness.api.dto.StartWorkflowRequestDTO;
import cn.xiaofuge.deepseek.harness.api.dto.WorkflowEventResponseDTO;
import cn.xiaofuge.deepseek.harness.api.dto.WorkflowStartResponseDTO;
import cn.xiaofuge.deepseek.harness.api.IWorkflowCommandApi;
import cn.xiaofuge.deepseek.harness.api.response.Response;
import cn.xiaofuge.deepseek.harness.cases.workflow.IWorkflowCase;
import java.util.function.Consumer;
import org.springframework.stereotype.Service;

@Service
public class WorkflowCommandApi implements IWorkflowCommandApi {

    private final IWorkflowCase workflowCase;

    public WorkflowCommandApi(IWorkflowCase workflowCase) {
        this.workflowCase = workflowCase;
    }

    public Response<WorkflowStartResponseDTO> start(StartWorkflowRequestDTO request) {
        return execute(() -> workflowCase.start(request));
    }

    public Response<WorkflowStartResponseDTO> startStreaming(
            StartWorkflowRequestDTO request,
            Consumer<WorkflowEventResponseDTO> eventSink
    ) {
        return execute(() -> workflowCase.startStreaming(request, eventSink));
    }

    public Response<Void> cancel(String runId, String reason) {
        return execute(() -> {
            workflowCase.cancel(runId, reason);
            return null;
        });
    }

    private <T> Response<T> execute(java.util.function.Supplier<T> action) {
        try {
            return Response.success(action.get());
        } catch (IllegalArgumentException exception) {
            return Response.invalidArgument(exception.getMessage());
        } catch (RuntimeException exception) {
            return Response.internalError(exception.getMessage());
        }
    }
}
