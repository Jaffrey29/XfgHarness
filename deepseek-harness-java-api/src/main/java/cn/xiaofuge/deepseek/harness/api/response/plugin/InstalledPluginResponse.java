package cn.xiaofuge.deepseek.harness.api.response.plugin;

import java.time.Instant;

/**
 * 返回 Web 控制台使用的一条已安装插件记录。
 *
 * <p>调用流程：Controller/DTO → 应用门面 → 用例；响应对象由用例结果转换。</p>
 * <p>示例：DTO 只做协议隔离，不承载领域规则。</p>
 * @author 小傅哥
 * @website bugstack.cn
 */
public record InstalledPluginResponse(
        String pluginId,
        String displayName,
        String pluginVersion,
        String runtimeType,
        String installMode,
        String status,
        String sourcePath,
        String entrypoint,
        Instant installedAt
) {
}
