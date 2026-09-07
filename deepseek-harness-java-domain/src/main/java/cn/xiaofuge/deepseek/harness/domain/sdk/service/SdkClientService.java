package cn.xiaofuge.deepseek.harness.domain.sdk.service;

import cn.xiaofuge.deepseek.harness.domain.sdk.ISdkClientService;
import cn.xiaofuge.deepseek.harness.domain.sdk.adapter.port.ISdkTransportPort;
import cn.xiaofuge.deepseek.harness.domain.sdk.model.valobj.*;

import java.util.Map;
import java.util.function.Consumer;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;

/**
 * SDK client service: sends requests over the transport port and routes
 * notifications to registered handlers.
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
@Service
@Conditional(SdkStandaloneCondition.class)
public class SdkClientService implements ISdkClientService {

    private final ISdkTransportPort transport;
    private volatile Consumer<SessionStatus> statusHandler;
    private volatile Consumer<Object> eventHandler;

    /**
     * 处理Sdk Client Service相关逻辑。
     */
    public SdkClientService(ISdkTransportPort transport) {
        this.transport = transport;
        this.transport.onNotification(frame -> {
            switch (frame.method()) {
                case "session.status" -> {
                    if (statusHandler != null) statusHandler.accept(SessionStatus.IDLE);
                }
                case "session.event" -> {
                    if (eventHandler != null) eventHandler.accept(frame.params());
                }
            }
        });
    }

    @Override
    @SuppressWarnings("unchecked")
    public InitializeResult initialize(InitializeParams params) {
        Map<String, Object> result = (Map<String, Object>) transport.request("initialize", params);
        return new InitializeResult(
                (String) result.get("serverName"),
                (String) result.get("serverVersion")
        );
    }

    @Override
    @SuppressWarnings("unchecked")
    public SessionPromptResult sessionPrompt(SessionPromptParams params) {
        Map<String, Object> result = (Map<String, Object>) transport.request("session/prompt", params);
        return new SessionPromptResult((String) result.get("messageId"));
    }

    @Override
    public void shutdown() {
        transport.request("shutdown", null);
        transport.close();
    }

    @Override
    public void onSessionStatus(Consumer<SessionStatus> handler) {
        this.statusHandler = handler;
    }

    @Override
    public void onSessionEvent(Consumer<Object> handler) {
        this.eventHandler = handler;
    }
}
