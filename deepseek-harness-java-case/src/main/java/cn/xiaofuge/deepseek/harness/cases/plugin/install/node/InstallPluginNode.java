package cn.xiaofuge.deepseek.harness.cases.plugin.install.node;

import cn.xiaofuge.deepseek.harness.cases.orchestration.AbstractStrategyRouter;
import cn.xiaofuge.deepseek.harness.cases.orchestration.NoContext;
import cn.xiaofuge.deepseek.harness.cases.orchestration.StrategyHandler;
import cn.xiaofuge.deepseek.harness.api.dto.InstallPluginRequestDTO;
import cn.xiaofuge.deepseek.harness.api.dto.InstallPluginResponseDTO;
import cn.xiaofuge.deepseek.harness.cases.support.CaseGuard;
import cn.xiaofuge.deepseek.harness.domain.plugin.IPluginRegistryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 插件安装节点。
 * <p>流程：校验安装命令 → 调用插件注册领域服务安装并登记 → 将领域结果映射为 API DTO。</p>
 * <p>数据示例：传入 {@code pluginId=weather-forecast, runtimeType=PYTHON, entrypoint=main.py}；
 * 返回 {@code status=INSTALLED, installMode=LOCAL, installedAt=2026-09-07T10:00:00Z}。</p>
 */
@Service
public class InstallPluginNode
        extends AbstractStrategyRouter<InstallPluginRequestDTO, NoContext, InstallPluginResponseDTO> {

    private static final Logger log = LoggerFactory.getLogger(InstallPluginNode.class);

    private final IPluginRegistryService pluginRegistryService;

    public InstallPluginNode(IPluginRegistryService pluginRegistryService) {
        this.pluginRegistryService = pluginRegistryService;
    }

    /**
     * 执行当前节点核心逻辑：校验输入、调用领域服务并返回输出。
     */
    @Override
    protected InstallPluginResponseDTO doApply(InstallPluginRequestDTO request, NoContext context) {
        CaseGuard.requireNonNull(request, "install command must not be null");
        CaseGuard.requireText(request.pluginId(), "pluginId must not be blank");
        var result = pluginRegistryService.installPlugin(
                request.pluginId(),
                request.displayName(),
                request.pluginVersion(),
                request.runtimeType(),
                request.sourcePath(),
                request.entrypoint()
        );
        log.info("[Plugin] 插件安装完成，pluginId={} status={} mode={}",
                result.pluginId(), result.status(), result.installMode());
        return new InstallPluginResponseDTO(
                result.pluginId(),
                result.runtimeType().name(),
                result.installMode().name(),
                result.status().name(),
                result.directlyExecutable(),
                result.installedAt()
        );
    }

    /**
     * 返回下一执行节点；单节点链路返回默认终止处理。
     */
    @Override
    public StrategyHandler<InstallPluginRequestDTO, NoContext, InstallPluginResponseDTO> getNext(
            InstallPluginRequestDTO request, NoContext context) {
        return defaultStrategyHandler;
    }
}
