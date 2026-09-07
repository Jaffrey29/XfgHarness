package cn.xiaofuge.deepseek.harness.domain.sdk.service;

import cn.xiaofuge.deepseek.harness.domain.sdk.ISdkServerService;
import cn.xiaofuge.deepseek.harness.domain.sdk.adapter.port.ISdkTransportPort;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;

/**
 * SDK server service: handles incoming requests via the handler port and
 * emits notifications to connected clients.
 *
 * <p>调用流程：应用领域规则 → 更新聚合或值对象 → 发布事件/返回结果。</p>
 * <p>示例：领域对象只暴露业务语言方法，不直接依赖基础设施。</p>
 * @author 小傅哥
 * @website bugstack.cn
 */
@Service
@Conditional(SdkStandaloneCondition.class)
public class SdkServerService implements ISdkServerService {

    private final ISdkTransportPort transport;

    /**
     * 处理Sdk Server Service。
     * 流程：应用领域规则 → 更新领域对象 → 返回结果。
     * @param transport transport参数。
     * @return 方法执行结果。
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
    /**
     * emit Notification。
     * 流程：应用领域规则 → 更新领域对象 → 返回结果。
     * @param method method参数。
     * @param params params参数。
     */


    @Override
    public void emitNotification(String method, Object params) {
        transport.notify(method, params);
    }
}
