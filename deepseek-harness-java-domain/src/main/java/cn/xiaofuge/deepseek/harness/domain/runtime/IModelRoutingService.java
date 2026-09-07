package cn.xiaofuge.deepseek.harness.domain.runtime;

import cn.xiaofuge.deepseek.harness.domain.runtime.model.model.valobj.ModelProviderProfileVO;

/**
 * 模型路由服务接口。
 * <p>
 * 执行流程：
 * 1. 接收执行画像编码；
 * 2. 路由到对应模型提供方；
 * 3. 返回模型提供方画像。
 *
 * <p>调用流程：应用领域规则 → 更新聚合或值对象 → 发布事件/返回结果。</p>
 * <p>示例：领域对象只暴露业务语言方法，不直接依赖基础设施。</p>
 * @author 小傅哥
 * @website bugstack.cn
 */
public interface IModelRoutingService {

    /**
     * 解析执行画像对应的模型提供方。
     * <p>
     * 执行流程：
     * 1. 读取 profileCode；
     * 2. 调用模型提供方端口；
     * 3. 返回解析后的模型提供方画像。
     *
     * @param profileCode 执行画像编码
     * @return 模型提供方画像
     */
    ModelProviderProfileVO resolve(String profileCode);
}
