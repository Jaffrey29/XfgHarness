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
 * @author 小傅哥
 * @website bugstack.cn
 */
@Service
public class ScheduleService implements IScheduleService {

    private static final long MIN_EVERY_INTERVAL_SECONDS = 300;

    private final IScheduleRepository repository;

    /**
     * 处理Schedule Service相关逻辑。
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

    @Override
    public boolean delete(String sessionId, String id) {
        return repository.findById(id)
                .filter(r -> r.sessionId().equals(sessionId))
                .map(r -> { repository.deleteById(id); return true; })
                .orElse(false);
    }

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
     * 处理allocate Id相关逻辑。
     */
    private static String allocateId() {
        return "sch_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
}
