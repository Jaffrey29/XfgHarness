package cn.walioffice.deepseek.harness.domain.runtime.profile.service.router;

/**
 * Resolves the execution mode for one profile code.
 *
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
