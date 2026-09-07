package cn.xiaofuge.deepseek.harness.domain.session.event.model.valobj;
/**
 * TurnEndPayload 定义当前模块中的record契约与职责。
 */
public record TurnEndPayload(long turn, TurnEndReason reason) {}
