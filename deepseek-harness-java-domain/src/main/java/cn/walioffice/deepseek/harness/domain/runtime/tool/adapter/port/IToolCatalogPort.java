package cn.walioffice.deepseek.harness.domain.runtime.tool.adapter.port;

import java.util.List;

/**
 * 工具目录端口。
 * <p>
 * 执行流程：
 * 1. 领域层传入请求工具集合；
 * 2. 基础设施层根据目录和策略进行筛选；
 * 3. 返回允许启用的工具列表。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public interface IToolCatalogPort {

    /**
     * 在工具策略过滤后返回可启用工具列表。
     * <p>
     * 执行流程：
     * 1. 读取 requestedTools；
     * 2. 对照工具目录和权限策略过滤；
     * 3. 返回最终可执行工具编码。
     *
     * @param requestedTools 调用方请求的工具编码
     * @return 可启用的工具编码
     */
    List<String> resolveEnabledTools(List<String> requestedTools);
}
