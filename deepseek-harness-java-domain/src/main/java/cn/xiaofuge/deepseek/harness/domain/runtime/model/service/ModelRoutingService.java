package cn.xiaofuge.deepseek.harness.domain.runtime.model.service;

import cn.xiaofuge.deepseek.harness.domain.runtime.IModelRoutingService;
import cn.xiaofuge.deepseek.harness.domain.runtime.model.adapter.port.IModelProviderPort;
import cn.xiaofuge.deepseek.harness.domain.runtime.model.model.valobj.ModelProviderProfileVO;

/**
 * 模型路由服务。
 * <p>
 * 执行流程：
 * 1. 接收执行画像编码；
 * 2. 转交模型提供方端口解析具体模型；
 * 3. 返回模型提供方画像。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public class ModelRoutingService implements IModelRoutingService {

    private final IModelProviderPort modelProviderPort;

    public ModelRoutingService(IModelProviderPort modelProviderPort) {
        this.modelProviderPort = modelProviderPort;
    }

    /**
     * 解析对应模型提供方。
     * <p>
     * 执行流程：
     * 1. 读取 profileCode；
     * 2. 调用模型提供方端口；
     * 3. 返回解析结果。
     */
    @Override
    public ModelProviderProfileVO resolve(String profileCode) {
        return modelProviderPort.resolve(profileCode);
    }

}
