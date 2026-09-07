package cn.xiaofuge.deepseek.harness.cases.workflow;

import cn.xiaofuge.deepseek.harness.api.dto.StartWorkflowRequestDTO;
import cn.xiaofuge.deepseek.harness.api.dto.WorkflowEventResponseDTO;
import cn.xiaofuge.deepseek.harness.api.dto.WorkflowStartResponseDTO;
import cn.xiaofuge.deepseek.harness.domain.workflow.model.valobj.WorkflowEventVO;
import java.util.function.Consumer;

public interface IWorkflowCase {

    WorkflowStartResponseDTO start(StartWorkflowRequestDTO request);

    WorkflowStartResponseDTO startStreaming(StartWorkflowRequestDTO request, Consumer<WorkflowEventResponseDTO> eventSink);

    void cancel(String runId, String reason);
}
