package cn.walioffice.deepseek.harness.infrastructure.adapter.port;

import cn.walioffice.deepseek.harness.domain.runtime.model.adapter.port.IModelProviderPort;
import cn.walioffice.deepseek.harness.domain.runtime.model.model.valobj.ModelProviderProfileVO;
import cn.walioffice.deepseek.harness.infrastructure.gateway.DeepSeekGatewayService;
import cn.walioffice.deepseek.harness.infrastructure.gateway.dto.ModelRequestDTO;
import cn.walioffice.deepseek.harness.infrastructure.gateway.dto.ModelResponseDTO;
import org.springframework.stereotype.Component;

/**
 * 模型提供方端口适配器。
 * <p>
 * 执行流程：
 * 1. 接收领域层的画像查询；
 * 2. 转交网关层解析具体提供方；
 * 3. 返回模型提供方画像。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
@Component
public class ModelProviderPort implements IModelProviderPort {

    private final DeepSeekGatewayService deepSeekGatewayService;

    public ModelProviderPort(DeepSeekGatewayService deepSeekGatewayService) {
        this.deepSeekGatewayService = deepSeekGatewayService;
    }

    @Override
    public ModelProviderProfileVO resolve(String profileCode) {
        ModelResponseDTO response = deepSeekGatewayService.resolveModel(new ModelRequestDTO(profileCode));
        return new ModelProviderProfileVO(response.providerCode(), response.modelCode());
    }

}
