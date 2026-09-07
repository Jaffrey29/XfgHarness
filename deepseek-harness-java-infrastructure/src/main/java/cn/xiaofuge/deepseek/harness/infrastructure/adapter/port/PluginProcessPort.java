package cn.xiaofuge.deepseek.harness.infrastructure.adapter.port;

import cn.xiaofuge.deepseek.harness.domain.plugin.bridge.model.valobj.PluginBridgePlanVO;
import cn.xiaofuge.deepseek.harness.domain.plugin.registry.model.entity.HarnessPluginEntity;
import cn.xiaofuge.deepseek.harness.domain.plugin.registry.model.valobj.PluginRuntimeTypeEnumVO;
import cn.xiaofuge.deepseek.harness.domain.plugin.runtime.adapter.port.IPluginProcessPort;
import cn.xiaofuge.deepseek.harness.domain.plugin.runtime.model.valobj.PluginProcessStatusVO;
import cn.xiaofuge.deepseek.harness.domain.plugin.runtime.model.valobj.PluginRunResultVO;
import cn.xiaofuge.deepseek.harness.domain.tool.plugin.PluginToolBridgeService;
import cn.xiaofuge.deepseek.harness.infrastructure.adapter.plugin.java.JavaPluginRuntimeManager;
import cn.xiaofuge.deepseek.harness.infrastructure.gateway.PluginSidecarGatewayService;
import java.nio.file.Path;
import java.time.Instant;
import org.springframework.stereotype.Component;

/**
 * Adapts plugin process lifecycle calls to either the Java in-process runtime
 * (for JAVA_NATIVE plugins) or the Node sidecar gateway (for DSH_NODE_BRIDGE).
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
@Component
public class PluginProcessPort implements IPluginProcessPort {

    private final PluginSidecarGatewayService pluginSidecarGatewayService;
    private final JavaPluginRuntimeManager javaPluginRuntimeManager;
    private final PluginToolBridgeService pluginToolBridgeService;

    /**
     * 处理Plugin Process Port相关逻辑。
     */
    public PluginProcessPort(
            PluginSidecarGatewayService pluginSidecarGatewayService,
            JavaPluginRuntimeManager javaPluginRuntimeManager,
            PluginToolBridgeService pluginToolBridgeService
    ) {
        this.pluginSidecarGatewayService = pluginSidecarGatewayService;
        this.javaPluginRuntimeManager = javaPluginRuntimeManager;
        this.pluginToolBridgeService = pluginToolBridgeService;
    }

    @Override
    public PluginRunResultVO start(HarnessPluginEntity plugin, PluginBridgePlanVO plan) {
        if (plugin.getRuntimeType() == PluginRuntimeTypeEnumVO.JAVA_NATIVE) {
            return startJavaPlugin(plugin, plan);
        }
        PluginRunResultVO result = pluginSidecarGatewayService.start(
                plugin.getPluginId(), plan.workingDirectory(), plan.entrypointPath());
        try {
            pluginSidecarGatewayService.runningProcess(plugin.getPluginId())
                    .ifPresent(process -> pluginToolBridgeService.bridgePlugin(plugin.getPluginId(), process));
        } catch (RuntimeException exception) {
            pluginSidecarGatewayService.stop(plugin.getPluginId());
            throw exception;
        }
        return result;
    }

    @Override
    public PluginProcessStatusVO status(String pluginId) {
        JavaPluginRuntimeManager.JavaPluginStatusResult javaStatus = javaPluginRuntimeManager.status(pluginId);
        if (javaStatus.running()) {
            return new PluginProcessStatusVO(pluginId, 0L, true, "java-in-process",
                    null, Instant.now(), javaStatus.message());
        }
        PluginProcessStatusVO nodeStatus = pluginSidecarGatewayService.status(pluginId);
        if (nodeStatus.running()) return nodeStatus;
        return new PluginProcessStatusVO(pluginId, null, false, null, null, null, javaStatus.message());
    }

    @Override
    public PluginProcessStatusVO stop(String pluginId) {
        pluginToolBridgeService.unbridgePlugin(pluginId);
        JavaPluginRuntimeManager.JavaPluginStatusResult javaResult = javaPluginRuntimeManager.stop(pluginId);
        if (!javaResult.running()) {
            return new PluginProcessStatusVO(pluginId, null, false, "java-in-process",
                    null, Instant.now(), javaResult.message());
        }
        return pluginSidecarGatewayService.stop(pluginId);
    }

    /**
     * 处理start Java Plugin相关逻辑。
     */
    private PluginRunResultVO startJavaPlugin(HarnessPluginEntity plugin, PluginBridgePlanVO plan) {
        Path jarPath = Path.of(plan.entrypointPath());
        JavaPluginRuntimeManager.JavaPluginStartResult result =
                javaPluginRuntimeManager.start(plugin.getPluginId(), jarPath);
        return new PluginRunResultVO(
                result.pluginId(),
                0L,
                "java-in-process",
                null,
                result.running(),
                result.startedAt(),
                result.message()
        );
    }
}
