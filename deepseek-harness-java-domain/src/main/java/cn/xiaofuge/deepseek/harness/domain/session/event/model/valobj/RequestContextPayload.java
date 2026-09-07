package cn.xiaofuge.deepseek.harness.domain.session.event.model.valobj;
/**
 * RequestContextPayload 定义当前模块中的record契约与职责。
 */
public record RequestContextPayload(String provider, String model, Long contextWindow) {}
