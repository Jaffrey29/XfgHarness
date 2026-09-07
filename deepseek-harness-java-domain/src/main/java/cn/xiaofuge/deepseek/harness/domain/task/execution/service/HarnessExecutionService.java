package cn.xiaofuge.deepseek.harness.domain.task.execution.service;

import cn.xiaofuge.deepseek.harness.domain.task.IHarnessExecutionService;
import cn.xiaofuge.deepseek.harness.domain.task.execution.adapter.port.IModelExecutionPort;
import cn.xiaofuge.deepseek.harness.domain.task.execution.model.valobj.HarnessExecutionResultVO;
import cn.xiaofuge.deepseek.harness.domain.task.execution.model.valobj.ModelExecutionResultVO;
import cn.xiaofuge.deepseek.harness.domain.session.log.adapter.repository.ISessionEventRepository;
import cn.xiaofuge.deepseek.harness.domain.session.log.model.entity.SessionEventEntity;
import cn.xiaofuge.deepseek.harness.domain.session.ISessionLogService;
import cn.xiaofuge.deepseek.harness.domain.session.registry.adapter.repository.IHarnessSessionRepository;
import cn.xiaofuge.deepseek.harness.domain.session.registry.model.aggregate.HarnessSessionAggregate;
import cn.xiaofuge.deepseek.harness.domain.shared.model.valobj.HarnessStatusEnumVO;

import java.time.Instant;
import org.springframework.stereotype.Service;

/**
 * Executes stored session prompts against the configured model provider.
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
@Service
public class HarnessExecutionService implements IHarnessExecutionService {

    private final IModelExecutionPort modelExecutionPort;
    private final IHarnessSessionRepository harnessSessionRepository;
    private final ISessionEventRepository sessionEventRepository;
    private final ISessionLogService sessionLogService;

    /**
     * 处理Harness Execution Service相关逻辑。
     */
    public HarnessExecutionService(
            IModelExecutionPort modelExecutionPort,
            IHarnessSessionRepository harnessSessionRepository,
            ISessionEventRepository sessionEventRepository,
            ISessionLogService sessionLogService
    ) {
        this.modelExecutionPort = modelExecutionPort;
        this.harnessSessionRepository = harnessSessionRepository;
        this.sessionEventRepository = sessionEventRepository;
        this.sessionLogService = sessionLogService;
    }

    @Override
    public HarnessExecutionResultVO executeSession(String sessionId) {
        HarnessSessionAggregate session = harnessSessionRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("session not found: " + sessionId));
        if (session.getStatus() == HarnessStatusEnumVO.PENDING_APPROVAL) {
            throw new IllegalStateException("session is pending approval: " + sessionId);
        }

        String prompt = resolveLatestUserPrompt(sessionId);
        session.changeStatus(HarnessStatusEnumVO.RUNNING);
        harnessSessionRepository.save(session);
        sessionLogService.recordExecutionStarted(sessionId, session.getResolvedModel());

        try {
            ModelExecutionResultVO result = modelExecutionPort.execute(session.getProfileCode(), prompt);
            session.changeStatus(HarnessStatusEnumVO.COMPLETED);
            harnessSessionRepository.save(session);
            sessionLogService.recordExecutionCompleted(sessionId, result.modelCode(), result.content());
            return new HarnessExecutionResultVO(sessionId, session.getStatus(), result.content(), Instant.now());
        } catch (RuntimeException exception) {
            String failureMessage = "模型执行失败：" + extractFailureMessage(exception);
            session.changeStatus(HarnessStatusEnumVO.FAILED);
            harnessSessionRepository.save(session);
            sessionLogService.recordExecutionFailed(sessionId, failureMessage);
            return new HarnessExecutionResultVO(sessionId, session.getStatus(), failureMessage, Instant.now());
        }
    }

    /**
     * 解析e Latest User Prompt。
     */
    private String resolveLatestUserPrompt(String sessionId) {
        return sessionEventRepository.queryBySessionId(sessionId).stream()
                .filter(event -> "user.message".equals(event.eventType()))
                .reduce((previous, current) -> current)
                .map(SessionEventEntity::payload)
                .filter(prompt -> prompt != null && !prompt.isBlank())
                .orElseThrow(() -> new IllegalStateException("no user prompt found for session: " + sessionId));
    }

    /**
     * 处理extract Failure Message相关逻辑。
     */
    private String extractFailureMessage(RuntimeException exception) {
        Throwable current = exception;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        String message = current.getMessage();
        if (message == null || message.isBlank()) {
            return current.getClass().getSimpleName();
        }
        return message;
    }
}
