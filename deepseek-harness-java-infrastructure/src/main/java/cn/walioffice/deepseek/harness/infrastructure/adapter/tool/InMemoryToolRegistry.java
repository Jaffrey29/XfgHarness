package cn.walioffice.deepseek.harness.infrastructure.adapter.tool;

import cn.walioffice.deepseek.harness.domain.tool.adapter.ToolRegistry;
import cn.walioffice.deepseek.harness.domain.tool.model.entity.ToolDefinition;
import cn.walioffice.deepseek.harness.domain.tool.model.entity.ToolExecutionInput;
import cn.walioffice.deepseek.harness.domain.tool.model.valobj.ToolExecutionMode;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory {@link ToolRegistry} implementation.
 * <p>
 * Tools register at startup (or dynamically via plugins) and are held in a
 * thread-safe map. This mirrors the TS {@code ToolRuntime} registry portion.
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public class InMemoryToolRegistry implements ToolRegistry {

    private final Map<String, ToolDefinition> tools = new ConcurrentHashMap<>();
    private final Map<String, Runnable> disposers = new ConcurrentHashMap<>();

    @Override
    public Runnable register(ToolDefinition definition) {
        String name = definition.name();
        if (tools.containsKey(name)) {
            throw new IllegalStateException("Tool already registered: " + name);
        }
        tools.put(name, definition);
        Runnable disposer = () -> {
            tools.remove(name);
            disposers.remove(name);
        };
        disposers.put(name, disposer);
        return disposer;
    }

    @Override
    public Optional<ToolDefinition> lookup(String name) {
        return Optional.ofNullable(tools.get(name));
    }

    @Override
    public Collection<ToolDefinition> all() {
        return tools.values();
    }

    @Override
    public ToolExecutionMode executionMode(ToolExecutionInput exec) {
        ToolDefinition def = tools.get(exec.name());
        if (def == null) return ToolExecutionMode.EXCLUSIVE;
        return def.isConcurrencySafe(exec.arguments())
                ? ToolExecutionMode.PARALLEL
                : ToolExecutionMode.EXCLUSIVE;
    }
}
