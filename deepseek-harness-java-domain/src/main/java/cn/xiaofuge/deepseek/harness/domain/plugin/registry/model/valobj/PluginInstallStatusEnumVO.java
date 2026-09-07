package cn.xiaofuge.deepseek.harness.domain.plugin.registry.model.valobj;

/**
 * Enumerates the lifecycle states for one installed plugin.
 *
 * <p>调用流程：插件安装/激活 → Bridge 转换 → Runtime 执行 → Hook/事件回传。</p>
 * <p>示例：Java SPI 插件注册工具后，Agent 可通过工具名调用其 run 方法。</p>
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
