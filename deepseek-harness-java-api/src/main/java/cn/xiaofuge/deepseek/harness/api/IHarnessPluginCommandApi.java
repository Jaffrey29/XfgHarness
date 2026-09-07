package cn.xiaofuge.deepseek.harness.api;

import cn.xiaofuge.deepseek.harness.api.dto.ActivatePluginRequestDTO;
import cn.xiaofuge.deepseek.harness.api.dto.ActivatePluginResponseDTO;
import cn.xiaofuge.deepseek.harness.api.dto.InstallPluginRequestDTO;
import cn.xiaofuge.deepseek.harness.api.dto.InstallPluginResponseDTO;
import cn.xiaofuge.deepseek.harness.api.dto.PluginLifecycleResponseDTO;
import cn.xiaofuge.deepseek.harness.api.dto.RunPluginRequestDTO;
import cn.xiaofuge.deepseek.harness.api.dto.RunPluginResponseDTO;
import cn.xiaofuge.deepseek.harness.api.response.Response;

public interface IHarnessPluginCommandApi {

    Response<InstallPluginResponseDTO> installPlugin(InstallPluginRequestDTO request);

    Response<ActivatePluginResponseDTO> activatePlugin(ActivatePluginRequestDTO request);

    Response<RunPluginResponseDTO> runPlugin(RunPluginRequestDTO request);

    Response<PluginLifecycleResponseDTO> disablePlugin(String pluginId);

    Response<PluginLifecycleResponseDTO> enablePlugin(String pluginId);

    Response<PluginLifecycleResponseDTO> uninstallPlugin(String pluginId);
}
