package cn.walioffice.deepseek.harness.domain.sdk.adapter.port;

import java.util.function.Consumer;

/**
 * Port for newline-delimited JSON-RPC 2.0 transport over byte streams.
 *
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
