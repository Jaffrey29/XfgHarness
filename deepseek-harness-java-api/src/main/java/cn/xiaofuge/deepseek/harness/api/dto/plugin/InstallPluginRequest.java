package cn.xiaofuge.deepseek.harness.api.dto.plugin;

/**
 * 插件安装请求。
 *
 * @param pluginId 插件 ID
 * @param displayName 插件显示名
 * @param pluginVersion 插件版本
 * @param runtimeType 插件运行时类型
 * @param sourcePath 插件本地来源路径
 * @param entrypoint 插件入口文件或符号
 *
 * <p>调用流程：Controller/DTO → 应用门面 → 用例；响应对象由用例结果转换。</p>
 * <p>示例：DTO 只做协议隔离，不承载领域规则。</p>
 * @author 小傅哥
 * @website bugstack.cn
 */
public record InstallPluginRequest(
        String pluginId,
        String displayName,
        String pluginVersion,
        String runtimeType,
        String sourcePath,
        String entrypoint
) {
}
