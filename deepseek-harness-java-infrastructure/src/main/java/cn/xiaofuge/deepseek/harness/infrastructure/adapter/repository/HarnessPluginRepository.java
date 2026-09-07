package cn.xiaofuge.deepseek.harness.infrastructure.adapter.repository;

import cn.xiaofuge.deepseek.harness.domain.plugin.registry.adapter.repository.IHarnessPluginRepository;
import cn.xiaofuge.deepseek.harness.domain.plugin.registry.model.entity.HarnessPluginEntity;
import cn.xiaofuge.deepseek.harness.domain.plugin.registry.model.valobj.PluginInstallModeEnumVO;
import cn.xiaofuge.deepseek.harness.domain.plugin.registry.model.valobj.PluginInstallStatusEnumVO;
import cn.xiaofuge.deepseek.harness.domain.plugin.registry.model.valobj.PluginRuntimeTypeEnumVO;
import cn.xiaofuge.deepseek.harness.infrastructure.dao.IHarnessPluginDao;
import cn.xiaofuge.deepseek.harness.infrastructure.dao.po.HarnessPluginPO;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * 通过 DAO 层持久化插件安装信息。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
@Repository
public class HarnessPluginRepository implements IHarnessPluginRepository {

    private final IHarnessPluginDao harnessPluginDao;

    /**
     * 处理Harness Plugin Repository相关逻辑。
     */
    public HarnessPluginRepository(IHarnessPluginDao harnessPluginDao) {
        this.harnessPluginDao = harnessPluginDao;
    }

    @Override
    @Transactional
    public void save(HarnessPluginEntity plugin) {
        harnessPluginDao.save(new HarnessPluginPO(
                plugin.getPluginId(),
                plugin.getDisplayName(),
                plugin.getPluginVersion(),
                plugin.getRuntimeType().name(),
                plugin.getInstallMode().name(),
                plugin.getSourcePath(),
                plugin.getEntrypoint(),
                plugin.getStatus().name(),
                plugin.getInstalledAt()
        ));
    }

    @Override
    public Optional<HarnessPluginEntity> findByPluginId(String pluginId) {
        return harnessPluginDao.findByPluginId(pluginId).map(this::toEntity);
    }

    @Override
    public List<HarnessPluginEntity> listAll() {
        return harnessPluginDao.listAll().stream().map(this::toEntity).toList();
    }

    @Override
    @Transactional
    public void deleteByPluginId(String pluginId) {
        harnessPluginDao.deleteByPluginId(pluginId);
    }

    /**
     * 处理to Entity相关逻辑。
     */
    private HarnessPluginEntity toEntity(HarnessPluginPO pluginPO) {
        return new HarnessPluginEntity(
                pluginPO.pluginId(),
                pluginPO.displayName(),
                pluginPO.pluginVersion(),
                PluginRuntimeTypeEnumVO.valueOf(pluginPO.runtimeType()),
                PluginInstallModeEnumVO.valueOf(pluginPO.installMode()),
                pluginPO.sourcePath(),
                pluginPO.entrypoint(),
                PluginInstallStatusEnumVO.valueOf(pluginPO.status()),
                pluginPO.installedAt()
        );
    }
}
