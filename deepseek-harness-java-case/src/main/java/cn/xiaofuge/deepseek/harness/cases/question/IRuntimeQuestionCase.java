package cn.xiaofuge.deepseek.harness.cases.question;

import cn.xiaofuge.deepseek.harness.api.dto.RuntimeQuestionResponseDTO;
import cn.xiaofuge.deepseek.harness.api.dto.SubmitUserAnswerRequestDTO;
import cn.xiaofuge.deepseek.harness.api.dto.SubmitUserAnswerResponseDTO;
import java.util.List;

/**
 * 运行期用户问答用例契约。
 * <p>流程：Trigger/前端 → Case → InMemoryUserQuestionGateway → 挂起的 Agent。</p>
 * <p>数据示例：先轮询到 {@code questionId=…, questions=[{id: "q1", options: […]}]}，
 * 再提交 {@code answers=[{id: "q1", selected: ["选项A"]}]} 唤醒 Agent。</p>
 */
public interface IRuntimeQuestionCase {

    List<RuntimeQuestionResponseDTO> listPending();

    SubmitUserAnswerResponseDTO submitAnswer(String questionId, SubmitUserAnswerRequestDTO request);
}
