package cn.xiaofuge.deepseek.harness.cases.question;

import cn.xiaofuge.deepseek.harness.api.dto.RuntimeQuestionResponseDTO;
import cn.xiaofuge.deepseek.harness.api.dto.SubmitUserAnswerRequestDTO;
import cn.xiaofuge.deepseek.harness.api.dto.SubmitUserAnswerResponseDTO;
import cn.xiaofuge.deepseek.harness.cases.support.CaseGuard;
import cn.xiaofuge.deepseek.harness.domain.agent.contract.outbound.UserQuestionGateway;
import cn.xiaofuge.deepseek.harness.domain.agent.service.run.InMemoryUserQuestionGateway;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 运行期用户问答编排实现。
 * <p>查询：读取网关中全部待回答请求并转 DTO；提交：校验回答非空后送达挂起的 Agent。</p>
 * <p>数据示例：提交 {@code answers=[{id:"q1", selected:["A"]}] } 成功返回
 * {@code submitted=true}；questionId 不存在（已超时/已回答）返回 {@code submitted=false}。</p>
 */
@Service
public class RuntimeQuestionCaseImpl implements IRuntimeQuestionCase {

    private static final Logger log = LoggerFactory.getLogger(RuntimeQuestionCaseImpl.class);

    private final InMemoryUserQuestionGateway questionGateway;

    public RuntimeQuestionCaseImpl(UserQuestionGateway userQuestionGateway) {
        if (!(userQuestionGateway instanceof InMemoryUserQuestionGateway gateway)) {
            throw new IllegalStateException(
                    "runtime question endpoints require " + InMemoryUserQuestionGateway.class.getSimpleName());
        }
        this.questionGateway = gateway;
    }

    @Override
    public List<RuntimeQuestionResponseDTO> listPending() {
        return questionGateway.getPendingQuestions().stream()
                .map(item -> new RuntimeQuestionResponseDTO(
                        item.requestId(),
                        item.agentId(),
                        item.questions().stream()
                                .map(RuntimeQuestionCaseImpl::toQuestionItem)
                                .toList(),
                        Instant.ofEpochMilli(item.createdAt()).toString()
                ))
                .toList();
    }

    @Override
    public SubmitUserAnswerResponseDTO submitAnswer(String questionId, SubmitUserAnswerRequestDTO request) {
        CaseGuard.requireText(questionId, "questionId must not be blank");
        if (request == null || request.answers() == null || request.answers().isEmpty()) {
            throw new IllegalArgumentException("answers must not be empty");
        }
        List<UserQuestionGateway.Answer> answers = new ArrayList<>();
        for (SubmitUserAnswerRequestDTO.AnswerItemDTO item : request.answers()) {
            CaseGuard.requireText(item.id(), "answer id must not be blank");
            List<String> selected = item.selected() == null ? List.of() : item.selected();
            String custom = item.custom() == null ? null : item.custom().trim();
            boolean hasCustom = custom != null && !custom.isEmpty();
            if (selected.isEmpty() && !hasCustom) {
                throw new IllegalArgumentException("answer " + item.id() + " requires selected options or custom text");
            }
            answers.add(new UserQuestionGateway.Answer(item.id(), List.copyOf(selected), hasCustom ? custom : null));
        }

        boolean submitted = questionGateway.submitAnswer(questionId, new UserQuestionGateway.UserAnswer(answers));
        if (submitted) {
            log.info("[Question] 用户回答已提交，questionId={} answers={}", questionId, answers.size());
            return new SubmitUserAnswerResponseDTO(true, null);
        }
        log.warn("[Question] 用户回答提交失败（问题不存在或已处理），questionId={}", questionId);
        return new SubmitUserAnswerResponseDTO(false, "question not found or already answered");
    }

    private static RuntimeQuestionResponseDTO.RuntimeQuestionItemDTO toQuestionItem(
            UserQuestionGateway.Question question
    ) {
        return new RuntimeQuestionResponseDTO.RuntimeQuestionItemDTO(
                question.id(),
                question.question(),
                question.header(),
                question.options() == null ? List.of() : question.options().stream()
                        .map(option -> new RuntimeQuestionResponseDTO.RuntimeQuestionOptionDTO(
                                option.label(), option.description()))
                        .toList(),
                question.multiSelect()
        );
    }
}
