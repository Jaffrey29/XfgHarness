package cn.xiaofuge.deepseek.harness.domain.jobs.model.entity;
import cn.xiaofuge.deepseek.harness.domain.jobs.model.valobj.JobStatus;


import cn.xiaofuge.deepseek.harness.domain.jobs.model.valobj.JobKind;
import cn.xiaofuge.deepseek.harness.domain.jobs.model.valobj.JobOutcome;
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

    /**
     * 处理Job Record相关逻辑。
     */
    public JobRecord(String id, JobKind kind, String label, String ownerSessionId) {
        this.id = id;
        this.kind = kind;
        this.label = label;
        this.ownerSessionId = ownerSessionId;
        this.status = JobStatus.RUNNING;
    }

    /**
     * 处理complete相关逻辑。
     */
    public void complete(JobOutcome outcome) {
        this.outcome = outcome;
        this.status = outcome.status();
        this.detail = outcome.detail();
        this.output = outcome.output();
        this.finishedAt = Instant.now();
    }

    /**
     * 处理outcome相关逻辑。
     */
    public JobOutcome outcome() { return outcome; }
    /**
     * 处理status相关逻辑。
     */
    public JobStatus status() { return status; }
    /**
     * 处理id相关逻辑。
     */
    public String id() { return id; }
    /**
     * 处理kind相关逻辑。
     */
    public JobKind kind() { return kind; }
    /**
     * 处理label相关逻辑。
     */
    public String label() { return label; }
    /**
     * 处理owner Session Id相关逻辑。
     */
    public String ownerSessionId() { return ownerSessionId; }
    /**
     * 处理detail相关逻辑。
     */
    public String detail() { return detail; }
    /**
     * 处理output相关逻辑。
     */
    public String output() { return output; }
    /**
     * 处理started At相关逻辑。
     */
    public Instant startedAt() { return startedAt; }
    /**
     * 处理finished At相关逻辑。
     */
    public Instant finishedAt() { return finishedAt; }
}
