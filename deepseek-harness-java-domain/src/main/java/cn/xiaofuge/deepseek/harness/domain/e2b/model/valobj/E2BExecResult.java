package cn.xiaofuge.deepseek.harness.domain.e2b.model.valobj;

/**
 * Result of code execution inside an E2B sandbox.
 *
 * @param output       stdout output
 * @param error        stderr output
 * @param exitCode     process exit code
 * @param durationMs   execution duration
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public record E2BExecResult(String output, String error, int exitCode, long durationMs) {}
