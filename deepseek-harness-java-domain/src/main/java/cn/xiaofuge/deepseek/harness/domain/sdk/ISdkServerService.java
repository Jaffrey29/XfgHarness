package cn.xiaofuge.deepseek.harness.domain.sdk;

/**
 * Application-facing SDK server seam: handles incoming requests and
 * emits notifications to connected clients.
 *
 * <p>调用流程：应用领域规则 → 更新聚合或值对象 → 发布事件/返回结果。</p>
 * <p>示例：领域对象只暴露业务语言方法，不直接依赖基础设施。</p>
 * @author 小傅哥
 * @website bugstack.cn
 */
public interface ISdkServerService {

    void start();

    void stop();

    void emitNotification(String method, Object params);
}
