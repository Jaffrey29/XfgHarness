package cn.xiaofuge.deepseek.harness.api.service.command;

import static org.junit.jupiter.api.Assertions.*;

import cn.xiaofuge.deepseek.harness.api.dto.goal.GoalCommandRequest;
import cn.xiaofuge.deepseek.harness.api.response.goal.GoalViewResponse;
import cn.xiaofuge.deepseek.harness.domain.goal.IGoalCommandService;
import cn.xiaofuge.deepseek.harness.domain.goal.model.valobj.GoalAction;
import cn.xiaofuge.deepseek.harness.domain.goal.model.valobj.GoalCommandVO;
import cn.xiaofuge.deepseek.harness.domain.goal.model.valobj.GoalSnapshotVO;
import cn.xiaofuge.deepseek.harness.domain.goal.model.valobj.GoalStatus;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class GoalCommandApiTest {

    @Test
    void shouldMapRequestToDomainAndResponse() {
        StubGoalCommandService service = new StubGoalCommandService();
        GoalCommandApi api = new GoalCommandApi(service);

        GoalViewResponse response = api.apply("session-1", new GoalCommandRequest(
                "create", "goal-1", "重构", "补齐边界", null, 12, true));

        assertEquals("session-1", service.lastSessionId);
        assertEquals(GoalAction.CREATE, service.lastCommand.action());
        assertEquals("goal-1", service.lastCommand.goalId());
        assertEquals("重构", response.title());
        assertTrue(response.exists());
    }

    @Test
    void shouldRejectInvalidSessionOrAction() {
        GoalCommandApi api = new GoalCommandApi((sessionId, command) -> null);

        assertThrows(IllegalArgumentException.class, () -> api.apply(" ", new GoalCommandRequest("create", null, "a", null, null, null, null)));
        assertThrows(IllegalArgumentException.class, () -> api.apply("session-1", new GoalCommandRequest("bad", null, "a", null, null, null, null)));
    }

    private static final class StubGoalCommandService implements IGoalCommandService {
        private String lastSessionId;
        private GoalCommandVO lastCommand;

        @Override
        public GoalSnapshotVO apply(String sessionId, GoalCommandVO command) {
            lastSessionId = sessionId;
            lastCommand = command;
            return new GoalSnapshotVO(sessionId, command.goalId(), 1L, command.title(), command.description(), GoalStatus.ACTIVE, null, 12, 0, true, Instant.EPOCH, Instant.EPOCH);
        }
    }
}
