package cn.xiaofuge.deepseek.harness.trigger.service.command;

import cn.xiaofuge.deepseek.harness.api.IRuntimeQuestionApi;
import cn.xiaofuge.deepseek.harness.api.dto.RuntimeQuestionResponseDTO;
import cn.xiaofuge.deepseek.harness.api.dto.SubmitUserAnswerRequestDTO;
import cn.xiaofuge.deepseek.harness.api.dto.SubmitUserAnswerResponseDTO;
import cn.xiaofuge.deepseek.harness.api.response.Response;
import cn.xiaofuge.deepseek.harness.cases.question.IRuntimeQuestionCase;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 运行期用户问答 API 适配：统一异常归一化为 Response 码。
 */
@Service
public class RuntimeQuestionApi implements IRuntimeQuestionApi {

    private final IRuntimeQuestionCase runtimeQuestionCase;

    public RuntimeQuestionApi(IRuntimeQuestionCase runtimeQuestionCase) {
        this.runtimeQuestionCase = runtimeQuestionCase;
    }

    public Response<List<RuntimeQuestionResponseDTO>> listPending() {
        return execute(runtimeQuestionCase::listPending);
    }

    public Response<SubmitUserAnswerResponseDTO> submitAnswer(
            String questionId,
            SubmitUserAnswerRequestDTO request
    ) {
        return execute(() -> runtimeQuestionCase.submitAnswer(questionId, request));
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
