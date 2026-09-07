package cn.xiaofuge.deepseek.harness.domain.plugin.registry.model.entity;

import cn.xiaofuge.deepseek.harness.domain.plugin.registry.model.valobj.PluginInstallModeEnumVO;
import cn.xiaofuge.deepseek.harness.domain.plugin.registry.model.valobj.PluginInstallStatusEnumVO;
import cn.xiaofuge.deepseek.harness.domain.plugin.registry.model.valobj.PluginRuntimeTypeEnumVO;
import java.time.Instant;
import java.util.Objects;

/**
 * Stores one installed plugin registration.
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public class HarnessPluginEntity {

    private final String pluginId;
    private final String displayName;
    private final String pluginVersion;
    private final PluginRuntimeTypeEnumVO runtimeType;
    private final PluginInstallModeEnumVO installMode;
    private final String sourcePath;
    private final String entrypoint;
    private PluginInstallStatusEnumVO status;
    private final Instant installedAt;

    /**
     * 处理Harness Plugin Entity相关逻辑。
     */
    public HarnessPluginEntity(
            String pluginId,
            String displayName,
            String pluginVersion,
            PluginRuntimeTypeEnumVO runtimeType,
            PluginInstallModeEnumVO installMode,
            String sourcePath,
            String entrypoint,
            PluginInstallStatusEnumVO status,
            Instant installedAt
    ) {
        this.pluginId = Objects.requireNonNull(pluginId, "pluginId");
        this.displayName = Objects.requireNonNull(displayName, "displayName");
        this.pluginVersion = Objects.requireNonNull(pluginVersion, "pluginVersion");
        this.runtimeType = Objects.requireNonNull(runtimeType, "runtimeType");
        this.installMode = Objects.requireNonNull(installMode, "installMode");
        this.sourcePath = Objects.requireNonNull(sourcePath, "sourcePath");
        this.entrypoint = Objects.requireNonNull(entrypoint, "entrypoint");
        this.status = Objects.requireNonNull(status, "status");
        this.installedAt = Objects.requireNonNull(installedAt, "installedAt");
    }

    /**
     * 获取Plugin Id。
     */
    public String getPluginId() { return pluginId; }
    /**
     * 获取Display Name。
     */
    public String getDisplayName() { return displayName; }
    /**
     * 获取Plugin Version。
     */
    public String getPluginVersion() { return pluginVersion; }
    /**
     * 获取Runtime Type。
     */
    public PluginRuntimeTypeEnumVO getRuntimeType() { return runtimeType; }
    /**
     * 获取Install Mode。
     */
    public PluginInstallModeEnumVO getInstallMode() { return installMode; }
    /**
     * 获取Source Path。
     */
    public String getSourcePath() { return sourcePath; }
    /**
     * 获取Entrypoint。
     */
    public String getEntrypoint() { return entrypoint; }
    /**
     * 获取Status。
     */
    public PluginInstallStatusEnumVO getStatus() { return status; }
    /**
     * 获取Installed At。
     */
    public Instant getInstalledAt() { return installedAt; }

    /**
     * Marks the plugin active after a successful activation phase.
     */
    public void activate() { this.status = PluginInstallStatusEnumVO.ACTIVE; }

    /**
     * Marks the plugin failed after an activation or bridge check failure.
     */
    public void fail() { this.status = PluginInstallStatusEnumVO.FAILED; }

    /**
     * Marks the plugin disabled — kept on disk but not available for execution.
     */
    public void disable() { this.status = PluginInstallStatusEnumVO.DISABLED; }

    /**
     * Marks the plugin enabled — back to registered, ready for activation.
     */
    public void enable() { this.status = PluginInstallStatusEnumVO.REGISTERED; }

    /**
     * Marks the plugin uninstalled — record kept for audit but no longer loadable.
     */
    public void uninstall() { this.status = PluginInstallStatusEnumVO.UNINSTALLED; }
}
