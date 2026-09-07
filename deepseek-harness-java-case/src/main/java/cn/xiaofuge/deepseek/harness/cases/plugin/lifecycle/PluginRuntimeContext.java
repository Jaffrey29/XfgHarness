package cn.xiaofuge.deepseek.harness.cases.plugin.lifecycle;

import cn.xiaofuge.deepseek.harness.domain.plugin.bridge.model.valobj.PluginBridgePlanVO;
import cn.xiaofuge.deepseek.harness.domain.plugin.registry.model.entity.HarnessPluginEntity;
import cn.xiaofuge.deepseek.harness.domain.plugin.registry.model.valobj.PluginInstallStatusEnumVO;
import java.time.Instant;

public class PluginRuntimeContext {

    private HarnessPluginEntity plugin;
    private PluginBridgePlanVO bridgePlan;
    private PluginInstallStatusEnumVO status;
    private Instant checkedAt;

    public HarnessPluginEntity getPlugin() {
        return plugin;
    }

    public void setPlugin(HarnessPluginEntity plugin) {
        this.plugin = plugin;
    }

    public PluginBridgePlanVO getBridgePlan() {
        return bridgePlan;
    }

    public void setBridgePlan(PluginBridgePlanVO bridgePlan) {
        this.bridgePlan = bridgePlan;
    }

    public PluginInstallStatusEnumVO getStatus() {
        return status;
    }

    public void setStatus(PluginInstallStatusEnumVO status) {
        this.status = status;
    }

    public Instant getCheckedAt() {
        return checkedAt;
    }

    public void setCheckedAt(Instant checkedAt) {
        this.checkedAt = checkedAt;
    }
}
