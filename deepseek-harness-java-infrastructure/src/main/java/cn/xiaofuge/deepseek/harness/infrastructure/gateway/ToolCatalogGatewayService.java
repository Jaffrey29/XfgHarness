package cn.xiaofuge.deepseek.harness.infrastructure.gateway;

import cn.xiaofuge.deepseek.harness.infrastructure.gateway.dto.ToolCatalogRequestDTO;
import cn.xiaofuge.deepseek.harness.infrastructure.gateway.dto.ToolCatalogResponseDTO;
import java.util.LinkedHashSet;
import org.springframework.stereotype.Service;

/**
 * Provides a placeholder gateway for tool-policy resolution.
 *
 * <p>调用流程：领域端口 → 基础设施适配器 → 外部系统。</p>
 * <p>示例：适配器只实现端口契约，业务规则保留在 domain。</p>
 * @author 小傅哥
 * @website bugstack.cn
 */
@Service
public class ToolCatalogGatewayService {

    /**
     * Resolves enabled tools from the requested list.
     *
     * @param request tool request
     * @return resolved tool list
     */
    public ToolCatalogResponseDTO resolve(ToolCatalogRequestDTO request) {
        return new ToolCatalogResponseDTO(new java.util.ArrayList<>(new LinkedHashSet<>(request.requestedTools())));
    }
}
