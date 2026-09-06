package cn.xiaofuge.deepseek.harness.domain.sdk.service;

import cn.xiaofuge.deepseek.harness.domain.sdk.ISdkServerService;
import cn.xiaofuge.deepseek.harness.domain.sdk.adapter.port.ISdkServerHandler;
import cn.xiaofuge.deepseek.harness.domain.sdk.adapter.port.ISdkTransportPort;

/**
 * SDK server service: handles incoming requests via the handler port and
 * emits notifications to connected clients.
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public class SdkServerService implements ISdkServerService {

    private final ISdkTransportPort transport;
    private final ISdkServerHandler handler;

    public SdkServerService(ISdkTransportPort transport, ISdkServerHandler handler) {
        this.transport = transport;
        this.handler = handler;
    }

    @Override
    public void start() {
        // Transport handles request routing; server just needs to be ready
    }

    @Override
    public void stop() {
        transport.close();
    }

    @Override
    public void emitNotification(String method, Object params) {
        transport.notify(method, params);
    }
}
