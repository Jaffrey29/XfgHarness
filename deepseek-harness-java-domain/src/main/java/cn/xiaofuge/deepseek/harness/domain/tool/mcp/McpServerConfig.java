package cn.xiaofuge.deepseek.harness.domain.tool.mcp;

import java.util.List;
import java.util.Map;

/**
 * 一个 MCP server 的连接配置。
 *
 * @param name    server 名（工具名前缀 mcp__&lt;name&gt;__*）
 * @param command 可执行命令（stdio 传输）
 * @param args    命令参数
 * @param env     追加环境变量
 * @param cwd     工作目录（可空）
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public record McpServerConfig(
        String name,
        String command,
        List<String> args,
        Map<String, String> env,
        String cwd
) {
    public McpServerConfig {
        args = args == null ? List.of() : List.copyOf(args);
        env = env == null ? Map.of() : Map.copyOf(env);
    }
}
