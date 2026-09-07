package cn.xiaofuge.deepseek.harness.cases.plugin.lifecycle;

public record PluginExecutionInput(String pluginId, PluginRuntimeKind kind) implements PluginRuntimeCommand {
}
