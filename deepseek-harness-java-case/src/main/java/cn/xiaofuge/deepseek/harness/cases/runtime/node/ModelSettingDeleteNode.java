package cn.xiaofuge.deepseek.harness.cases.runtime.node;

import cn.xiaofuge.deepseek.harness.cases.orchestration.AbstractStrategyRouter;
import cn.xiaofuge.deepseek.harness.cases.orchestration.NoContext;
import cn.xiaofuge.deepseek.harness.cases.orchestration.StrategyHandler;
import cn.xiaofuge.deepseek.harness.cases.support.CaseGuard;
import cn.xiaofuge.deepseek.harness.domain.runtime.setting.IModelSettingCommandService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 模型设置删除节点。
 * <p>流程：校验 profileCode 非空 → 调用领域命令服务按 profile 删除 → Pipeline 终止。</p>
 * <p>数据示例：传入 {@code profileCode=default}，删除该 profile 对应的模型配置且无返回值。</p>
 */
@Service
public class ModelSettingDeleteNode extends AbstractStrategyRouter<String, NoContext, Void> {

    private static final Logger log = LoggerFactory.getLogger(ModelSettingDeleteNode.class);

    private final IModelSettingCommandService modelSettingCommandService;

    public ModelSettingDeleteNode(IModelSettingCommandService modelSettingCommandService) {
        this.modelSettingCommandService = modelSettingCommandService;
    }

    /**
     * 执行当前节点核心逻辑：校验输入、调用领域服务并返回输出。
     */
    @Override
    protected Void doApply(String profileCode, NoContext context) {
        CaseGuard.requireText(profileCode, "profileCode must not be blank");
        modelSettingCommandService.deleteByProfileCode(profileCode);
        log.info("[Runtime] 模型配置删除完成，profileCode={}", profileCode);
        return null;
    }

    /**
     * 返回下一执行节点；单节点链路返回默认终止处理。
     */
    @Override
    public StrategyHandler<String, NoContext, Void> getNext(String profileCode, NoContext context) {
        return defaultStrategyHandler;
    }
}
