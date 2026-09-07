package cn.xiaofuge.deepseek.harness.domain.hooks.model.valobj;

/**
 * Hook interception points shared by both dialects.
 *
 * <p>调用流程：应用领域规则 → 更新聚合或值对象 → 发布事件/返回结果。</p>
 * <p>示例：领域对象只暴露业务语言方法，不直接依赖基础设施。</p>
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
