package cn.xiaofuge.deepseek.harness.domain.sdk;

/**
 * Application-facing SDK server seam: handles incoming requests and
 * emits notifications to connected clients.
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public interface ISdkServerService {

    void start();

    void stop();

    void emitNotification(String method, Object params);
}
