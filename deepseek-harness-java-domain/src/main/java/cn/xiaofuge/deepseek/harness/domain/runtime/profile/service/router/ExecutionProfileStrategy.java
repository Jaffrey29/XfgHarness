package cn.xiaofuge.deepseek.harness.domain.runtime.profile.service.router;

/**
 * Resolves the execution mode for one profile code.
 *
 * <p>调用流程：应用领域规则 → 更新聚合或值对象 → 发布事件/返回结果。</p>
 * <p>示例：领域对象只暴露业务语言方法，不直接依赖基础设施。</p>
 * @author 小傅哥
 * @website bugstack.cn
 */
public interface ExecutionProfileStrategy {

    /**
     * Returns the profile code handled by this strategy.
     *
     * @return profile code
     */
    String profileCode();

    /**
     * Resolves the execution mode for the current profile.
     *
     * @return execution mode string
     */
    String executionMode();
}
