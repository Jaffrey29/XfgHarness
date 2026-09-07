package cn.xiaofuge.deepseek.harness.domain.jobs.model.valobj;

import cn.xiaofuge.deepseek.harness.domain.jobs.model.entity.JobRecord;
import java.time.Instant;

/**
 * Read-only projection of a job, safe to hand to listeners and tools.
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public record JobSnapshot(
        String id,
        String kind,
        String label,
        String ownerSessionId,
        JobStatus status,
        String detail,
        String output,
        Instant startedAt,
        Instant finishedAt
) {
    /**
     * 处理from相关逻辑。
     */
    public static JobSnapshot from(JobRecord record) {
        JobOutcome outcome = record.outcome();
        return new JobSnapshot(
                record.id(),
                record.kind().name().toLowerCase(),
                record.label(),
                record.ownerSessionId(),
                record.status(),
                record.detail(),
                outcome != null ? outcome.output() : null,
                record.startedAt(),
                record.finishedAt()
        );
    }
}
