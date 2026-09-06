package cn.xiaofuge.deepseek.harness.api.service.query;

import cn.xiaofuge.deepseek.harness.api.response.plugin.InstalledPluginResponse;
import cn.xiaofuge.deepseek.harness.domain.plugin.registry.adapter.repository.IHarnessPluginRepository;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 已安装插件查询适配器。
 * <p>
 * 执行流程：
 * 1. 从插件仓储读取插件实体；
 * 2. 将实体转换为前端展示对象；
 * 3. 返回给控制器列表展示。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
@Service
public class HarnessInstalledPluginQueryApi {

    private final IHarnessPluginRepository harnessPluginRepository;

    public HarnessInstalledPluginQueryApi(IHarnessPluginRepository harnessPluginRepository) {
        this.harnessPluginRepository = harnessPluginRepository;
    }

    /**
     * 查询已安装插件列表。
     * <p>
     * 执行流程：
     * 1. 查询仓储中的全部插件；
     * 2. 映射为 `InstalledPluginResponse`；
     * 3. 返回给上层页面。
     */
    public List<InstalledPluginResponse> listInstalledPlugins() {
        return harnessPluginRepository.listAll().stream()
                .map(plugin -> new InstalledPluginResponse(
                        plugin.getPluginId(),
                        plugin.getDisplayName(),
                        plugin.getPluginVersion(),
                        plugin.getRuntimeType().name(),
                        plugin.getInstallMode().name(),
                        plugin.getStatus().name(),
                        plugin.getSourcePath(),
                        plugin.getEntrypoint(),
                        plugin.getInstalledAt()
                ))
                .toList();
    }
}
