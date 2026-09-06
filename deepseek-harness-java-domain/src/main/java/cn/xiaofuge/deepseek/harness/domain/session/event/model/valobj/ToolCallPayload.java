package cn.xiaofuge.deepseek.harness.domain.session.event.model.valobj;
public record ToolCallPayload(long turn, long step, String callId, String name, String arguments) {}
