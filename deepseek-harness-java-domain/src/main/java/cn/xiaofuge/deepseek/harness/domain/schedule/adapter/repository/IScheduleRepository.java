package cn.xiaofuge.deepseek.harness.domain.schedule.adapter.repository;

import cn.xiaofuge.deepseek.harness.domain.schedule.model.entity.ScheduleRecord;
import java.util.List;
import java.util.Optional;

/**
 * Persistence boundary for schedule records. The runtime appends create/delete/dispatch
 * events and replays them into the active set.
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public interface IScheduleRepository {

    void save(ScheduleRecord record);

    void deleteById(String id);

    Optional<ScheduleRecord> findById(String id);

    List<ScheduleRecord> findBySessionId(String sessionId);

    List<ScheduleRecord> findDue(String sessionId, java.time.Instant now);
}
