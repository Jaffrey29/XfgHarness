package cn.walioffice.deepseek.harness.domain.session.event.model.valobj;
public record AssistantChunkPayload(long turn, long step, String chunk, String kind) {}
