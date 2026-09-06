package cn.walioffice.deepseek.harness.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MCP server 配置绑定。
 * <p>
 * 对应 {@code harness.mcp.servers} 配置块。每个 server 是一个 stdio 子进程：
 * <pre>
 * harness:
 *   mcp:
 *     servers:
 *       - name: filesystem
 *         command: npx
 *         args: ["-y", "@modelcontextprotocol/server-filesystem", "/tmp"]
 *         env:
 *           SOME_KEY: value
 *         cwd: /optional/working/dir
 * </pre>
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
@ConfigurationProperties(prefix = "harness.mcp")
public class McpServersProperties {

    /** MCP server 列表。 */
    private List<McpServer> servers = new ArrayList<>();

    public List<McpServer> getServers() {
        return servers;
    }

    public void setServers(List<McpServer> servers) {
        this.servers = servers != null ? servers : new ArrayList<>();
    }

    /** 单个 MCP server 的 stdio 启动配置。 */
    public static class McpServer {
        /** server 名（工具命名为 {@code mcp__<name>__<tool>}）。 */
        private String name;
        /** 启动命令（如 npx / node / python）。 */
        private String command;
        /** 命令参数。 */
        private List<String> args = new ArrayList<>();
        /** 附加环境变量。 */
        private Map<String, String> env = new HashMap<>();
        /** 子进程工作目录。 */
        private String cwd;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getCommand() { return command; }
        public void setCommand(String command) { this.command = command; }
        public List<String> getArgs() { return args; }
        public void setArgs(List<String> args) { this.args = args != null ? args : new ArrayList<>(); }
        public Map<String, String> getEnv() { return env; }
        public void setEnv(Map<String, String> env) { this.env = env != null ? env : new HashMap<>(); }
        public String getCwd() { return cwd; }
        public void setCwd(String cwd) { this.cwd = cwd; }
    }
}
