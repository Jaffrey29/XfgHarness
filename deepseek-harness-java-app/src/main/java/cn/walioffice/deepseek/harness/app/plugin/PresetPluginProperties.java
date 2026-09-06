package cn.walioffice.deepseek.harness.app.plugin;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 绑定 application.yml 中的 {@code harness.plugin.preset} 插件列表。
 * 启动时未注册的预置插件会被自动安装。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
@Configuration
@ConfigurationProperties(prefix = "harness.plugin.preset")
public class PresetPluginProperties {

    private List<PresetPlugin> plugins = new ArrayList<>();

    public List<PresetPlugin> getPlugins() {
        return plugins;
    }

    public void setPlugins(List<PresetPlugin> plugins) {
        this.plugins = plugins;
    }

    public static class PresetPlugin {
        private String pluginId;
        private String displayName;
        private String pluginVersion;
        private String runtimeType;
        private String sourcePath;
        private String entrypoint;
        private boolean autoEnable = true;

        public String getPluginId() { return pluginId; }
        public void setPluginId(String pluginId) { this.pluginId = pluginId; }
        public String getDisplayName() { return displayName; }
        public void setDisplayName(String displayName) { this.displayName = displayName; }
        public String getPluginVersion() { return pluginVersion; }
        public void setPluginVersion(String pluginVersion) { this.pluginVersion = pluginVersion; }
        public String getRuntimeType() { return runtimeType; }
        public void setRuntimeType(String runtimeType) { this.runtimeType = runtimeType; }
        public String getSourcePath() { return sourcePath; }
        public void setSourcePath(String sourcePath) { this.sourcePath = sourcePath; }
        public String getEntrypoint() { return entrypoint; }
        public void setEntrypoint(String entrypoint) { this.entrypoint = entrypoint; }
        public boolean isAutoEnable() { return autoEnable; }
        public void setAutoEnable(boolean autoEnable) { this.autoEnable = autoEnable; }
    }
}
