package cn.xiaofuge.deepseek.harness.app.plugin;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 绑定 application.yml 中的 {@code harness.plugin.preset} 插件列表。
 * 启动时未注册的预置插件会被自动安装。
 *
 * <p>调用流程：Spring Boot 启动 → 加载模型/插件预设 → 注册运行时组件。</p>
 * <p>示例：启动器同步默认模型并装载预置插件。</p>
 * @author 小傅哥
 * @website bugstack.cn
 */
@Configuration
@ConfigurationProperties(prefix = "harness.plugin.preset")
public class PresetPluginProperties {

    private List<PresetPlugin> plugins = new ArrayList<>();

    /**
     * 获取Plugins。
     */
    public List<PresetPlugin> getPlugins() {
        return plugins;
    }

    /**
     * 设置Plugins。
     */
    public void setPlugins(List<PresetPlugin> plugins) {
        this.plugins = plugins;
    }

    /**
     * PresetPlugin 定义当前模块中的class契约与职责。
     */
    public static class PresetPlugin {
        private String pluginId;
        private String displayName;
        private String pluginVersion;
        private String runtimeType;
        private String sourcePath;
    private String entrypoint;
    private Boolean autoStart;
    private boolean autoEnable = true;

        /**
         * 获取Plugin Id。
         */
        public String getPluginId() { return pluginId; }
        /**
         * 设置Plugin Id。
         */
        public void setPluginId(String pluginId) { this.pluginId = pluginId; }
        /**
         * 获取Display Name。
         */
        public String getDisplayName() { return displayName; }
        /**
         * 设置Display Name。
         */
        public void setDisplayName(String displayName) { this.displayName = displayName; }
        /**
         * 获取Plugin Version。
         */
        public String getPluginVersion() { return pluginVersion; }
        /**
         * 设置Plugin Version。
         */
        public void setPluginVersion(String pluginVersion) { this.pluginVersion = pluginVersion; }
        /**
         * 获取Runtime Type。
         */
        public String getRuntimeType() { return runtimeType; }
        /**
         * 设置Runtime Type。
         */
        public void setRuntimeType(String runtimeType) { this.runtimeType = runtimeType; }
        /**
         * 获取Source Path。
         */
        public String getSourcePath() { return sourcePath; }
        /**
         * 设置Source Path。
         */
        public void setSourcePath(String sourcePath) { this.sourcePath = sourcePath; }
        /**
         * 获取Entrypoint。
         */
        public String getEntrypoint() { return entrypoint; }
        /**
         * 设置Entrypoint。
         */
        public void setEntrypoint(String entrypoint) { this.entrypoint = entrypoint; }
        /**
         * 判断Auto Start是否成立。优先读 autoStart，为 null 时回退到 autoEnable。
         */
        public boolean isAutoStart() { return autoStart != null ? autoStart : autoEnable; }
        public Boolean getAutoStart() { return autoStart; }
        public void setAutoStart(Boolean autoStart) { this.autoStart = autoStart; }

        /** @deprecated use {@link #isAutoStart()} instead */
        @Deprecated
        public boolean isAutoEnable() { return autoEnable; }
        /** @deprecated use {@link #setAutoStart(Boolean)} instead */
        @Deprecated
        public void setAutoEnable(boolean autoEnable) { this.autoEnable = autoEnable; }
    }
}
