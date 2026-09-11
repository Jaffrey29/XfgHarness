package cn.xiaofuge.deepseek.harness.infrastructure.adapter.repository;

import cn.xiaofuge.deepseek.harness.domain.runtime.setting.adapter.repository.IModelSettingRepository;
import cn.xiaofuge.deepseek.harness.domain.runtime.setting.model.entity.ModelSettingEntity;
import cn.xiaofuge.deepseek.harness.infrastructure.dao.IHarnessModelSettingDao;
import cn.xiaofuge.deepseek.harness.infrastructure.dao.po.HarnessModelSettingPO;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * 模型设置仓储实现。
 * <p>
 * 执行流程：
 * 1. 接收领域层的模型设置实体；
 * 2. 通过 DAO 层完成持久化；
 * 3. 读取时再映射回领域实体。
 *
 * <p>调用流程：领域端口 → 基础设施适配器 → 外部系统。</p>
 * <p>示例：适配器只实现端口契约，业务规则保留在 domain。</p>
 * @author 小傅哥
 * @website bugstack.cn
 */
@Repository
public class ModelSettingRepository implements IModelSettingRepository {

    private final IHarnessModelSettingDao harnessModelSettingDao;

    /**
     * 处理模型 Setting Repository。
     * 流程：适配外部协议或资源 → 调用底层能力 → 返回结果。
     * @param harnessModelSettingDao harness 模型 Setting Dao参数。
     * @return 方法执行结果。
     */
    public ModelSettingRepository(IHarnessModelSettingDao harnessModelSettingDao) {
        this.harnessModelSettingDao = harnessModelSettingDao;
    }

    @Override
    @Transactional
    public void save(ModelSettingEntity setting) {
        harnessModelSettingDao.save(new HarnessModelSettingPO(
                setting.getProfileCode(),
                setting.getProviderCode(),
                setting.getModelCode(),
                setting.getBaseUrl(),
                setting.getApiKeyRef(),
                setting.getProtocol(),
                setting.isEnabled(),
                setting.isActive(),
                setting.getUpdatedAt()
        ));
    }
    /**
     * 查询All。
     * 流程：适配外部协议或资源 → 调用底层能力 → 返回结果。
     * @return 方法执行结果。
     */


    @Override
    public List<ModelSettingEntity> queryAll() {
        return harnessModelSettingDao.queryAll().stream().map(this::toEntity).toList();
    }
    /**
     * 查找By Profile Code。
     * 流程：适配外部协议或资源 → 调用底层能力 → 返回结果。
     * @param profileCode profile Code参数。
     * @return 方法执行结果。
     */


    @Override
    public Optional<ModelSettingEntity> findByProfileCode(String profileCode) {
        return harnessModelSettingDao.findByProfileCode(profileCode).map(this::toEntity);
    }

    /**
     * 按 profile + model 定位模型设置。
     */
    @Override
    public Optional<ModelSettingEntity> findByProfileAndModel(String profileCode, String modelCode) {
        return harnessModelSettingDao.findByProfileAndModel(profileCode, modelCode).map(this::toEntity);
    }

    /**
     * 激活指定模型；同事务内先清同 profile 激活标记再置目标行。
     */
    @Override
    @Transactional
    public boolean activate(String profileCode, String modelCode) {
        harnessModelSettingDao.clearActiveByProfile(profileCode);
        return harnessModelSettingDao.activate(profileCode, modelCode, java.time.Instant.now()) > 0;
    }

    /**
     * 删除某 profile 下的指定模型。
     */
    @Override
    @Transactional
    public void deleteByProfileAndModel(String profileCode, String modelCode) {
        harnessModelSettingDao.deleteByProfileAndModel(profileCode, modelCode);
    }
    /**
     * 删除By Profile Code。
     * 流程：适配外部协议或资源 → 调用底层能力 → 返回结果。
     * @param profileCode profile Code参数。
     */


    @Override
    @Transactional
    public void deleteByProfileCode(String profileCode) {
        harnessModelSettingDao.deleteByProfileCode(profileCode);
    }

    /**
     * 转换为 Entity。
     * 流程：适配外部协议或资源 → 调用底层能力 → 返回结果。
     * @param po PO参数。
     * @return 转换后的目标对象。
     */
    private ModelSettingEntity toEntity(HarnessModelSettingPO po) {
        return new ModelSettingEntity(
                po.profileCode(),
                po.providerCode(),
                po.modelCode(),
                po.baseUrl(),
                po.apiKeyRef(),
                po.protocol(),
                po.enabled(),
                po.active(),
                po.updatedAt()
        );
    }
}
