package cn.walioffice.deepseek.harness.domain.llm.model.valobj;

/** 推理或思考内容，与可见文本相互区分。 */
public record ReasoningBlock(String text) implements ContentBlock {}
