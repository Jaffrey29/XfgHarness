package cn.xiaofuge.deepseek.harness.domain.runtime;

import java.util.List;

/**
 * 工具解析服务接口。
 * <p>
 * 执行流程：
 * 1. 接收用户请求的工具编码；
 * 2. 根据目录和策略进行过滤；
 * 3. 返回最终允许启用的工具列表。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public interface IToolResolutionService {

    /**
     * 解析可启用工具列表。
     * <p>
     * 执行流程：
     * 1. 读取 requestedTools；
     * 2. 过滤掉不允许执行的工具；
     * 3. 返回最终可启用的工具编码。
     *
     * @param requestedTools 调用方请求的工具编码
     * @return 可启用的工具编码
     */
    List<String> resolveEnabledTools(List<String> requestedTools);
}
