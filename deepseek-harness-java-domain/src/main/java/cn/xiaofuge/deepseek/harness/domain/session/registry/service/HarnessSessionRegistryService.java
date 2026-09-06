package cn.xiaofuge.deepseek.harness.domain.session.registry.service;

import cn.xiaofuge.deepseek.harness.domain.session.IHarnessSessionRegistryService;
import cn.xiaofuge.deepseek.harness.domain.task.execution.model.valobj.RuntimeExecutionPlanVO;
import cn.xiaofuge.deepseek.harness.domain.session.registry.adapter.repository.IHarnessSessionRepository;
import cn.xiaofuge.deepseek.harness.domain.session.registry.model.aggregate.HarnessSessionAggregate;
import cn.xiaofuge.deepseek.harness.domain.session.registry.model.valobj.SessionOpenedResultVO;
import java.util.UUID;

/**
 * Owns session creation and status persistence.
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public class HarnessSessionRegistryService implements IHarnessSessionRegistryService {

    private final IHarnessSessionRepository harnessSessionRepository;

    public HarnessSessionRegistryService(IHarnessSessionRepository harnessSessionRepository) {
        this.harnessSessionRepository = harnessSessionRepository;
    }

    @Override
    public SessionOpenedResultVO openQueuedSession(RuntimeExecutionPlanVO plan) {
        String sessionId = UUID.randomUUID().toString();
        HarnessSessionAggregate session = new HarnessSessionAggregate(sessionId, plan.workspaceId(), plan.profileCode(), plan.acceptedAt());
        session.prepare(plan.executionMode(), plan.resolvedModel());
        session.changeStatus(plan.status());
        harnessSessionRepository.save(session);
        return new SessionOpenedResultVO(sessionId, plan.resolvedModel(), plan.status(), plan.acceptedAt());
    }
}
