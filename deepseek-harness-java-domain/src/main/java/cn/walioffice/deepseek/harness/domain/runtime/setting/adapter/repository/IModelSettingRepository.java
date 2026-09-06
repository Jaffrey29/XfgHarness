package cn.walioffice.deepseek.harness.domain.runtime.setting.adapter.repository;

import cn.walioffice.deepseek.harness.domain.runtime.setting.model.entity.ModelSettingEntity;
import java.util.List;
import java.util.Optional;

/**
 * 模型设置仓储接口。
 * <p>
 * 执行流程：
 * 1. 领域服务发起模型设置读写；
 * 2. 基础设施层负责持久化访问；
 * 3. 返回领域实体或完成写入。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public interface IModelSettingRepository {

    /**
     * 保存模型设置。
     * <p>
     * 执行流程：
     * 1. 接收领域实体；
     * 2. 映射并写入持久层；
     * 3. 完成后返回。
     */
    void save(ModelSettingEntity setting);

    /**
     * 查询全部模型设置。
     * <p>
     * 执行流程：
     * 1. 从持久层读取全部记录；
     * 2. 转换为领域实体；
     * 3. 返回实体列表。
     */
    List<ModelSettingEntity> queryAll();

    /**
     * 按画像编码查询模型设置。
     * <p>
     * 执行流程：
     * 1. 根据 profileCode 定位记录；
     * 2. 转换为领域实体；
     * 3. 返回可选结果。
     */
    Optional<ModelSettingEntity> findByProfileCode(String profileCode);

    /**
     * 按画像编码删除模型设置。
     * <p>
     * 执行流程：
     * 1. 读取 profileCode；
     * 2. 删除持久化记录；
     * 3. 返回删除结果。
     */
    void deleteByProfileCode(String profileCode);
}
