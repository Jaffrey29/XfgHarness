package cn.walioffice.deepseek.harness.domain.runtime.setting;

import cn.walioffice.deepseek.harness.domain.runtime.setting.model.valobj.ModelSettingVO;

/**
 * 模型设置命令服务接口。
 * <p>
 * 执行流程：
 * 1. 上层提交新增或删除配置；
 * 2. 领域层校验并写入仓储；
 * 3. 返回最新设置或完成删除。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public interface IModelSettingCommandService {

    /**
     * 保存或更新模型设置。
     * <p>
     * 执行流程：
     * 1. 接收配置参数；
     * 2. 构造领域实体并写入仓储；
     * 3. 返回保存后的视图对象。
     */
    ModelSettingVO save(String profileCode, String providerCode, String modelCode, String baseUrl, String apiKeyRef, boolean enabled);

    /**
     * 按画像编码删除模型设置。
     * <p>
     * 执行流程：
     * 1. 读取 profileCode；
     * 2. 调用仓储删除；
     * 3. 结束处理。
     */
    void deleteByProfileCode(String profileCode);
}
