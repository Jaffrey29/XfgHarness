package cn.xiaofuge.deepseek.harness.domain.runtime.tool.service;

import cn.xiaofuge.deepseek.harness.domain.runtime.IToolResolutionService;
import cn.xiaofuge.deepseek.harness.domain.runtime.tool.adapter.port.IToolCatalogPort;
import java.util.List;

/**
 * 工具解析服务。
 * <p>
 * 执行流程：
 * 1. 接收用户请求的工具集合；
 * 2. 调用工具目录端口完成过滤；
 * 3. 返回最终可启用的工具列表。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public class ToolResolutionService implements IToolResolutionService {

    private final IToolCatalogPort toolCatalogPort;

    public ToolResolutionService(IToolCatalogPort toolCatalogPort) {
        this.toolCatalogPort = toolCatalogPort;
    }

    /**
     * 解析可启用工具。
     * <p>
     * 执行流程：
     * 1. 读取请求工具列表；
     * 2. 调用工具目录端口过滤；
     * 3. 返回过滤后的工具列表。
     */
    @Override
    public List<String> resolveEnabledTools(List<String> requestedTools) {
        return toolCatalogPort.resolveEnabledTools(requestedTools);
    }
}
