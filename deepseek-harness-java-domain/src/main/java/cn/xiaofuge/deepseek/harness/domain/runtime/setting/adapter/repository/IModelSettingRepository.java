package cn.xiaofuge.deepseek.harness.domain.runtime.setting.adapter.repository;

import cn.xiaofuge.deepseek.harness.domain.runtime.setting.model.entity.ModelSettingEntity;
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
 * <p>调用流程：应用领域规则 → 更新聚合或值对象 → 发布事件/返回结果。</p>
 * <p>示例：领域对象只暴露业务语言方法，不直接依赖基础设施。</p>
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
     * 按画像编码与模型编码查询模型设置。
     * <p>
     * 执行流程：
     * 1. 根据 profileCode + modelCode 定位记录；
     * 2. 转换为领域实体；
     * 3. 返回可选结果。
     */
    Optional<ModelSettingEntity> findByProfileAndModel(String profileCode, String modelCode);

    /**
     * 将指定模型置为该 profile 当前生效项（同 profile 其他行取消激活）。
     * <p>
     * 执行流程：
     * 1. 清除该 profile 下所有行的激活标记；
     * 2. 激活目标行；
     * 3. 返回是否存在目标行。
     */
    boolean activate(String profileCode, String modelCode);

    /**
     * 删除某 profile 下的指定模型。
     * <p>
     * 执行流程：
     * 1. 读取 profileCode + modelCode；
     * 2. 删除持久化记录；
     * 3. 返回删除结果。
     */
    void deleteByProfileAndModel(String profileCode, String modelCode);

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
