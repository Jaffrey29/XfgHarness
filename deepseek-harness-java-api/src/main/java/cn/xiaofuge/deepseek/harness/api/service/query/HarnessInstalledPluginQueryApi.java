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
 * <p>调用流程：Controller/DTO → 应用门面 → 用例；响应对象由用例结果转换。</p>
 * <p>示例：DTO 只做协议隔离，不承载领域规则。</p>
 * @author 小傅哥
 * @website bugstack.cn
 */
@Service
public class HarnessInstalledPluginQueryApi {

    private final IHarnessPluginRepository harnessPluginRepository;

    /**
     * 处理Harness Installed 插件 Query API。
     * 流程：转换请求 → 调用用例或领域服务 → 转换响应。
     * @param harnessPluginRepository harness 插件 Repository参数。
     * @return 方法执行结果。
     */
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
