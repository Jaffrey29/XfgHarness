package cn.walioffice.deepseek.harness.domain.shared.model.valobj;

/**
 * Represents the shared lifecycle state used by task and session domains.
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public enum HarnessStatusEnumVO {
    CREATED,
    PENDING_APPROVAL,
    QUEUED,
    RUNNING,
    COMPLETED,
    FAILED
}
