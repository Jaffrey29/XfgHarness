package cn.xiaofuge.deepseek.harness.api;

import cn.xiaofuge.deepseek.harness.api.dto.InstalledPluginResponseDTO;
import cn.xiaofuge.deepseek.harness.api.dto.QueryPluginStatusResponseDTO;
import cn.xiaofuge.deepseek.harness.api.response.Response;
import java.util.List;

public interface IHarnessPluginQueryApi {

    Response<List<InstalledPluginResponseDTO>> listInstalledPlugins();

    Response<QueryPluginStatusResponseDTO> queryPluginStatus(String pluginId);
}
