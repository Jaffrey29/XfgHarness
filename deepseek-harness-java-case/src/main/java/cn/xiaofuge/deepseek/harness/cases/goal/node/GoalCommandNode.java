package cn.xiaofuge.deepseek.harness.cases.goal.node;

import cn.xiaofuge.deepseek.harness.api.dto.GoalCommandRequestDTO;
import cn.xiaofuge.deepseek.harness.domain.goal.model.valobj.GoalSnapshotVO;
import cn.xiaofuge.deepseek.harness.api.dto.GoalViewResponseDTO;
import cn.xiaofuge.deepseek.harness.cases.orchestration.AbstractStrategyRouter;
import cn.xiaofuge.deepseek.harness.cases.orchestration.NoContext;
import cn.xiaofuge.deepseek.harness.cases.orchestration.StrategyHandler;
import cn.xiaofuge.deepseek.harness.cases.support.CaseGuard;
import cn.xiaofuge.deepseek.harness.domain.goal.IGoalCommandService;
import cn.xiaofuge.deepseek.harness.domain.goal.model.valobj.GoalAction;
import cn.xiaofuge.deepseek.harness.domain.goal.model.valobj.GoalCommandVO;
import org.springframework.stereotype.Service;

@Service
public class GoalCommandNode
        extends AbstractStrategyRouter<GoalCommandNode.Input, NoContext, GoalViewResponseDTO> {

    private final IGoalCommandService goalCommandService;

    public GoalCommandNode(IGoalCommandService goalCommandService) {
        this.goalCommandService = goalCommandService;
    }

    @Override
    protected GoalViewResponseDTO doApply(Input input, NoContext context) {
        CaseGuard.requireSessionId(input.sessionId());
        CaseGuard.requireNonNull(input.request(), "goal request must not be blank");
        CaseGuard.requireText(input.request().action(), "goal action must not be blank");
        return toResponse(goalCommandService.apply(input.sessionId(), toDomain(input.request())));
    }

    @Override
    public StrategyHandler<Input, NoContext, GoalViewResponseDTO> getNext(Input input, NoContext context) {
        return defaultStrategyHandler;
    }

    private GoalCommandVO toDomain(GoalCommandRequestDTO request) {
        return new GoalCommandVO(
                GoalAction.valueOf(request.action().trim().toUpperCase(java.util.Locale.ROOT)),
                request.goalId(),
                request.title(),
                request.description(),
                request.blockedReason(),
                request.maxRounds(),
                request.continuationEnabled()
        );
    }

    public record Input(String sessionId, GoalCommandRequestDTO request) {
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
