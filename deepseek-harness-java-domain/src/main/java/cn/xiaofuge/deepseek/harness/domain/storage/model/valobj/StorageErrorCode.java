package cn.xiaofuge.deepseek.harness.domain.storage.model.valobj;

/**
 * StorageErrorCode 定义当前模块中的enum契约与职责。
 */
public enum StorageErrorCode {
    MALFORMED_MEDIUM,
    UNIT_NOT_FOUND,
    UNIT_ALREADY_OPEN,
    BACKEND_NOT_FOUND,
    CLOSED
}
