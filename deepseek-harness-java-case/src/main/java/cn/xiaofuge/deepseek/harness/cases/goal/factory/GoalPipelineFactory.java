package cn.xiaofuge.deepseek.harness.cases.goal.factory;

import cn.xiaofuge.deepseek.harness.domain.goal.model.valobj.GoalCommandVO;
import cn.xiaofuge.deepseek.harness.api.dto.GoalViewResponseDTO;
import cn.xiaofuge.deepseek.harness.cases.goal.node.GoalCommandNode;
import cn.xiaofuge.deepseek.harness.cases.goal.node.GoalQueryNode;
import cn.xiaofuge.deepseek.harness.cases.orchestration.CasePipeline;
import cn.xiaofuge.deepseek.harness.cases.orchestration.CasePipelineFactory;
import cn.xiaofuge.deepseek.harness.cases.orchestration.NoContext;
import org.springframework.stereotype.Service;

@Service
public class GoalPipelineFactory implements CasePipelineFactory<String, NoContext, GoalViewResponseDTO> {

    private final GoalQueryNode goalQueryNode;
    private final GoalCommandNode goalCommandNode;

    public GoalPipelineFactory(GoalQueryNode goalQueryNode, GoalCommandNode goalCommandNode) {
        this.goalQueryNode = goalQueryNode;
        this.goalCommandNode = goalCommandNode;
    }

    public CasePipeline<String, NoContext, GoalViewResponseDTO> queryPipeline() {
        return CasePipeline.of("Goal query", goalQueryNode, () -> NoContext.INSTANCE);
    }

    public CasePipeline<GoalCommandNode.Input, NoContext, GoalViewResponseDTO> commandPipeline() {
        return CasePipeline.of("Goal command", goalCommandNode, () -> NoContext.INSTANCE);
    }

    public CasePipeline<String, NoContext, GoalViewResponseDTO> pipeline() {
        return queryPipeline();
    }
}
