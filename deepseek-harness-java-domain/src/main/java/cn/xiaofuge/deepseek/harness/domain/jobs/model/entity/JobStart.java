package cn.xiaofuge.deepseek.harness.domain.jobs.model.entity;

import cn.xiaofuge.deepseek.harness.domain.jobs.model.valobj.JobKind;
import cn.xiaofuge.deepseek.harness.domain.jobs.model.valobj.JobOutcome;

/**
 * Producer declaration passed to the registry's {@code start}.
 *
 * <p>调用流程：应用领域规则 → 更新聚合或值对象 → 发布事件/返回结果。</p>
 * <p>示例：领域对象只暴露业务语言方法，不直接依赖基础设施。</p>
 * @author 小傅哥
 * @website bugstack.cn
 */
public record JobStart(
        JobKind kind,
        String label,
        String ownerSessionId,
        java.util.concurrent.Callable<JobOutcome> work
) {}
