package cn.xiaofuge.deepseek.harness.cases.plugin.lifecycle;

public interface PluginRuntimeCommand {

    String pluginId();

    PluginRuntimeKind kind();
}
