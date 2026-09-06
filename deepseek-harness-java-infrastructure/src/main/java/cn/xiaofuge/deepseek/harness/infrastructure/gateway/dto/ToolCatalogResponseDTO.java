package cn.xiaofuge.deepseek.harness.infrastructure.gateway.dto;

import java.util.List;

/**
 * Returns the resolved tool codes from the gateway layer.
 *
 * @param enabledTools enabled tool codes
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public record ToolCatalogResponseDTO(List<String> enabledTools) {

    public ToolCatalogResponseDTO {
        enabledTools = enabledTools == null ? List.of() : List.copyOf(enabledTools);
    }
}
