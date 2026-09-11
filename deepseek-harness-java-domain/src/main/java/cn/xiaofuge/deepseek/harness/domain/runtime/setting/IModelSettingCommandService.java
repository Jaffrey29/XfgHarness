package cn.xiaofuge.deepseek.harness.domain.runtime.setting;

import cn.xiaofuge.deepseek.harness.domain.runtime.setting.model.valobj.ModelSettingVO;

/**
 * 模型设置命令服务接口。
 * <p>
 * 执行流程：
 * 1. 上层提交新增或删除配置；
 * 2. 领域层校验并写入仓储；
 * 3. 返回最新设置或完成删除。
 *
 * <p>调用流程：应用领域规则 → 更新聚合或值对象 → 发布事件/返回结果。</p>
 * <p>示例：领域对象只暴露业务语言方法，不直接依赖基础设施。</p>
 * @author 小傅哥
 * @website bugstack.cn
 */
public interface IModelSettingCommandService {

    /**
     * 保存或更新模型设置（含渠道协议）。
     * <p>
     * 执行流程：
     * 1. 接收配置参数；
     * 2. 构造领域实体并写入仓储；
     * 3. 返回保存后的视图对象。
     *
     * @param protocol 渠道协议编码（openai / anthropic / ollama），空值按 openai 兼容处理
     */
    ModelSettingVO save(String profileCode, String providerCode, String modelCode,
                        String baseUrl, String apiKeyRef, String protocol, boolean enabled);

    /**
     * 保存或更新模型设置（兼容旧调用方，协议按 openai 处理）。
     */
    default ModelSettingVO save(String profileCode, String providerCode, String modelCode,
                                String baseUrl, String apiKeyRef, boolean enabled) {
        return save(profileCode, providerCode, modelCode, baseUrl, apiKeyRef, null, enabled);
    }

    /**
     * 按画像编码删除模型设置。
     * <p>
     * 执行流程：
     * 1. 读取 profileCode；
     * 2. 调用仓储删除；
     * 3. 结束处理。
     */
    void deleteByProfileCode(String profileCode);

    /**
     * 删除某 profile 下的指定模型；若删除的是当前生效模型，自动激活剩余的第一条。
     *
     * @param profileCode 画像编码
     * @param modelCode 模型编码
     */
    void deleteByProfileAndModel(String profileCode, String modelCode);

    /**
     * 将指定模型切换为该 profile 当前生效模型。
     *
     * @param profileCode 画像编码
     * @param modelCode 模型编码
     * @return 激活后的视图对象
     * @throws IllegalArgumentException 目标配置不存在时抛出
     */
    ModelSettingVO activate(String profileCode, String modelCode);
}
