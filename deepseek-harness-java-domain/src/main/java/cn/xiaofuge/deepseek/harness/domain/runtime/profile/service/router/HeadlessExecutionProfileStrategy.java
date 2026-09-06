package cn.xiaofuge.deepseek.harness.domain.runtime.profile.service.router;

/**
 * Resolves the execution mode for headless runs.
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public class HeadlessExecutionProfileStrategy implements ExecutionProfileStrategy {

    @Override
    public String profileCode() {
        return "headless";
    }

    @Override
    public String executionMode() {
        return "HEADLESS";
    }
}
