package cn.xiaofuge.deepseek.harness.domain.task.approval.service;

import cn.xiaofuge.deepseek.harness.domain.session.ISessionLogService;
import cn.xiaofuge.deepseek.harness.domain.session.registry.adapter.repository.IHarnessSessionRepository;
import cn.xiaofuge.deepseek.harness.domain.session.registry.model.aggregate.HarnessSessionAggregate;
import cn.xiaofuge.deepseek.harness.domain.shared.model.valobj.HarnessStatusEnumVO;
import cn.xiaofuge.deepseek.harness.domain.task.IApprovalCommandService;
import cn.xiaofuge.deepseek.harness.domain.task.IHarnessExecutionService;
import cn.xiaofuge.deepseek.harness.domain.task.approval.model.valobj.ApprovalActionVO;
import cn.xiaofuge.deepseek.harness.domain.task.execution.model.valobj.HarnessExecutionResultVO;
import java.time.Instant;

/** Executes approval actions and resumes the task flow. */
public class ApprovalCommandService implements IApprovalCommandService {

    private final IHarnessSessionRepository harnessSessionRepository;
    private final ISessionLogService sessionLogService;
    private final IHarnessExecutionService harnessExecutionService;

    public ApprovalCommandService(
            IHarnessSessionRepository harnessSessionRepository,
            ISessionLogService sessionLogService,
            IHarnessExecutionService harnessExecutionService
    ) {
        this.harnessSessionRepository = harnessSessionRepository;
        this.sessionLogService = sessionLogService;
        this.harnessExecutionService = harnessExecutionService;
    }

    @Override
    public ApprovalActionVO approve(String sessionId) {
        HarnessSessionAggregate session = harnessSessionRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("session not found: " + sessionId));
        if (session.getStatus() != HarnessStatusEnumVO.PENDING_APPROVAL) {
            throw new IllegalStateException("session is not pending approval: " + sessionId);
        }
        session.changeStatus(HarnessStatusEnumVO.QUEUED);
        harnessSessionRepository.save(session);
        sessionLogService.recordApprovalAccepted(sessionId);
        HarnessExecutionResultVO executionResult = harnessExecutionService.executeSession(sessionId);
        Instant operatedAt = Instant.now();
        return new ApprovalActionVO(sessionId, executionResult.status().name(), executionResult.assistantMessage(), operatedAt);
    }
}
