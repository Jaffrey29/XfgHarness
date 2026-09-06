package cn.xiaofuge.deepseek.harness.domain.jobs.model.entity;

import cn.xiaofuge.deepseek.harness.domain.jobs.model.valobj.JobKind;
import cn.xiaofuge.deepseek.harness.domain.jobs.model.valobj.JobOutcome;

/**
 * Producer declaration passed to the registry's {@code start}.
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public record JobStart(
        JobKind kind,
        String label,
        String ownerSessionId,
        java.util.concurrent.Callable<JobOutcome> work
) {}
