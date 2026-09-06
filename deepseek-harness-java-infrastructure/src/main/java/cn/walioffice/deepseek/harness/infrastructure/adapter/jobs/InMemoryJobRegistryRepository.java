package cn.walioffice.deepseek.harness.infrastructure.adapter.jobs;

import cn.walioffice.deepseek.harness.domain.jobs.adapter.repository.IJobRegistryRepository;
import cn.walioffice.deepseek.harness.domain.jobs.model.entity.JobRecord;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory job registry repository for standalone mode.
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public class InMemoryJobRegistryRepository implements IJobRegistryRepository {

    private final ConcurrentHashMap<String, JobRecord> store = new ConcurrentHashMap<>();

    @Override
    public void save(JobRecord record) {
        store.put(record.id(), record);
    }

    @Override
    public Optional<JobRecord> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<JobRecord> findByOwnerSessionId(String sessionId) {
        return store.values().stream()
                .filter(r -> sessionId.equals(r.ownerSessionId()))
                .toList();
    }

    @Override
    public List<JobRecord> findAll() {
        return List.copyOf(store.values());
    }

    @Override
    public void deleteById(String id) {
        store.remove(id);
    }
}
