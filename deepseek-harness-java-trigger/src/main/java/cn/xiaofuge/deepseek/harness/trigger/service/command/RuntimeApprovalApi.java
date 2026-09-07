package cn.xiaofuge.deepseek.harness.trigger.service.command;

import cn.xiaofuge.deepseek.harness.api.dto.ResolveRuntimeApprovalRequestDTO;
import cn.xiaofuge.deepseek.harness.api.dto.ResolveRuntimeApprovalResponseDTO;
import cn.xiaofuge.deepseek.harness.api.dto.RuntimeApprovalResponseDTO;
import cn.xiaofuge.deepseek.harness.api.IRuntimeApprovalApi;
import cn.xiaofuge.deepseek.harness.api.response.Response;
import cn.xiaofuge.deepseek.harness.cases.approval.IRuntimeApprovalCase;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class RuntimeApprovalApi implements IRuntimeApprovalApi {

    private final IRuntimeApprovalCase runtimeApprovalCase;

    public RuntimeApprovalApi(IRuntimeApprovalCase runtimeApprovalCase) {
        this.runtimeApprovalCase = runtimeApprovalCase;
    }

    public Response<List<RuntimeApprovalResponseDTO>> listPending() {
        return execute(runtimeApprovalCase::listPending);
    }

    public Response<ResolveRuntimeApprovalResponseDTO> resolve(
            String approvalId,
            ResolveRuntimeApprovalRequestDTO request
    ) {
        return execute(() -> runtimeApprovalCase.resolve(approvalId, request));
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
