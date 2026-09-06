package cn.walioffice.deepseek.harness.domain.schedule;

import cn.walioffice.deepseek.harness.domain.schedule.model.entity.ScheduleRecord;
import cn.walioffice.deepseek.harness.domain.schedule.model.valobj.ScheduleView;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Agent-scoped durable one-shot and fixed-rate reminders over the session event log.
 *
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
