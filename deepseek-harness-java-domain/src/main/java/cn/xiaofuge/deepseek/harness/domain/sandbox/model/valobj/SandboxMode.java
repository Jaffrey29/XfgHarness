package cn.xiaofuge.deepseek.harness.domain.sandbox.model.valobj;

/**
 * File-sandbox modes ordered from most restrictive to least.
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public enum SandboxMode {
    READ_ONLY,
    WORKSPACE_WRITE,
    DANGER_FULL_ACCESS
}
