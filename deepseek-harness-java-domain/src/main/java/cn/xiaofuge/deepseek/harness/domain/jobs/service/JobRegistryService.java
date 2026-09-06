package cn.xiaofuge.deepseek.harness.domain.jobs.service;

import cn.xiaofuge.deepseek.harness.domain.jobs.IJobRegistryService;
import cn.xiaofuge.deepseek.harness.domain.jobs.adapter.repository.IJobRegistryRepository;
import cn.xiaofuge.deepseek.harness.domain.jobs.model.entity.JobRecord;
import cn.xiaofuge.deepseek.harness.domain.jobs.model.entity.JobStart;
import cn.xiaofuge.deepseek.harness.domain.jobs.model.valobj.JobOutcome;
import cn.xiaofuge.deepseek.harness.domain.jobs.model.valobj.JobSnapshot;
import cn.xiaofuge.deepseek.harness.domain.jobs.model.valobj.JobStatus;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * In-process job registry. Tracks running jobs, cancels on disposal, and
 * delegates execution to a virtual-thread pool.
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public class JobRegistryService implements IJobRegistryService {

    private final IJobRegistryRepository repository;
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final ConcurrentHashMap<String, JobRecord> liveJobs = new ConcurrentHashMap<>();
    private final AtomicInteger idCounter = new AtomicInteger(0);
    private volatile boolean closed = false;

    public JobRegistryService(IJobRegistryRepository repository) {
        this.repository = repository;
    }

    @Override
    public JobSnapshot start(JobStart start) {
        if (closed) throw new IllegalStateException("job registry is closed");
        String id = start.kind().name().toLowerCase() + "_" + idCounter.incrementAndGet();
        JobRecord record = new JobRecord(id, start.kind(), start.label(), start.ownerSessionId());
        repository.save(record);
        liveJobs.put(id, record);

        CompletableFuture.runAsync(() -> {
            if (closed) return;
            try {
                JobOutcome outcome = start.work().call();
                record.complete(outcome);
            } catch (Exception e) {
                record.complete(new JobOutcome(JobStatus.FAILED, e.getClass().getSimpleName() + ": " + e.getMessage(), null));
            } finally {
                repository.save(record);
                liveJobs.remove(id);
            }
        }, executor);

        return JobSnapshot.from(record);
    }

    @Override
    public Optional<JobSnapshot> read(String id, String requestingSessionId) {
        return repository.findById(id)
                .filter(r -> r.ownerSessionId() == null || r.ownerSessionId().equals(requestingSessionId))
                .map(JobSnapshot::from);
    }

    @Override
    public List<JobSnapshot> list(String requestingSessionId) {
        return repository.findByOwnerSessionId(requestingSessionId).stream()
                .map(JobSnapshot::from)
                .toList();
    }

    @Override
    public boolean cancel(String id, String requestingSessionId, String reason) {
        JobRecord record = liveJobs.get(id);
        if (record == null) return false;
        if (record.ownerSessionId() != null && !requestingSessionId.equals(record.ownerSessionId())) return false;
        record.complete(new JobOutcome(JobStatus.KILLED, reason, null));
        liveJobs.remove(id);
        repository.save(record);
        return true;
    }

    public void close() {
        closed = true;
        executor.shutdownNow();
    }
}
