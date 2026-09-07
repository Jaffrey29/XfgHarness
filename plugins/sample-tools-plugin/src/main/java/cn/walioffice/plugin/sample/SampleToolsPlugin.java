package cn.walioffice.plugin.sample;

import cn.xiaofuge.deepseek.harness.domain.model.entity.ToolDefinition;
import cn.xiaofuge.deepseek.harness.domain.spi.AbstractHarnessPlugin;
import java.util.List;

/**
 * 示例插件入口：提供天气查询和文件摘要两个工具。
 * <p>
 * 继承 {@link AbstractHarnessPlugin}，只需传 pluginId 并实现 {@link #tools()}。
 * 通过 application.yml preset 预装，启动即可用。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public class SampleToolsPlugin extends AbstractHarnessPlugin {

    public SampleToolsPlugin() {
        super("sample-tools");
    }

    @Override
    public List<ToolDefinition> tools() {
        return List.of(new WeatherQueryTool(), new FileSummaryTool());
    }
}
