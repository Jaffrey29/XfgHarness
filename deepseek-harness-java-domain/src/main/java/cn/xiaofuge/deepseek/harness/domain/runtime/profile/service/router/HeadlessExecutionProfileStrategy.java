package cn.xiaofuge.deepseek.harness.domain.runtime.profile.service.router;

import org.springframework.stereotype.Service;

/**
 * Resolves the execution mode for headless runs.
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
@Service
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
