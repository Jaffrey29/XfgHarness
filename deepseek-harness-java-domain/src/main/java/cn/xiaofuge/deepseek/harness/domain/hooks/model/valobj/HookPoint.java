package cn.xiaofuge.deepseek.harness.domain.hooks.model.valobj;

/**
 * Hook interception points shared by both dialects.
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public enum HookPoint {
    PRE_TOOL_USE,
    POST_TOOL_USE,
    USER_PROMPT_SUBMIT,
    SESSION_START,
    STOP,
    NOTIFICATION
}
