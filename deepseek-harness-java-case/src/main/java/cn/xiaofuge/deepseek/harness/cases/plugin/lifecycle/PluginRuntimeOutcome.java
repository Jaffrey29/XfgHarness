package cn.xiaofuge.deepseek.harness.cases.plugin.lifecycle;

import cn.xiaofuge.deepseek.harness.api.dto.ActivatePluginResponseDTO;
import cn.xiaofuge.deepseek.harness.api.dto.RunPluginResponseDTO;

public sealed interface PluginRuntimeOutcome {

    record Activation(ActivatePluginResponseDTO result) implements PluginRuntimeOutcome {
    }

    record Run(RunPluginResponseDTO result) implements PluginRuntimeOutcome {
    }
}
