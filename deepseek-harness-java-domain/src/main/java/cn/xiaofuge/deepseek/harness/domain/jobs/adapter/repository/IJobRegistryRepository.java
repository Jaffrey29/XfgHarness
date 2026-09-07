package cn.xiaofuge.deepseek.harness.domain.jobs.adapter.repository;

import cn.xiaofuge.deepseek.harness.domain.jobs.model.entity.JobRecord;
import java.util.List;
import java.util.Optional;

/**
 * IJobRegistryRepository 定义当前模块中的interface契约与职责。
 */
public interface IJobRegistryRepository {

    void save(JobRecord record);

    Optional<JobRecord> findById(String id);

    List<JobRecord> findByOwnerSessionId(String sessionId);

    List<JobRecord> findAll();

    void deleteById(String id);
}
