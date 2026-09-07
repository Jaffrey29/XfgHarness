package cn.xiaofuge.deepseek.harness.domain.agent.subagent;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Service;

/**
 * Registry for subagent providers.
 * <p>
 * Mirrors TS {@code ctx.subagents}: a service that routes subagent
 * start requests to the appropriate provider by name.
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
@Service
public class SubagentRegistry {

    private final Map<String, SubagentProvider> providers = new ConcurrentHashMap<>();
    private final AtomicInteger runIdCounter = new AtomicInteger(0);

    /**
     * 处理register相关逻辑。
     */
    public void register(SubagentProvider provider) {
        providers.put(provider.name(), provider);
    }

    /**
     * 处理unregister相关逻辑。
     */
    public void unregister(String name) {
        providers.remove(name);
    }

    /**
     * 解析e。
     */
    public SubagentProvider resolve(String name) {
        SubagentProvider p = providers.get(name);
        if (p == null) {
            throw new IllegalArgumentException("no subagent provider registered for: " + name);
        }
        return p;
    }

    /**
     * 列出Provider Names。
     */
    public List<String> listProviderNames() {
        return List.copyOf(providers.keySet());
    }

    /**
     * 处理next Run Id相关逻辑。
     */
    public String nextRunId() {
        return "run-" + runIdCounter.incrementAndGet();
    }
}
