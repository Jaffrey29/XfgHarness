package cn.xiaofuge.deepseek.harness.cases.runtime.node;

import cn.xiaofuge.deepseek.harness.cases.orchestration.AbstractStrategyRouter;
import cn.xiaofuge.deepseek.harness.cases.orchestration.NoContext;
import cn.xiaofuge.deepseek.harness.cases.orchestration.StrategyHandler;
import cn.xiaofuge.deepseek.harness.api.dto.ModelSettingResponseDTO;
import cn.xiaofuge.deepseek.harness.api.dto.SaveModelSettingRequestDTO;
import cn.xiaofuge.deepseek.harness.cases.support.CaseGuard;
import cn.xiaofuge.deepseek.harness.domain.runtime.setting.IModelSettingCommandService;
import cn.xiaofuge.deepseek.harness.domain.runtime.setting.model.valobj.ModelSettingVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 模型设置保存节点。
 * <p>流程：校验命令和 profileCode → 调用领域命令服务保存为 ModelSettingVO → 映射为配置 DTO。</p>
 * <p>数据示例：传入 {@code profileCode=default, modelCode=glm-5.3-flash, enabled=true}；
 * 返回包含同一 profileCode 和 updatedAt 的 DTO。</p>
 */
@Service
public class ModelSettingSaveNode
        extends AbstractStrategyRouter<SaveModelSettingRequestDTO, NoContext, ModelSettingResponseDTO> {

    private static final Logger log = LoggerFactory.getLogger(ModelSettingSaveNode.class);

    private final IModelSettingCommandService modelSettingCommandService;

    public ModelSettingSaveNode(IModelSettingCommandService modelSettingCommandService) {
        this.modelSettingCommandService = modelSettingCommandService;
    }

    /**
     * 执行当前节点核心逻辑：校验输入、调用领域服务并返回输出。
     */
    @Override
    protected ModelSettingResponseDTO doApply(SaveModelSettingRequestDTO command, NoContext context) {
        CaseGuard.requireNonNull(command, "model setting command must not be null");
        CaseGuard.requireText(command.profileCode(), "profileCode must not be blank");
        ModelSettingVO setting = modelSettingCommandService.save(
                command.profileCode(), command.providerCode(), command.modelCode(),
                command.baseUrl(), command.apiKeyRef(), command.protocol(), command.enabled()
        );
        log.info("[Runtime] 模型配置保存完成，profileCode={} modelCode={} protocol={} enabled={}",
                setting.profileCode(), setting.modelCode(), setting.protocol(), setting.enabled());
        return toResponse(setting);
    }

    /**
     * 返回下一执行节点；单节点链路返回默认终止处理。
     */
    @Override
    public StrategyHandler<SaveModelSettingRequestDTO, NoContext, ModelSettingResponseDTO> getNext(
            SaveModelSettingRequestDTO command, NoContext context) {
        return defaultStrategyHandler;
    }

    /**
     * 将领域层结果对象转换为用例层 DTO。
     */
    private ModelSettingResponseDTO toResponse(ModelSettingVO setting) {
        return new ModelSettingResponseDTO(
                setting.profileCode(), setting.providerCode(), setting.modelCode(),
                setting.baseUrl(), setting.apiKeyRef(), setting.protocol(),
                setting.enabled(), setting.active(), setting.updatedAt()
        );
    }
}
