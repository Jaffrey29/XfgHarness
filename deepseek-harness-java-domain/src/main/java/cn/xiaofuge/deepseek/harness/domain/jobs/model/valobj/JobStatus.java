package cn.xiaofuge.deepseek.harness.domain.jobs.model.valobj;

/**
 * JobStatus 定义当前模块中的enum契约与职责。
 */
public enum JobStatus {
    RUNNING,
    STOPPING,
    COMPLETED,
    KILLED,
    FAILED
}
