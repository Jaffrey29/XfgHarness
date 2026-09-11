package cn.xiaofuge.deepseek.harness.api.dto;

/**
 * 删除模型设置请求。
 *
 * <p>调用流程：Controller/DTO → 应用门面 → 用例；响应对象由用例结果转换。</p>
 * <p>示例：DTO 只做协议隔离，不承载领域规则。</p>
 *
 * @param profileCode 画像编码（必填）
 * @param modelCode 模型编码（可空；为空时删除该 profile 下全部配置）
 * @author 小傅哥
 * @website bugstack.cn
 */
public record DeleteModelSettingRequestDTO(
        String profileCode,
        String modelCode
) {
}
