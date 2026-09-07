package cn.xiaofuge.deepseek.harness.domain.session.event.model.valobj;
/**
 * AssistantChunkPayload 定义当前模块中的record契约与职责。
 */
public record AssistantChunkPayload(long turn, long step, String chunk, String kind) {}
