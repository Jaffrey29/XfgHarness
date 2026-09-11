package cn.xiaofuge.deepseek.harness.domain.runtime.setting.service;

import cn.xiaofuge.deepseek.harness.domain.runtime.setting.IModelSettingCommandService;
import cn.xiaofuge.deepseek.harness.domain.runtime.setting.IModelSettingQueryService;
import cn.xiaofuge.deepseek.harness.domain.runtime.setting.adapter.repository.IModelSettingRepository;
import cn.xiaofuge.deepseek.harness.domain.runtime.setting.model.entity.ModelSettingEntity;
import cn.xiaofuge.deepseek.harness.domain.runtime.setting.model.valobj.ModelSettingVO;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * 模型设置领域服务。
 * <p>
 * 执行流程：
 * 1. 从仓储读取或保存模型设置；
 * 2. 将实体转换为值对象；
 * 3. 供查询和命令用例共同使用。
 *
 * <p>调用流程：应用领域规则 → 更新聚合或值对象 → 发布事件/返回结果。</p>
 * <p>示例：领域对象只暴露业务语言方法，不直接依赖基础设施。</p>
 * @author 小傅哥
 * @website bugstack.cn
 */
@Service
public class ModelSettingService implements IModelSettingQueryService, IModelSettingCommandService {

    private final IModelSettingRepository modelSettingRepository;

    /**
     * 处理模型 Setting Service。
     * 流程：应用领域规则 → 更新领域对象 → 返回结果。
     * @param modelSettingRepository 模型 Setting Repository参数。
     * @return 方法执行结果。
     */
    public ModelSettingService(IModelSettingRepository modelSettingRepository) {
        this.modelSettingRepository = modelSettingRepository;
    }

    /**
     * 查询全部模型设置。
     * <p>
     * 执行流程：
     * 1. 从仓储读取实体列表；
     * 2. 映射为值对象；
     * 3. 返回列表。
     */
    @Override
    public List<ModelSettingVO> queryAll() {
        return modelSettingRepository.queryAll().stream().map(this::toValue).toList();
    }

    /**
     * 按画像编码查询模型设置。
     * <p>
     * 执行流程：
     * 1. 调用仓储定位实体；
     * 2. 转换为值对象；
     * 3. 返回可选结果。
     */
    @Override
    public Optional<ModelSettingVO> findByProfileCode(String profileCode) {
        return modelSettingRepository.findByProfileCode(profileCode).map(this::toValue);
    }

    /**
     * 保存模型设置。
     * <p>
     * 执行流程：
     * 1. 构造模型设置实体；
     * 2. 若该 profile 尚无激活行，则本次保存自动置为当前生效模型；
     * 3. 写入仓储；
     * 4. 返回保存后的值对象。
     */
    @Override
    public ModelSettingVO save(String profileCode, String providerCode, String modelCode,
                               String baseUrl, String apiKeyRef, String protocol, boolean enabled) {
        boolean hasActive = modelSettingRepository.queryAll().stream()
                .anyMatch(e -> e.getProfileCode().equals(profileCode) && e.isActive());
        ModelSettingEntity entity = ModelSettingEntity.create(
                profileCode, providerCode, modelCode, baseUrl, apiKeyRef, protocol, enabled, !hasActive);
        modelSettingRepository.save(entity);
        return toValue(entity);
    }

    /**
     * 删除指定模型设置。
     * <p>
     * 执行流程：
     * 1. 读取 profileCode；
     * 2. 调用仓储删除；
     * 3. 结束处理。
     */
    @Override
    public void deleteByProfileCode(String profileCode) {
        modelSettingRepository.deleteByProfileCode(profileCode);
    }

    /**
     * 删除某 profile 下的指定模型。
     * <p>
     * 执行流程：
     * 1. 记录被删行是否为当前生效模型；
     * 2. 删除该行；
     * 3. 若删除的是生效模型且该 profile 还有剩余配置，自动激活剩余的第一条。
     */
    @Override
    public void deleteByProfileAndModel(String profileCode, String modelCode) {
        boolean wasActive = modelSettingRepository.findByProfileAndModel(profileCode, modelCode)
                .map(ModelSettingEntity::isActive)
                .orElse(false);
        modelSettingRepository.deleteByProfileAndModel(profileCode, modelCode);
        if (!wasActive) {
            return;
        }
        modelSettingRepository.queryAll().stream()
                .filter(e -> e.getProfileCode().equals(profileCode))
                .findFirst()
                .ifPresent(next -> modelSettingRepository.activate(profileCode, next.getModelCode()));
    }

    /**
     * 切换某 profile 的当前生效模型。
     * <p>
     * 执行流程：
     * 1. 校验目标配置存在；
     * 2. 调用仓储完成激活（同事务先清同 profile 激活标记）；
     * 3. 返回激活后的值对象。
     */
    @Override
    public ModelSettingVO activate(String profileCode, String modelCode) {
        ModelSettingEntity target = modelSettingRepository.findByProfileAndModel(profileCode, modelCode)
                .orElseThrow(() -> new IllegalArgumentException(
                        "model setting not found: profile=" + profileCode + ", model=" + modelCode));
        modelSettingRepository.activate(profileCode, modelCode);
        return toValue(target);
    }

    /**
     * 将实体转换为值对象。
     * <p>
     * 执行流程：
     * 1. 读取实体字段；
     * 2. 封装为 VO；
     * 3. 返回给调用方。
     */
    private ModelSettingVO toValue(ModelSettingEntity entity) {
        return new ModelSettingVO(
                entity.getProfileCode(),
                entity.getProviderCode(),
                entity.getModelCode(),
                entity.getBaseUrl(),
                entity.getApiKeyRef(),
                entity.getProtocol(),
                entity.isEnabled(),
                entity.isActive(),
                entity.getUpdatedAt()
        );
    }

}
