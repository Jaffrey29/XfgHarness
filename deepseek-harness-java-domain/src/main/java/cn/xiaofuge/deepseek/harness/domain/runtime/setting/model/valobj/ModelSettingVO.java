package cn.xiaofuge.deepseek.harness.domain.runtime.setting.model.valobj;

import java.time.Instant;

/**
 * 模型设置视图对象。
 * <p>
 * 执行流程：
 * 1. 仓储读出模型设置实体；
 * 2. 领域服务转换成视图对象；
 * 3. API 层直接映射给前端响应。
 *
 * @param profileCode 执行画像编码
 * @param providerCode 提供方编码
 * @param modelCode 模型编码
 * @param baseUrl 提供方基础地址
 * @param apiKeyRef API Key 引用
 * @param enabled 是否启用
 * @param updatedAt 更新时间
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public record ModelSettingVO(
        String profileCode,
        String providerCode,
        String modelCode,
        String baseUrl,
        String apiKeyRef,
        boolean enabled,
        Instant updatedAt
) {
}
