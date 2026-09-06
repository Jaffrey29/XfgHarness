package cn.walioffice.deepseek.harness.domain.plugin.registry.model.valobj;

/**
 * Enumerates the lifecycle states for one installed plugin.
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public enum PluginInstallStatusEnumVO {
    REGISTERED,
    ACTIVE,
    FAILED,
    DISABLED,
    UNINSTALLED
}
