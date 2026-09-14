package cn.xiaofuge.deepseek.harness.domain.plugin.registry.adapter.port;

import cn.xiaofuge.deepseek.harness.domain.plugin.registry.model.entity.HarnessPluginEntity;
import cn.xiaofuge.deepseek.harness.domain.plugin.registry.model.valobj.PluginInventoryMetadataVO;
import java.util.Optional;

public interface IPluginInventoryMetadataPort {

    Optional<PluginInventoryMetadataVO> read(HarnessPluginEntity plugin);
}
