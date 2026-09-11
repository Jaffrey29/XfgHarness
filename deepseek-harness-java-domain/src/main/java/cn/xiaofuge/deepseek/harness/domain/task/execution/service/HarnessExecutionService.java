package cn.xiaofuge.deepseek.harness.domain.task.execution.service;

import cn.xiaofuge.deepseek.harness.domain.task.IHarnessExecutionService;
import cn.xiaofuge.deepseek.harness.domain.llm.model.valobj.GenerateOptions;
import cn.xiaofuge.deepseek.harness.domain.model.entity.Message;
import cn.xiaofuge.deepseek.harness.domain.model.entity.MessageSource;
import cn.xiaofuge.deepseek.harness.domain.model.valobj.TextBlock;
import cn.xiaofuge.deepseek.harness.domain.runtime.setting.IModelSettingQueryService;
import cn.xiaofuge.deepseek.harness.domain.runtime.setting.model.valobj.ModelSettingVO;
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
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Executes stored session prompts against the configured model provider.
 *
 * <p>调用流程：提交 → 权限评估 → 审批 → 队列 → 执行 → 状态回写。</p>
 * <p>示例：需要审批的命令先挂起，批准后进入执行队列。</p>
 * @author 小傅哥
 * @website bugstack.cn
 */
@Service
public class HarnessExecutionService implements IHarnessExecutionService {

    private final IModelExecutionPort modelExecutionPort;
    private final IHarnessSessionRepository harnessSessionRepository;
    private final ISessionEventRepository sessionEventRepository;
    private final ISessionLogService sessionLogService;
    private final IModelSettingQueryService modelSettingQueryService;

    /**
     * 初始化任务执行服务。
     * <p>流程：读取队列任务 → 委托模型执行 → 回写执行状态和事件。</p>
     */
    public HarnessExecutionService(
            IModelExecutionPort modelExecutionPort,
            IHarnessSessionRepository harnessSessionRepository,
            ISessionEventRepository sessionEventRepository,
            ISessionLogService sessionLogService,
            IModelSettingQueryService modelSettingQueryService
    ) {
        this.modelExecutionPort = modelExecutionPort;
        this.harnessSessionRepository = harnessSessionRepository;
        this.sessionEventRepository = sessionEventRepository;
        this.sessionLogService = sessionLogService;
        this.modelSettingQueryService = modelSettingQueryService;
    }
    /**
     * 执行Session。
     * 流程：应用领域规则 → 更新领域对象 → 返回结果。
     * @param sessionId 会话 ID。
     * @return 方法执行结果。
     */


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
            ModelExecutionResultVO result = modelExecutionPort.execute(buildOptions(session, prompt));
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
     * 处理resolveLatestUserPrompt提示词。
     * <p>流程：校验入参 → 执行领域动作 → 返回处理结果。</p>
     * @param sessionId 会话ID。
     * @return 领域处理结果。
     */
    private String resolveLatestUserPrompt(String sessionId) {
        return sessionEventRepository.queryBySessionId(sessionId).stream()
                .filter(event -> "user.message".equals(event.eventType()))
                .reduce((previous, current) -> current)
                .map(SessionEventEntity::payload)
                .filter(prompt -> prompt != null && !prompt.isBlank())
                .orElseThrow(() -> new IllegalStateException("no user prompt found for session: " + sessionId));
    }

    private GenerateOptions buildOptions(HarnessSessionAggregate session, String prompt) {
        var setting = modelSettingQueryService.findByProfileCode(session.getProfileCode())
                .filter(ModelSettingVO::enabled)
                .orElseThrow(() -> new IllegalStateException(
                        "model setting is missing or disabled for profile: " + session.getProfileCode()));
        var message = Message.createUser(
                List.of(new TextBlock(prompt)),
                new MessageSource.UserSource()
        );
        return new GenerateOptions(
                setting.providerCode(),
                setting.modelCode(),
                null,
                List.of(message),
                null,
                null,
                null,
                null,
                null,
                session.getSessionId(),
                null,
                setting.baseUrl(),
                setting.apiKeyRef(),
                setting.protocol()
        );
    }

    /**
     * 提取 Failure 消息。
     * 流程：应用领域规则 → 更新领域对象 → 返回结果。
     * @param exception 异常对象。
     * @return 方法执行结果。
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
