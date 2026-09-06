package cn.xiaofuge.deepseek.harness.domain.jobs.model.valobj;

public record JobOutcome(JobStatus status, String detail, String output) {}
