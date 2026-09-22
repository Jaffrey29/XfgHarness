package cn.xiaofuge.deepseek.harness.api;

import cn.xiaofuge.deepseek.harness.api.dto.RuntimeQuestionResponseDTO;
import cn.xiaofuge.deepseek.harness.api.dto.SubmitUserAnswerRequestDTO;
import cn.xiaofuge.deepseek.harness.api.dto.SubmitUserAnswerResponseDTO;
import cn.xiaofuge.deepseek.harness.api.response.Response;
import java.util.List;

/**
 * 运行期用户问答 API：查询 {@code ask_user_question} 挂起的待回答问题，并提交用户回答。
 */
public interface IRuntimeQuestionApi {

    Response<List<RuntimeQuestionResponseDTO>> listPending();

    Response<SubmitUserAnswerResponseDTO> submitAnswer(
            String questionId,
            SubmitUserAnswerRequestDTO request
    );
}
