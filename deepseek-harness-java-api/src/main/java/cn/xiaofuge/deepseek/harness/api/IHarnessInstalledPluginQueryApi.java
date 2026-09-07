package cn.xiaofuge.deepseek.harness.api;

import cn.xiaofuge.deepseek.harness.api.dto.InstalledPluginResponseDTO;
import cn.xiaofuge.deepseek.harness.api.response.Response;
import java.util.List;

public interface IHarnessInstalledPluginQueryApi {

    Response<List<InstalledPluginResponseDTO>> listInstalledPlugins();
}
