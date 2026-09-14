package cn.xiaofuge.deepseek.harness.cases.plugin;

import cn.xiaofuge.deepseek.harness.api.dto.PluginInventoryResponseDTO;
import org.springframework.stereotype.Service;

@Service
public class PluginInventoryQueryCaseImpl implements IPluginInventoryQueryCase {

    private final PluginInventoryQueryFactory pluginInventoryQueryFactory;

    public PluginInventoryQueryCaseImpl(PluginInventoryQueryFactory pluginInventoryQueryFactory) {
        this.pluginInventoryQueryFactory = pluginInventoryQueryFactory;
    }

    @Override
    public PluginInventoryResponseDTO inventory() {
        return pluginInventoryQueryFactory.inventory();
    }
}
