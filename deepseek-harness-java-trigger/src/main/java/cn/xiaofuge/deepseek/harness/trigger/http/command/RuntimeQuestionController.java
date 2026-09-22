package cn.xiaofuge.deepseek.harness.trigger.http.command;

import cn.xiaofuge.deepseek.harness.api.IRuntimeQuestionApi;
import cn.xiaofuge.deepseek.harness.api.dto.RuntimeQuestionResponseDTO;
import cn.xiaofuge.deepseek.harness.api.dto.SubmitUserAnswerRequestDTO;
import cn.xiaofuge.deepseek.harness.api.dto.SubmitUserAnswerResponseDTO;
import cn.xiaofuge.deepseek.harness.api.response.Response;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 运行期用户问答 REST 端点。
 * <p>
 * agent 执行到 {@code ask_user_question} 工具时挂起等待；前端轮询
 * {@code GET /pending} 拿到待回答问题渲染问答卡，用户选择/输入后
 * {@code POST /{questionId}/answer} 提交回答，挂起的 agent 随即继续。
 * <p>调用流程：Controller → API 适配器 → 用例 → InMemoryUserQuestionGateway → 挂起的 Agent。</p>
 */
@RestController
@RequestMapping("/api/harness/questions/runtime")
public class RuntimeQuestionController {

    private final IRuntimeQuestionApi runtimeQuestionApi;

    public RuntimeQuestionController(IRuntimeQuestionApi runtimeQuestionApi) {
        this.runtimeQuestionApi = runtimeQuestionApi;
    }

    /** 当前待回答的用户问题列表。 */
    @GetMapping("/pending")
    public Response<List<RuntimeQuestionResponseDTO>> listPending() {
        return runtimeQuestionApi.listPending();
    }

    /**
     * 提交用户回答。
     * <p>body: {@code {"answers":[{"id":"q1","selected":["选项A"],"custom":"自由文本"}]}}</p>
     */
    @PostMapping("/{questionId}/answer")
    public Response<SubmitUserAnswerResponseDTO> submitAnswer(
            @PathVariable("questionId") String questionId,
            @RequestBody SubmitUserAnswerRequestDTO request
    ) {
        return runtimeQuestionApi.submitAnswer(questionId, request);
    }
}
