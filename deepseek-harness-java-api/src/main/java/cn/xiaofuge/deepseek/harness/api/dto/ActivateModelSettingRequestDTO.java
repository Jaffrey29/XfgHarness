package cn.xiaofuge.deepseek.harness.api.dto;

/**
 * 激活模型设置请求：将某 profile 下的指定模型切换为当前生效模型。
 *
 * <p>调用流程：Controller/DTO → 应用门面 → 用例；响应对象由用例结果转换。</p>
 * <p>示例：DTO 只做协议隔离，不承载领域规则。</p>
 *
 * @param profileCode 画像编码
 * @param modelCode 模型编码
 * @author 小傅哥
 * @website bugstack.cn
 */
public record ActivateModelSettingRequestDTO(
        String profileCode,
        String modelCode
) {
}
