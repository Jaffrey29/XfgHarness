package cn.walioffice.deepseek.harness.domain.jobs.model.entity;
import cn.walioffice.deepseek.harness.domain.jobs.model.valobj.JobStatus;


import cn.walioffice.deepseek.harness.domain.jobs.model.valobj.JobKind;
import cn.walioffice.deepseek.harness.domain.jobs.model.valobj.JobOutcome;
import java.time.Instant;

/**
 * Mutable in-memory job record. Never handed out directly — callers get a snapshot.
 *
 * @param id             registry-issued job id
 * @param kind           producer kind (bash, subagent, ...)
 * @param label          one-line model-facing label
 * @param ownerSessionId optional owning session
 * @param status         current lifecycle status
 * @param detail         kind-specific detail
 * @param output         final output
 * @param startedAt      start instant
 * @param finishedAt     finish instant (null while running)
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public class JobRecord {
    private final String id;
    private final JobKind kind;
    private final String label;
    private final String ownerSessionId;
    private volatile JobStatus status;
    private volatile String detail;
    private volatile String output;
    private final Instant startedAt = Instant.now();
    private volatile Instant finishedAt;
    private volatile JobOutcome outcome;

    public JobRecord(String id, JobKind kind, String label, String ownerSessionId) {
        this.id = id;
        this.kind = kind;
        this.label = label;
        this.ownerSessionId = ownerSessionId;
        this.status = JobStatus.RUNNING;
    }

    public void complete(JobOutcome outcome) {
        this.outcome = outcome;
        this.status = outcome.status();
        this.detail = outcome.detail();
        this.output = outcome.output();
        this.finishedAt = Instant.now();
    }

    public JobOutcome outcome() { return outcome; }
    public JobStatus status() { return status; }
    public String id() { return id; }
    public JobKind kind() { return kind; }
    public String label() { return label; }
    public String ownerSessionId() { return ownerSessionId; }
    public String detail() { return detail; }
    public String output() { return output; }
    public Instant startedAt() { return startedAt; }
    public Instant finishedAt() { return finishedAt; }
}
