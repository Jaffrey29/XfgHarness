package cn.xiaofuge.deepseek.harness.domain.hooks.model.valobj;

/**
 * One configured command hook: a shell command line with an optional per-hook timeout.
 *
 * @param command    the shell command line to run
 * @param timeoutSec per-hook timeout in seconds (null = default)
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public record CommandHook(String command, Integer timeoutSec) {}
