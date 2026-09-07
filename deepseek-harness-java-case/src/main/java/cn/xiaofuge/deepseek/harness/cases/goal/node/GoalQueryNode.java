package cn.xiaofuge.deepseek.harness.cases.goal.node;

import cn.xiaofuge.deepseek.harness.api.dto.GoalViewResponseDTO;
import cn.xiaofuge.deepseek.harness.cases.orchestration.AbstractStrategyRouter;
import cn.xiaofuge.deepseek.harness.cases.orchestration.NoContext;
import cn.xiaofuge.deepseek.harness.cases.orchestration.StrategyHandler;
import cn.xiaofuge.deepseek.harness.cases.support.CaseGuard;
import cn.xiaofuge.deepseek.harness.domain.goal.IGoalQueryService;
import cn.xiaofuge.deepseek.harness.domain.goal.model.valobj.GoalSnapshotVO;
import org.springframework.stereotype.Service;

@Service
public class GoalQueryNode extends AbstractStrategyRouter<String, NoContext, GoalViewResponseDTO> {

    private final IGoalQueryService goalQueryService;

    public GoalQueryNode(IGoalQueryService goalQueryService) {
        this.goalQueryService = goalQueryService;
    }

    @Override
    protected GoalViewResponseDTO doApply(String sessionId, NoContext context) {
        CaseGuard.requireSessionId(sessionId);
        return goalQueryService.getGoal(sessionId)
                .map(this::toResponse)
                .orElseGet(() -> GoalViewResponseDTO.empty(sessionId));
    }

    @Override
    public StrategyHandler<String, NoContext, GoalViewResponseDTO> getNext(String sessionId, NoContext context) {
        return defaultStrategyHandler;
    }

    private GoalViewResponseDTO toResponse(GoalSnapshotVO snapshot) {
        return new GoalViewResponseDTO(
                snapshot.sessionId(), snapshot.goalId(), snapshot.revision(), snapshot.title(),
                snapshot.description(), snapshot.status().name(), snapshot.blockedReason(),
                snapshot.maxRounds(), snapshot.roundsStarted(), snapshot.continuationEnabled(),
                snapshot.createdAt(), snapshot.updatedAt(), true
        );
    }
}
