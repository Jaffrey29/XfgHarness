package cn.xiaofuge.deepseek.harness.domain.session.event.model.valobj;
public record RequestContextPayload(String provider, String model, Long contextWindow) {}
