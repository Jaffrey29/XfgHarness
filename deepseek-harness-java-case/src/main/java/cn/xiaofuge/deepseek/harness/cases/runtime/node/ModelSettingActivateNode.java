package cn.xiaofuge.deepseek.harness.cases.runtime.node;

import cn.xiaofuge.deepseek.harness.cases.orchestration.AbstractStrategyRouter;
import cn.xiaofuge.deepseek.harness.cases.orchestration.NoContext;
import cn.xiaofuge.deepseek.harness.cases.orchestration.StrategyHandler;
import cn.xiaofuge.deepseek.harness.api.dto.ActivateModelSettingRequestDTO;
import cn.xiaofuge.deepseek.harness.api.dto.ModelSettingResponseDTO;
import cn.xiaofuge.deepseek.harness.cases.support.CaseGuard;
import cn.xiaofuge.deepseek.harness.domain.runtime.setting.IModelSettingCommandService;
import cn.xiaofuge.deepseek.harness.domain.runtime.setting.model.valobj.ModelSettingVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 模型设置激活节点。
 * <p>流程：校验 profileCode + modelCode → 调用领域命令服务将该模型置为当前生效 → 映射为配置 DTO。</p>
 * <p>数据示例：传入 {@code profileCode=deepseek, modelCode=glm-5.3-flash}；
 * 返回该配置的 DTO 且 active=true，同 profile 其他配置自动取消激活。</p>
 */
@Service
public class ModelSettingActivateNode
        extends AbstractStrategyRouter<ActivateModelSettingRequestDTO, NoContext, ModelSettingResponseDTO> {

    private static final Logger log = LoggerFactory.getLogger(ModelSettingActivateNode.class);

    private final IModelSettingCommandService modelSettingCommandService;

    public ModelSettingActivateNode(IModelSettingCommandService modelSettingCommandService) {
        this.modelSettingCommandService = modelSettingCommandService;
    }

    /**
     * 执行当前节点核心逻辑：校验输入、调用领域服务并返回输出。
     */
    @Override
    protected ModelSettingResponseDTO doApply(ActivateModelSettingRequestDTO command, NoContext context) {
        CaseGuard.requireNonNull(command, "activate model setting command must not be null");
        CaseGuard.requireText(command.profileCode(), "profileCode must not be blank");
        CaseGuard.requireText(command.modelCode(), "modelCode must not be blank");
        ModelSettingVO setting = modelSettingCommandService.activate(command.profileCode(), command.modelCode());
        log.info("[Runtime] 模型配置激活完成，profileCode={} modelCode={}", setting.profileCode(), setting.modelCode());
        return new ModelSettingResponseDTO(
                setting.profileCode(), setting.providerCode(), setting.modelCode(),
                setting.baseUrl(), setting.apiKeyRef(), setting.protocol(),
                setting.enabled(), setting.active(), setting.updatedAt()
        );
    }

    /**
     * 返回下一执行节点；单节点链路返回默认终止处理。
     */
    @Override
    public StrategyHandler<ActivateModelSettingRequestDTO, NoContext, ModelSettingResponseDTO> getNext(
            ActivateModelSettingRequestDTO command, NoContext context) {
        return defaultStrategyHandler;
    }
}
