package cn.walioffice.deepseek.harness.domain.session;

import cn.walioffice.deepseek.harness.domain.task.execution.model.valobj.RuntimeExecutionPlanVO;
import cn.walioffice.deepseek.harness.domain.session.registry.model.valobj.SessionOpenedResultVO;

/**
 * Owns session aggregate lifecycle behavior.
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public interface IHarnessSessionRegistryService {

    SessionOpenedResultVO openQueuedSession(RuntimeExecutionPlanVO plan);
}
