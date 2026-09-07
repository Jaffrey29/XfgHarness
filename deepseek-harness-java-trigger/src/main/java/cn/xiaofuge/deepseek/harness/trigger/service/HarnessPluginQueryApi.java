package cn.xiaofuge.deepseek.harness.trigger.service;

import cn.xiaofuge.deepseek.harness.api.dto.QueryPluginStatusResponseDTO;
import cn.xiaofuge.deepseek.harness.api.dto.InstalledPluginResponseDTO;
import cn.xiaofuge.deepseek.harness.api.IHarnessPluginQueryApi;
import cn.xiaofuge.deepseek.harness.api.response.Response;
import cn.xiaofuge.deepseek.harness.trigger.service.query.HarnessInstalledPluginQueryApi;
import cn.xiaofuge.deepseek.harness.cases.plugin.IQueryHarnessPluginStatusCase;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 插件查询适配器。
 * <p>
 * 执行流程：
 * 1. 接收 Trigger 层的插件查询请求；
 * 2. 调用领域查询服务或查询用例组装读模型；
 * 3. 将 case 层 Result 映射为 api Response 返回前端可直接渲染的响应对象。
 *
 * <p>调用流程：HTTP/Web 层 → API 适配器 → 应用门面。</p>
 * <p>示例：控制器只负责协议转换，不写业务规则。</p>
 * @author 小傅哥
 * @website bugstack.cn
 */
@Service
public class HarnessPluginQueryApi implements IHarnessPluginQueryApi {

    private final HarnessInstalledPluginQueryApi harnessInstalledPluginQueryApi;
    private final IQueryHarnessPluginStatusCase queryHarnessPluginStatusCase;

    /**
     * 初始化插件查询适配器，并注入已安装插件与运行状态查询能力。
     * <p>流程：HTTP 请求 → API 适配器 → 查询用例。</p>
     */
    public HarnessPluginQueryApi(HarnessInstalledPluginQueryApi harnessInstalledPluginQueryApi,
                                 IQueryHarnessPluginStatusCase queryHarnessPluginStatusCase) {
        this.harnessInstalledPluginQueryApi = harnessInstalledPluginQueryApi;
        this.queryHarnessPluginStatusCase = queryHarnessPluginStatusCase;
    }

    /**
     * 列出Installed Plugins。
     */
    public Response<List<InstalledPluginResponseDTO>> listInstalledPlugins() {
        return harnessInstalledPluginQueryApi.listInstalledPlugins();
    }

    /**
     * 查询Plugin Status。
     */
    public Response<QueryPluginStatusResponseDTO> queryPluginStatus(String pluginId) {
        return execute(() -> queryHarnessPluginStatusCase.execute(pluginId));
    }

    private <T> Response<T> execute(java.util.function.Supplier<T> action) {
        try {
            return Response.success(action.get());
        } catch (IllegalArgumentException exception) {
            return Response.invalidArgument(exception.getMessage());
        } catch (RuntimeException exception) {
            return Response.internalError(exception.getMessage());
        }
    }
}
