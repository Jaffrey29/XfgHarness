package cn.xiaofuge.deepseek.harness.infrastructure.gateway;

import cn.xiaofuge.deepseek.harness.infrastructure.gateway.dto.ToolCatalogRequestDTO;
import cn.xiaofuge.deepseek.harness.infrastructure.gateway.dto.ToolCatalogResponseDTO;
import java.util.LinkedHashSet;
import org.springframework.stereotype.Service;

/**
 * Provides a placeholder gateway for tool-policy resolution.
 *
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
