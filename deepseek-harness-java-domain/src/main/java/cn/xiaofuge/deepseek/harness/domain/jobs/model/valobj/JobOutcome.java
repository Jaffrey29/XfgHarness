package cn.xiaofuge.deepseek.harness.domain.jobs.model.valobj;

/**
 * JobOutcome 定义当前模块中的record契约与职责。
 */
public record JobOutcome(JobStatus status, String detail, String output) {}
