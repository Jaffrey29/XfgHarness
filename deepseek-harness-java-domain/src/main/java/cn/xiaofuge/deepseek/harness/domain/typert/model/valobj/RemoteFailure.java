package cn.xiaofuge.deepseek.harness.domain.typert.model.valobj;

/**
 * One Remote call's failure as the carrier reported it.
 *
 * @param code    machine-readable error code
 * @param message human-readable error message
 * @param details structured error details (JSON-serializable)
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public record RemoteFailure(String code, String message, Object details) {}
