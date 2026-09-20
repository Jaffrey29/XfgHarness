package cn.xiaofuge.deepseek.harness.infrastructure.adapter.extension;

import cn.xiaofuge.deepseek.harness.domain.tool.adapter.ToolRegistry;
import cn.xiaofuge.deepseek.harness.domain.tool.mcp.IMcpClient;
import cn.xiaofuge.deepseek.harness.domain.tool.mcp.McpBootstrap;
import cn.xiaofuge.deepseek.harness.domain.tool.mcp.McpServerConfig;
import cn.xiaofuge.deepseek.harness.infrastructure.adapter.mcp.HttpMcpClient;
import cn.xiaofuge.deepseek.harness.infrastructure.adapter.mcp.StdioMcpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * MCP 运行时：持有共享 {@link McpBootstrap}，支持启动时全量连接和
 * 运行期按 server 增删改（扩展管理热更新）。
 *
 * <p>调用流程：触发层/工具 → 扩展管理服务 → 本运行时 → MCP server。</p>
 * @author 小傅哥
 * @website bugstack.cn
 */
public class McpRuntime {

    private static final Logger log = LoggerFactory.getLogger(McpRuntime.class);

    private static final long CALL_TIMEOUT_MS = 60_000;
    private static final long TEST_TIMEOUT_MS = 15_000;

    private final ToolRegistry toolRegistry;
    private final McpBootstrap bootstrap;
    private final Map<String, Integer> toolCounts = new ConcurrentHashMap<>();

    public McpRuntime(ToolRegistry toolRegistry) {
        this.toolRegistry = toolRegistry;
        this.bootstrap = new McpBootstrap(toolRegistry, this::createClient);
    }

    /** 启动时全量连接（由 McpBootstrapConfig 的 ApplicationRunner 调用）。 */
    public int connectAll(List<McpServerConfig> configs) {
        return bootstrap.connectAll(configs);
    }

    /** 连接单个 server 并注册工具；返回注册的工具数，失败抛出异常。 */
    public int connect(McpServerConfig config) {
        int count = bootstrap.connectAndRegister(config);
        if (!bootstrap.isConnected(config.name())) {
            List<String> errors = bootstrap.errors();
            String lastError = errors.isEmpty() ? "未知错误" : errors.get(errors.size() - 1);
            throw new IllegalStateException("MCP server 连接失败: " + lastError);
        }
        toolCounts.put(config.name(), count);
        return count;
    }

    /** 断开单个 server 并注销其工具（幂等）。 */
    public boolean disconnect(String name) {
        boolean removed = bootstrap.disconnectServer(name);
        toolCounts.remove(name);
        return removed;
    }

    /** 断开全部（应用关闭时释放子进程）。 */
    public void disconnectAll() {
        bootstrap.disconnectAll();
        toolCounts.clear();
    }

    public boolean isConnected(String name) {
        return bootstrap.isConnected(name);
    }

    /** server name → 已注册工具数（仅含连接成功的）。 */
    public Map<String, Integer> status() {
        Map<String, Integer> status = new LinkedHashMap<>();
        for (String name : bootstrap.connectedServers()) {
            status.put(name, toolCounts.getOrDefault(name, 0));
        }
        return status;
    }

    /**
     * 连通性测试：临时连接、列工具、断开，不注册任何工具。
     */
    public ExtensionMcpProbe probe(McpServerConfig config) {
        IMcpClient client = createClient(config);
        try {
            client.connect().get(TEST_TIMEOUT_MS, TimeUnit.SECONDS);
            List<IMcpClient.McpToolDescriptor> tools = client.listTools().get(TEST_TIMEOUT_MS, TimeUnit.SECONDS);
            List<String> names = tools.stream().map(IMcpClient.McpToolDescriptor::name).toList();
            return new ExtensionMcpProbe(true, names, "");
        } catch (Exception e) {
            String message = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            return new ExtensionMcpProbe(false, List.of(), message);
        } finally {
            try { client.disconnect(); } catch (Exception ignored) { }
        }
    }

    /** 测试结果探针。 */
    public record ExtensionMcpProbe(boolean success, List<String> tools, String message) {}

    private IMcpClient createClient(McpServerConfig config) {
        if ("stdio".equals(config.transport())) {
            return new StdioMcpClient(config.command(), config.args(), config.env(), config.cwd(), CALL_TIMEOUT_MS);
        }
        return new HttpMcpClient(config.transport(), config.url(), config.headers(), CALL_TIMEOUT_MS);
    }
}
