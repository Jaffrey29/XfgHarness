package cn.walioffice.deepseek.harness.domain.tool.model.entity;

import cn.walioffice.deepseek.harness.domain.llm.model.valobj.ContentBlock;
import cn.walioffice.deepseek.harness.domain.llm.model.valobj.TextBlock;
import cn.walioffice.deepseek.harness.domain.llm.model.entity.Message;
import cn.walioffice.deepseek.harness.domain.tool.model.valobj.ToolFailure;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 单次工具调用在本机执行后的判别结果。
 * 对应 TS 侧 {@code ToolExecutionResult}。
 *
 * <p>使用 {@link #isError()} 区分成功与失败。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public sealed interface ToolExecutionResult
        permits ToolExecutionResult.Success, ToolExecutionResult.Failure {

/** 是否执行失败。 */
    boolean isError();

/** 发送给模型的内容块。 */
    List<ContentBlock> content();

/** 供下一次请求使用的额外上下文消息。 */
    List<Message> additionalContexts();

/** 当前结果是否结束当前 Agent 回合。 */
    boolean concludesTurn();

/** 标准化的工具执行成功结果。 */
    record Success(
            Object value,
            List<ContentBlock> content,
            Map<String, Object> meta,
            List<Message> additionalContexts,
            boolean concludesTurn
    ) implements ToolExecutionResult {
        public Success {
            content = content == null ? List.of() : List.copyOf(content);
            additionalContexts = additionalContexts == null ? List.of() : List.copyOf(additionalContexts);
            meta = meta == null ? null : Map.copyOf(meta);
        }
        @Override public boolean isError() { return false; }
        @Override public boolean concludesTurn() { return concludesTurn; }
    }

/** 标准化的工具执行失败结果。 */
    record Failure(
            ToolFailure error,
            List<ContentBlock> content,
            Map<String, Object> meta,
            List<Message> additionalContexts
    ) implements ToolExecutionResult {
        public Failure {
            content = content == null ? List.of() : List.copyOf(content);
            additionalContexts = additionalContexts == null ? List.of() : List.copyOf(additionalContexts);
            meta = meta == null ? null : Map.copyOf(meta);
        }
        @Override public boolean isError() { return true; }
        @Override public boolean concludesTurn() { return false; }
    }

/** 快速创建简单文本成功结果。 */
    static Success ok(List<ContentBlock> content) {
        return new Success(null, content, null, List.of(), false);
    }

/** 快速创建简单文本失败结果。 */
    static Failure fail(String message, String code) {
        var error = new ToolFailure(message,
                new ToolFailure.ToolErrorInfo(code, code));
        var content = List.of((ContentBlock) new TextBlock(message));
        return new Failure(error, content, null, List.of());
    }
}
