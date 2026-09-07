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
 * @author 小傅哥
 * @website bugstack.cn
 */
@Repository
public class ModelSettingRepository implements IModelSettingRepository {

    private final IHarnessModelSettingDao harnessModelSettingDao;

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
                setting.isEnabled(),
                setting.getUpdatedAt()
        ));
    }

    @Override
    public List<ModelSettingEntity> queryAll() {
        return harnessModelSettingDao.queryAll().stream().map(this::toEntity).toList();
    }

    @Override
    public Optional<ModelSettingEntity> findByProfileCode(String profileCode) {
        return harnessModelSettingDao.findByProfileCode(profileCode).map(this::toEntity);
    }

    @Override
    @Transactional
    public void deleteByProfileCode(String profileCode) {
        harnessModelSettingDao.deleteByProfileCode(profileCode);
    }

    private ModelSettingEntity toEntity(HarnessModelSettingPO po) {
        return new ModelSettingEntity(
                po.profileCode(),
                po.providerCode(),
                po.modelCode(),
                po.baseUrl(),
                po.apiKeyRef(),
                po.enabled(),
                po.updatedAt()
        );
    }
}
