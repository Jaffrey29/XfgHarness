package cn.xiaofuge.deepseek.harness.cases.approval;

import cn.xiaofuge.deepseek.harness.api.dto.ResolveRuntimeApprovalRequestDTO;
import cn.xiaofuge.deepseek.harness.api.dto.ResolveRuntimeApprovalResponseDTO;
import cn.xiaofuge.deepseek.harness.api.dto.RuntimeApprovalResponseDTO;
import cn.xiaofuge.deepseek.harness.cases.approval.factory.RuntimeApprovalPipelineFactory;
import cn.xiaofuge.deepseek.harness.cases.approval.factory.RuntimeApprovalResolveInput;
import cn.xiaofuge.deepseek.harness.domain.agent.model.valobj.RuntimeApprovalDecisionVO;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class RuntimeApprovalCaseImpl implements IRuntimeApprovalCase {

    private final RuntimeApprovalPipelineFactory runtimeApprovalPipelineFactory;

    public RuntimeApprovalCaseImpl(RuntimeApprovalPipelineFactory runtimeApprovalPipelineFactory) {
        this.runtimeApprovalPipelineFactory = runtimeApprovalPipelineFactory;
    }

    @Override
    public List<RuntimeApprovalResponseDTO> listPending() {
        return runtimeApprovalPipelineFactory.listPendingPipeline().execute(null);
    }

    @Override
    public ResolveRuntimeApprovalResponseDTO resolve(String approvalId, ResolveRuntimeApprovalRequestDTO request) {
        RuntimeApprovalDecisionVO decision;
        try {
            decision = RuntimeApprovalDecisionVO.valueOf(normalize(request.verdict()));
        } catch (RuntimeException exception) {
            return new ResolveRuntimeApprovalResponseDTO(false, null, "unknown verdict: " + request.verdict());
        }
        return runtimeApprovalPipelineFactory.resolvePipeline().execute(new RuntimeApprovalResolveInput(approvalId, decision));
    }

    private String normalize(String verdict) {
        return (verdict == null ? "DENY" : verdict)
                .trim()
                .toUpperCase()
                .replace('-', '_');
    }
}
