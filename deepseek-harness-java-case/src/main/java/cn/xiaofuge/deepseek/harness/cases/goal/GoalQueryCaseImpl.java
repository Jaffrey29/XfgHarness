package cn.xiaofuge.deepseek.harness.cases.goal;

import cn.xiaofuge.deepseek.harness.api.dto.GoalViewResponseDTO;
import cn.xiaofuge.deepseek.harness.cases.goal.factory.GoalPipelineFactory;
import org.springframework.stereotype.Service;

@Service
public class GoalQueryCaseImpl implements IGoalQueryCase {
    private final GoalPipelineFactory goalPipelineFactory;

    public GoalQueryCaseImpl(GoalPipelineFactory goalPipelineFactory) {
        this.goalPipelineFactory = goalPipelineFactory;
    }

    @Override
    public GoalViewResponseDTO getGoal(String sessionId) {
        return goalPipelineFactory.queryPipeline().execute(sessionId);
    }
}
