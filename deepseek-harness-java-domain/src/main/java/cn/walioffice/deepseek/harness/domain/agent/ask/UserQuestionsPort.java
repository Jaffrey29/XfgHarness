package cn.walioffice.deepseek.harness.domain.agent.ask;

import java.util.List;
import java.util.Optional;

/**
 * Port for user-question interaction.
 * <p>
 * Mirrors TS {@code ctx.userQuestions}: the agent calls {@link #ask} to
 * pause and wait for a human response. The infrastructure layer provides
 * an implementation (HTTP blocking, WebSocket, polling, etc.).
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public interface UserQuestionsPort {

    /**
     * Ask the user one or more questions and block until answers arrive.
     *
     * @param request the questions to ask
     * @return the user's answers
     */
    UserAnswer ask(UserQuestionRequest request);

    /** Whether a user-questions channel is currently available. */
    boolean isAvailable();

    // ── Types ───────────────────────────────────────────────────

    /** One question to ask. */
    record Question(
            String id,
            String question,
            String header,
            List<QuestionOption> options,
            boolean multiSelect
    ) {
        public Question {
            if (id == null || id.isBlank()) throw new IllegalArgumentException("question id required");
            if (question == null || question.isBlank()) throw new IllegalArgumentException("question text required");
        }
    }

    /** One selectable option. */
    record QuestionOption(
            String label,
            String description
    ) {}

    /** The request carrying one or more questions. */
    record UserQuestionRequest(
            List<Question> questions,
            String agentId
    ) {}

    /** One answer for a question. */
    record Answer(
            String id,
            List<String> selected,
            String custom
    ) {}

    /** The response from the user. */
    record UserAnswer(
            List<Answer> answers
    ) {}
}
