package cn.xiaofuge.deepseek.harness.domain.sdk.service;

import cn.xiaofuge.deepseek.harness.domain.sdk.ISdkServerService;
import cn.xiaofuge.deepseek.harness.domain.sdk.adapter.port.ISdkTransportPort;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;

/**
 * SDK server service: handles incoming requests via the handler port and
 * emits notifications to connected clients.
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
@Service
@Conditional(SdkStandaloneCondition.class)
public class SdkServerService implements ISdkServerService {

    private final ISdkTransportPort transport;

    /**
     * 处理Sdk Server Service相关逻辑。
     */
    public SdkServerService(ISdkTransportPort transport) {
        this.transport = transport;
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
