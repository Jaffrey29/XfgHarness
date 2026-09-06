package cn.xiaofuge.deepseek.harness.infrastructure.adapter.schedule;

import cn.xiaofuge.deepseek.harness.domain.schedule.adapter.repository.IScheduleRepository;
import cn.xiaofuge.deepseek.harness.domain.schedule.model.entity.ScheduleRecord;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Standalone 模式的内存调度仓库。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public class InMemoryScheduleRepository implements IScheduleRepository {

    private final ConcurrentHashMap<String, ScheduleRecord> store = new ConcurrentHashMap<>();

    @Override
    public void save(ScheduleRecord record) {
        store.put(record.id(), record);
    }

    @Override
    public void deleteById(String id) {
        store.remove(id);
    }

    @Override
    public Optional<ScheduleRecord> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<ScheduleRecord> findBySessionId(String sessionId) {
        return store.values().stream()
                .filter(r -> r.sessionId().equals(sessionId))
                .toList();
    }

    @Override
    public List<ScheduleRecord> findDue(String sessionId, Instant now) {
        return store.values().stream()
                .filter(r -> r.sessionId().equals(sessionId))
                .filter(r -> !r.scheduledAt().isAfter(now))
                .toList();
    }
}
