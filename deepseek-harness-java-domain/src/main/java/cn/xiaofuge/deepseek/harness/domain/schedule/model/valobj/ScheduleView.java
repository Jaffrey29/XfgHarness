package cn.xiaofuge.deepseek.harness.domain.schedule.model.valobj;

import cn.xiaofuge.deepseek.harness.domain.schedule.model.entity.ScheduleRecord;
import java.time.Instant;

/**
 * Complete model-facing view of one active reminder.
 *
 * @param record       the underlying schedule record
 * @param state        whether the target remains in the future
 * @param deliveryMode fixed v1 delivery boundary: the original session must be live
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public record ScheduleView(ScheduleRecord record, ScheduleState state, String deliveryMode) {

    /**
     * 创建目标对象。
     * 流程：应用领域规则 → 更新领域对象 → 返回结果。
     * @param record record参数。
     * @param now now参数。
     * @return 创建的领域对象。
     */
    public static ScheduleView of(ScheduleRecord record, Instant now) {
        ScheduleState state = record.scheduledAt().isAfter(now)
                ? ScheduleState.SCHEDULED
                : ScheduleState.OVERDUE;
        return new ScheduleView(record, state, "session-local");
    }
}
