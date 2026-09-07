package cn.xiaofuge.deepseek.harness.infrastructure.adapter.repository;

import cn.xiaofuge.deepseek.harness.domain.plugin.bridge.adapter.repository.IHarnessPluginRuntimeBindingRepository;
import cn.xiaofuge.deepseek.harness.domain.plugin.bridge.model.valobj.PluginBridgePlanVO;
import cn.xiaofuge.deepseek.harness.infrastructure.dao.IHarnessPluginRuntimeBindingDao;
import cn.xiaofuge.deepseek.harness.infrastructure.dao.po.HarnessPluginRuntimeBindingPO;
import java.time.Instant;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * 持久化某插件最近一次桥接解析结果。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
@Repository
public class HarnessPluginRuntimeBindingRepository implements IHarnessPluginRuntimeBindingRepository {

    private final IHarnessPluginRuntimeBindingDao harnessPluginRuntimeBindingDao;

    /**
     * 处理Harness Plugin Runtime Binding Repository相关逻辑。
     */
    public HarnessPluginRuntimeBindingRepository(IHarnessPluginRuntimeBindingDao harnessPluginRuntimeBindingDao) {
        this.harnessPluginRuntimeBindingDao = harnessPluginRuntimeBindingDao;
    }

    @Override
    @Transactional
    public void save(String pluginId, PluginBridgePlanVO plan, Instant checkedAt) {
        harnessPluginRuntimeBindingDao.save(new HarnessPluginRuntimeBindingPO(
                pluginId,
                plan.packageType().name(),
                plan.manifestPath(),
                plan.workingDirectory(),
                plan.runtimeCommand(),
                plan.bridgeReady(),
                plan.message(),
                checkedAt
        ));
    }
}
