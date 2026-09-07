package cn.xiaofuge.deepseek.harness.cases.goal;

import cn.xiaofuge.deepseek.harness.api.dto.GoalViewResponseDTO;
import cn.xiaofuge.deepseek.harness.api.dto.GoalCommandRequestDTO;
import cn.xiaofuge.deepseek.harness.cases.goal.factory.GoalPipelineFactory;
import cn.xiaofuge.deepseek.harness.cases.goal.node.GoalCommandNode;
import org.springframework.stereotype.Service;

@Service
public class GoalCommandCaseImpl implements IGoalCommandCase {
    private final GoalPipelineFactory goalPipelineFactory;

    public GoalCommandCaseImpl(GoalPipelineFactory goalPipelineFactory) {
        this.goalPipelineFactory = goalPipelineFactory;
    }

    @Override
    public GoalViewResponseDTO apply(String sessionId, GoalCommandRequestDTO request) {
        return goalPipelineFactory.commandPipeline()
                .execute(new GoalCommandNode.Input(sessionId, request));
    }
}
