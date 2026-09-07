package cn.xiaofuge.deepseek.harness.domain.sdk;

import cn.xiaofuge.deepseek.harness.domain.sdk.model.valobj.*;

import java.util.function.Consumer;

/**
 * Application-facing SDK client seam: connects to a harness server,
 * sends requests, and receives notifications.
 *
 * <p>调用流程：应用领域规则 → 更新聚合或值对象 → 发布事件/返回结果。</p>
 * <p>示例：领域对象只暴露业务语言方法，不直接依赖基础设施。</p>
 * @author 小傅哥
 * @website bugstack.cn
 */
public interface ISdkClientService {

    InitializeResult initialize(InitializeParams params);

    SessionPromptResult sessionPrompt(SessionPromptParams params);

    void shutdown();

    void onSessionStatus(Consumer<SessionStatus> handler);

    void onSessionEvent(Consumer<Object> handler);
}
