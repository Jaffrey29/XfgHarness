package cn.xiaofuge.deepseek.harness.domain.schedule.adapter.repository;

import cn.xiaofuge.deepseek.harness.domain.schedule.model.entity.ScheduleRecord;
import java.util.List;
import java.util.Optional;

/**
 * Persistence boundary for schedule records. The runtime appends create/delete/dispatch
 * events and replays them into the active set.
 *
 * <p>调用流程：创建计划 → 到期扫描 → 执行任务 → 更新状态。</p>
 * <p>示例：设置固定间隔后，调度器到期触发任务并记录结果。</p>
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
