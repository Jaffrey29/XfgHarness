package cn.walioffice.deepseek.harness.domain.jobs.adapter.repository;

import cn.walioffice.deepseek.harness.domain.jobs.model.entity.JobRecord;
import java.util.List;
import java.util.Optional;

public interface IJobRegistryRepository {

    void save(JobRecord record);

    Optional<JobRecord> findById(String id);

    List<JobRecord> findByOwnerSessionId(String sessionId);

    List<JobRecord> findAll();

    void deleteById(String id);
}
