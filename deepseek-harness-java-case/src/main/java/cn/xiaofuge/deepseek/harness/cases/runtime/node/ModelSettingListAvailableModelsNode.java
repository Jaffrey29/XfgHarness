package cn.xiaofuge.deepseek.harness.cases.runtime.node;

import cn.xiaofuge.deepseek.harness.cases.orchestration.AbstractStrategyRouter;
import cn.xiaofuge.deepseek.harness.cases.orchestration.NoContext;
import cn.xiaofuge.deepseek.harness.cases.orchestration.StrategyHandler;
import cn.xiaofuge.deepseek.harness.api.dto.AvailableModelResponseDTO;
import cn.xiaofuge.deepseek.harness.domain.runtime.setting.IModelSettingQueryService;
import cn.xiaofuge.deepseek.harness.domain.runtime.setting.model.valobj.ModelSettingVO;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 可用模型查询节点。
 * <p>流程：读取全部配置 → 过滤启用且模型编码非空的记录 → 按 provider:model 去重 → 返回可用模型列表。</p>
 * <p>数据示例：两个 profile 均配置 {@code openai-compatible:glm-5.3-flash} 时只返回一条
 * {@code modelCode=glm-5.3-flash}。</p>
 */
@Service
public class ModelSettingListAvailableModelsNode
        extends AbstractStrategyRouter<Void, NoContext, List<AvailableModelResponseDTO>> {

    private static final Logger log = LoggerFactory.getLogger(ModelSettingListAvailableModelsNode.class);

    private final IModelSettingQueryService modelSettingQueryService;

    public ModelSettingListAvailableModelsNode(IModelSettingQueryService modelSettingQueryService) {
        this.modelSettingQueryService = modelSettingQueryService;
    }

    /**
     * 执行当前节点核心逻辑：校验输入、调用领域服务并返回输出。
     */
    @Override
    protected List<AvailableModelResponseDTO> doApply(Void request, NoContext context) {
        Map<String, AvailableModelResponseDTO> models = new LinkedHashMap<>();
        modelSettingQueryService.queryAll().stream()
                .filter(ModelSettingVO::enabled)
                .filter(setting -> setting.modelCode() != null && !setting.modelCode().isBlank())
                .forEach(setting -> models.putIfAbsent(
                        setting.providerCode() + ":" + setting.modelCode(),
                        new AvailableModelResponseDTO(setting.providerCode(), setting.modelCode(), setting.modelCode())
                ));
        var result = List.copyOf(models.values());
        log.debug("[Runtime] 可用模型查询完成，count={}", result.size());
        return result;
    }

    /**
     * 返回下一执行节点；单节点链路返回默认终止处理。
     */
    @Override
    public StrategyHandler<Void, NoContext, List<AvailableModelResponseDTO>> getNext(Void request, NoContext context) {
        return defaultStrategyHandler;
    }
}
