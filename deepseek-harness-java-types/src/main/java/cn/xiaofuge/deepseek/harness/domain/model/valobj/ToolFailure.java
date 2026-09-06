package cn.xiaofuge.deepseek.harness.domain.model.valobj;

import java.util.Map;

/**
 * 工具执行失败的结构化信息。
 * 对应 TS 侧 {@code ToolErrorInfo} 和 {@code ToolFailure}。
 *
 * @param message 人类可读的错误消息
 * @param info    结构化错误信息，包含名称和错误码
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public record ToolFailure(String message, ToolErrorInfo info) {

    public ToolFailure {
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("message must be non-blank");
        }
    }

    public ToolFailure(String message, String code) {
        this(message, new ToolErrorInfo(code, code));
    }

/** 结构化错误标识，对应 TS 侧 {@code ToolErrorInfo}。 */
    public record ToolErrorInfo(String name, String code) {
        public ToolErrorInfo {
            if (name == null || name.isBlank()) {
                name = "TOOL_ERROR";
            }
            if (code == null || code.isBlank()) {
                code = "TOOL_ERROR";
            }
        }
    }
}
