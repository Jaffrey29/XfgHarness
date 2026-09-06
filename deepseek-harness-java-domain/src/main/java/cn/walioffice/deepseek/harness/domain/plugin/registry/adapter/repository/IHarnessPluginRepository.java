package cn.walioffice.deepseek.harness.domain.plugin.registry.adapter.repository;

import cn.walioffice.deepseek.harness.domain.plugin.registry.model.entity.HarnessPluginEntity;
import java.util.List;
import java.util.Optional;

/**
 * Defines durable access for installed plugins.
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public interface IHarnessPluginRepository {

    void save(HarnessPluginEntity plugin);

    Optional<HarnessPluginEntity> findByPluginId(String pluginId);

    List<HarnessPluginEntity> listAll();

    /**
     * Permanently removes one plugin registration row.
     *
     * @param pluginId plugin identifier
     */
    void deleteByPluginId(String pluginId);
}
