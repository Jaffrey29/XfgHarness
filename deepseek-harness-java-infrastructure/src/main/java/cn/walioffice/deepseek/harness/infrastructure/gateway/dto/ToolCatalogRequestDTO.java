package cn.walioffice.deepseek.harness.infrastructure.gateway.dto;

import java.util.List;

/**
 * Carries requested tool codes to the gateway layer.
 *
 * @param requestedTools requested tool codes
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public record ToolCatalogRequestDTO(List<String> requestedTools) {

    public ToolCatalogRequestDTO {
        requestedTools = requestedTools == null ? List.of() : List.copyOf(requestedTools);
    }
}
