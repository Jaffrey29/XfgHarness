package ${package};

import cn.walioffice.deepseek.harness.domain.plugin.spi.AbstractHarnessPlugin;
import cn.walioffice.deepseek.harness.domain.tool.model.entity.ToolDefinition;
import java.util.List;

/**
 * ${pluginName} — ${pluginDescription}
 * <p>
 * 继承 {@link AbstractHarnessPlugin}，只需传 pluginId 并实现 {@link #tools()}。
 * pluginId 必须与 META-INF/plugin.yaml 的 id 字段一致。
 */
public class ${pluginClass} extends AbstractHarnessPlugin {

    public ${pluginClass}() {
        super("${pluginId}");
    }

    @Override
    public List<ToolDefinition> tools() {
        return List.of(new ExampleTool());
    }

    // 如需初始化/释放资源，override onStart() / onStop()
}
