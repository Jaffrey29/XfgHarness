package cn.xiaofuge.deepseek.harness.domain.tool.mcp;

import cn.xiaofuge.deepseek.harness.domain.tool.adapter.ToolRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

/**
 * MCP 引导器：按配置连接 MCP server，并把远端工具以
 * {@code mcp__<server>__<tool>} 形式注册进共享 {@link ToolRegistry}。
 * <p>
 * 执行流程：
 * 1. 遍历 server 配置；
 * 2. 用客户端工厂创建 client（stdio 传输）并 connect + listTools；
 * 3. 每个远端工具包装成 {@link McpToolAdapter} 注册；
 * 4. 任一 server 失败不影响其余 server（记录到 {@link #errors()}）。
 * <p>
 * 支持运行期按 server 维度重连/断开（扩展管理热更新）：
 * {@link #connectServer} 返回注册工具数并记录每个工具的注销句柄，
 * {@link #disconnectServer} 负责注销工具并断开底层连接。
 * <p>
 * 对齐原版 TS mcp-client 的「连接监督 + 工具注入 registry」闭环。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public final class McpBootstrap {

    /** 客户端工厂（由基础设施层提供 stdio/http 实现）。 */
    @FunctionalInterface
    public interface McpClientFactory {
        IMcpClient create(McpServerConfig config);
    }

    private static final long DEFAULT_CALL_TIMEOUT_MS = 60_000;

    private final ToolRegistry registry;
    private final McpClientFactory clientFactory;
    private final Map<String, IMcpClient> clients = new ConcurrentHashMap<>();
    /** server name → 该 server 注册的全部工具注销句柄（重连/断开时逐一执行）。 */
    private final Map<String, List<Runnable>> disposers = new ConcurrentHashMap<>();
    private final List<String> errors = new CopyOnWriteArrayList<>();

    /**
     * 处理MCP Bootstrap。
     * 流程：应用领域规则 → 更新领域对象 → 返回结果。
     * @param registry registry参数。
     * @param clientFactory client Factory参数。
     * @return 方法执行结果。
     */
    public McpBootstrap(ToolRegistry registry, McpClientFactory clientFactory) {
        this.registry = registry;
        this.clientFactory = clientFactory;
    }

    /**
     * 连接并注册一个 MCP server 的全部工具。
     *
     * @return 注册的工具数
     */
    public int connectAndRegister(McpServerConfig config) {
        if (disposers.containsKey(config.name())) {
            // 已连接的 server 先断开，避免同名工具重复注册
            disconnectServer(config.name());
        }
        IMcpClient client = clientFactory.create(config);
        try {
            client.connect().get(30, TimeUnit.SECONDS);
            List<IMcpClient.McpToolDescriptor> tools = client.listTools().get(30, TimeUnit.SECONDS);
            clients.put(config.name(), client);
            List<Runnable> serverDisposers = new ArrayList<>(tools.size());
            for (IMcpClient.McpToolDescriptor tool : tools) {
                serverDisposers.add(registry.register(new McpToolAdapter(config.name(), tool, client, DEFAULT_CALL_TIMEOUT_MS)));
            }
            disposers.put(config.name(), serverDisposers);
            return tools.size();
        } catch (Exception e) {
            errors.add(config.name() + ": " + e.getMessage());
            try { client.disconnect(); } catch (Exception ignored) { }
            return 0;
        }
    }

    /** 逐个连接全部配置的 server，返回注册的工具总数。 */
    public int connectAll(List<McpServerConfig> configs) {
        int total = 0;
        for (McpServerConfig config : configs) {
            total += connectAndRegister(config);
        }
        return total;
    }

    /**
     * 断开并注销单个 server（幂等，未连接时返回 false）。
     */
    public boolean disconnectServer(String name) {
        List<Runnable> serverDisposers = disposers.remove(name);
        IMcpClient client = clients.remove(name);
        if (serverDisposers == null && client == null) {
            return false;
        }
        if (serverDisposers != null) {
            serverDisposers.forEach(Runnable::run);
        }
        if (client != null) {
            try { client.disconnect(); } catch (Exception ignored) { }
        }
        return true;
    }

    /** 判断某 server 当前是否已连接。 */
    public boolean isConnected(String name) {
        return clients.containsKey(name);
    }

    /** 当前已连接的 server 名称列表。 */
    public List<String> connectedServers() {
        return List.copyOf(clients.keySet());
    }

    /** 断开全部连接。 */
    public void disconnectAll() {
        for (String name : List.copyOf(clients.keySet())) {
            disconnectServer(name);
        }
    }

    /** 各 server 的连接错误（连接成功的不在其中）。 */
    public List<String> errors() {
        return List.copyOf(errors);
    }
}
