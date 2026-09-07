package cn.xiaofuge.deepseek.harness.infrastructure.adapter.port;

import cn.xiaofuge.deepseek.harness.domain.runtime.tool.adapter.port.IToolCatalogPort;
import cn.xiaofuge.deepseek.harness.infrastructure.gateway.ToolCatalogGatewayService;
import cn.xiaofuge.deepseek.harness.infrastructure.gateway.dto.ToolCatalogRequestDTO;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * 工具目录端口适配器。
 * <p>
 * 执行流程：
 * 1. 接收领域层的工具筛选请求；
 * 2. 调用网关层或本地规则完成过滤；
 * 3. 返回可启用工具列表。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
@Component
public class ToolCatalogPort implements IToolCatalogPort {

    private final ToolCatalogGatewayService toolCatalogGatewayService;

    /**
     * 处理Tool Catalog Port相关逻辑。
     */
    public ToolCatalogPort(ToolCatalogGatewayService toolCatalogGatewayService) {
        this.toolCatalogGatewayService = toolCatalogGatewayService;
    }

    @Override
    public List<String> resolveEnabledTools(List<String> requestedTools) {
        return toolCatalogGatewayService.resolve(new ToolCatalogRequestDTO(requestedTools)).enabledTools();
    }
}
