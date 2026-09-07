package cn.xiaofuge.deepseek.harness.domain.schedule.service;

import cn.xiaofuge.deepseek.harness.domain.schedule.IScheduleService;
import cn.xiaofuge.deepseek.harness.domain.schedule.adapter.repository.IScheduleRepository;
import cn.xiaofuge.deepseek.harness.domain.schedule.model.entity.ScheduleRecord;
import cn.xiaofuge.deepseek.harness.domain.schedule.model.valobj.ScheduleKind;
import cn.xiaofuge.deepseek.harness.domain.schedule.model.valobj.ScheduleView;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Durable reminder service. Creates after/at/every records, lists active views,
 * deletes by id, and drives due reminders.
 *
 * <p>调用流程：创建计划 → 到期扫描 → 执行任务 → 更新状态。</p>
 * <p>示例：设置固定间隔后，调度器到期触发任务并记录结果。</p>
 * @author 小傅哥
 * @website bugstack.cn
 */
@Service
public class ScheduleService implements IScheduleService {

    private static final long MIN_EVERY_INTERVAL_SECONDS = 300;

    private final IScheduleRepository repository;

    /**
     * 处理调度 Service。
     * 流程：应用领域规则 → 更新领域对象 → 返回结果。
     * @param repository repository参数。
     * @return 方法执行结果。
     */
    public ScheduleService(IScheduleRepository repository) {
        this.repository = repository;
    }

    @Override
    public ScheduleRecord createAfter(String sessionId, String prompt, Duration delay) {
        if (delay.isNegative() || delay.isZero()) {
            throw new IllegalArgumentException("after delay must be positive");
        }
        Instant target = Instant.now().plus(delay);
        ScheduleRecord record = new ScheduleRecord(
                allocateId(), ScheduleKind.AFTER, prompt.trim(), delay.getSeconds(), null, target, sessionId);
        repository.save(record);
        return record;
    }
    /**
     * 创建At。
     * 流程：应用领域规则 → 更新领域对象 → 返回结果。
     * @param sessionId 会话 ID。
     * @param prompt prompt参数。
     * @param target 目标对象。
     * @return 方法执行结果。
     */


    @Override
    public ScheduleRecord createAt(String sessionId, String prompt, Instant target) {
        if (target.isBefore(Instant.now())) {
            throw new IllegalArgumentException("at target must be in the future");
        }
        ScheduleRecord record = new ScheduleRecord(
                allocateId(), ScheduleKind.AT, prompt.trim(), null, null, target, sessionId);
        repository.save(record);
        return record;
    }
    /**
     * 创建Every。
     * 流程：应用领域规则 → 更新领域对象 → 返回结果。
     * @param sessionId 会话 ID。
     * @param prompt prompt参数。
     * @param interval interval参数。
     * @return 方法执行结果。
     */


    @Override
    public ScheduleRecord createEvery(String sessionId, String prompt, Duration interval) {
        if (interval.getSeconds() < MIN_EVERY_INTERVAL_SECONDS) {
            throw new IllegalArgumentException(
                    "every interval must be >= " + MIN_EVERY_INTERVAL_SECONDS + " seconds");
        }
        Instant target = Instant.now().plus(interval);
        ScheduleRecord record = new ScheduleRecord(
                allocateId(), ScheduleKind.EVERY, prompt.trim(), null, interval.getSeconds(), target, sessionId);
    /**
     * 列出。
     */
        repository.save(record);
        return record;
    }

    @Override
    public List<ScheduleView> list(String sessionId) {
        Instant now = Instant.now();
        return repository.findBySessionId(sessionId).stream()
                .map(r -> ScheduleView.of(r, now))
                .toList();
    }
    /**
     * 删除目标对象。
     * 流程：应用领域规则 → 更新领域对象 → 返回结果。
     * @param sessionId 会话 ID。
     * @param id id参数。
     * @return 方法执行结果。
     */


    @Override
    public boolean delete(String sessionId, String id) {
        return repository.findById(id)
                .filter(r -> r.sessionId().equals(sessionId))
                .map(r -> { repository.deleteById(id); return true; })
                .orElse(false);
    }
    /**
     * drive。
     * 流程：应用领域规则 → 更新领域对象 → 返回结果。
     * @param sessionId 会话 ID。
     * @param now now参数。
     * @return 方法执行结果。
     */


    @Override
    public List<ScheduleRecord> drive(String sessionId, Instant now) {
        List<ScheduleRecord> due = repository.findDue(sessionId, now);
        for (ScheduleRecord record : due) {
            if (record.kind() == ScheduleKind.EVERY) {
                // Advance to next anchor-aligned occurrence past now
                long interval = record.everySeconds();
                Instant next = record.scheduledAt();
                while (!next.isAfter(now)) {
                    next = next.plusSeconds(interval);
                }
                ScheduleRecord advanced = new ScheduleRecord(
                        record.id(), record.kind(), record.prompt(),
                        record.afterSeconds(), record.everySeconds(), next, record.sessionId());
                repository.save(advanced);
            } else {
                // One-shot: mark as dispatched by deleting
                repository.deleteById(record.id());
            }
        }
        return due;
    }

    /**
     * 列出全部 ocate ID。
     * 流程：应用领域规则 → 更新领域对象 → 返回结果。
     * @return 查询到的数据。
     */
    private static String allocateId() {
        return "sch_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
}
