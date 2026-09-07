package cn.xiaofuge.deepseek.harness.cases.plugin;

import cn.xiaofuge.deepseek.harness.api.dto.InstalledPluginResponseDTO;

import java.util.List;

public interface IInstalledPluginQueryCase {
    List<InstalledPluginResponseDTO> listInstalledPlugins();
}
