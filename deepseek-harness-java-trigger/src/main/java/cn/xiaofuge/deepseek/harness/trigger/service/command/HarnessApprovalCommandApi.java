package cn.xiaofuge.deepseek.harness.trigger.service.command;

import cn.xiaofuge.deepseek.harness.api.dto.ApprovalActionResponseDTO;
import cn.xiaofuge.deepseek.harness.api.IHarnessApprovalCommandApi;
import cn.xiaofuge.deepseek.harness.api.response.Response;
import cn.xiaofuge.deepseek.harness.cases.task.IApprovalCommandCase;
import org.springframework.stereotype.Service;

@Service
public class HarnessApprovalCommandApi implements IHarnessApprovalCommandApi {
    private final IApprovalCommandCase approvalCommandCase;

    public HarnessApprovalCommandApi(IApprovalCommandCase approvalCommandCase) {
        this.approvalCommandCase = approvalCommandCase;
    }

    public Response<ApprovalActionResponseDTO> approve(String sessionId) {
        return execute(() -> {
            var result = approvalCommandCase.approve(sessionId);
            return new ApprovalActionResponseDTO(
                    result.sessionId(), result.status(), result.message(), result.operatedAt()
            );
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
