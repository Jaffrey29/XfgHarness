package cn.xiaofuge.deepseek.harness.domain.schedule.adapter.repository;

import cn.xiaofuge.deepseek.harness.domain.schedule.model.entity.ScheduleRecord;
import java.util.List;
import java.util.Optional;

/**
 * IScheduleRepository 领域仓储契约。
 * <p>职责：承载领域规则、状态或边界，供上层编排与基础设施适配。</p>
 */
public interface IScheduleRepository {

    void save(ScheduleRecord record);

    void deleteById(String id);

    Optional<ScheduleRecord> findById(String id);

    List<ScheduleRecord> findBySessionId(String sessionId);

    List<ScheduleRecord> findDue(String sessionId, java.time.Instant now);
}
