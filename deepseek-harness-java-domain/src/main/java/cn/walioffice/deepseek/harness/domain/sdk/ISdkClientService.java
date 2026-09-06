package cn.walioffice.deepseek.harness.domain.sdk;

import cn.walioffice.deepseek.harness.domain.sdk.model.valobj.*;
import java.util.function.Consumer;

/**
 * Application-facing SDK client seam: connects to a harness server,
 * sends requests, and receives notifications.
 *
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
