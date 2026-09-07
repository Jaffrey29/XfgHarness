package cn.xiaofuge.deepseek.harness.cases.runtime.node;

import cn.xiaofuge.deepseek.harness.cases.orchestration.AbstractStrategyRouter;
import cn.xiaofuge.deepseek.harness.cases.orchestration.NoContext;
import cn.xiaofuge.deepseek.harness.cases.orchestration.StrategyHandler;
import cn.xiaofuge.deepseek.harness.cases.support.CaseGuard;
import cn.xiaofuge.deepseek.harness.domain.runtime.setting.IModelSettingCommandService;
import org.springframework.stereotype.Service;

@Service
public class ModelSettingDeleteNode extends AbstractStrategyRouter<String, NoContext, Void> {

    private final IModelSettingCommandService modelSettingCommandService;

    public ModelSettingDeleteNode(IModelSettingCommandService modelSettingCommandService) {
        this.modelSettingCommandService = modelSettingCommandService;
    }

    @Override
    protected Void doApply(String profileCode, NoContext context) {
        CaseGuard.requireText(profileCode, "profileCode must not be blank");
        modelSettingCommandService.deleteByProfileCode(profileCode);
        return null;
    }

    @Override
    public StrategyHandler<String, NoContext, Void> getNext(String profileCode, NoContext context) {
        return defaultStrategyHandler;
    }
}
