package cn.xiaofuge.deepseek.harness.domain.sdk.adapter.port;

import java.util.function.Consumer;

/**
 * Port for newline-delimited JSON-RPC 2.0 transport over byte streams.
 *
 * <p>调用流程：应用领域规则 → 更新聚合或值对象 → 发布事件/返回结果。</p>
 * <p>示例：领域对象只暴露业务语言方法，不直接依赖基础设施。</p>
 * @author 小傅哥
 * @website bugstack.cn
 */
public interface ISdkTransportPort {

    /**
     * Send a JSON-RPC request and wait for the response.
     *
     * @param method  request method
     * @param params  request params (JSON-serializable)
     * @return result object (JSON-deserialized)
     */
    Object request(String method, Object params);

    /**
     * Send a JSON-RPC notification (no response expected).
     *
     * @param method  notification method
     * @param params  notification params
     */
    void notify(String method, Object params);

    /**
     * Register a handler for incoming notifications.
     *
     * @param handler  notification handler (method, params)
     */
    void onNotification(Consumer<NotificationFrame> handler);

    /**
     * Close the transport.
     */
    void close();

    /** One incoming notification frame. */
    record NotificationFrame(String method, Object params) {}
}
