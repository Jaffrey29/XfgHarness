package cn.walioffice.deepseek.harness.domain.runtime.profile.service.router;

/**
 * Resolves the execution mode for browser-hosted runs.
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public class WebExecutionProfileStrategy implements ExecutionProfileStrategy {

    @Override
    public String profileCode() {
        return "web";
    }

    @Override
    public String executionMode() {
        return "WEB";
    }
}
