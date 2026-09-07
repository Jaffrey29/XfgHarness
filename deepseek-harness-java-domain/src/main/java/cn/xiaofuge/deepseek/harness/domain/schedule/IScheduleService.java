package cn.xiaofuge.deepseek.harness.domain.schedule;

import cn.xiaofuge.deepseek.harness.domain.schedule.model.entity.ScheduleRecord;
import cn.xiaofuge.deepseek.harness.domain.schedule.model.valobj.ScheduleView;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Agent-scoped durable one-shot and fixed-rate reminders over the session event log.
 *
 * <p>调用流程：创建计划 → 到期扫描 → 执行任务 → 更新状态。</p>
 * <p>示例：设置固定间隔后，调度器到期触发任务并记录结果。</p>
 * @author 小傅哥
 * @website bugstack.cn
 */
public interface IScheduleService {

    ScheduleRecord createAfter(String sessionId, String prompt, Duration delay);

    ScheduleRecord createAt(String sessionId, String prompt, Instant target);

    ScheduleRecord createEvery(String sessionId, String prompt, Duration interval);

    List<ScheduleView> list(String sessionId);

    boolean delete(String sessionId, String id);

    /**
     * Drive due reminders: returns records whose target has passed and marks
     * one-shots as dispatched (recurring records advance to next occurrence).
     *
     * @param sessionId owning session
     * @param now       current instant
     * @return due reminders to deliver
     */
    List<ScheduleRecord> drive(String sessionId, Instant now);
}
