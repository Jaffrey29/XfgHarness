package cn.xiaofuge.deepseek.harness.api.service.query;

import static org.junit.jupiter.api.Assertions.*;

import cn.xiaofuge.deepseek.harness.api.response.goal.GoalViewResponse;
import cn.xiaofuge.deepseek.harness.domain.goal.model.valobj.GoalSnapshotVO;
import cn.xiaofuge.deepseek.harness.domain.goal.model.valobj.GoalStatus;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class GoalQueryApiTest {

    @Test
    void shouldReturnEmptyWhenNoGoalExists() {
        GoalQueryApi api = new GoalQueryApi(sessionId -> Optional.empty());

        GoalViewResponse response = api.getGoal("session-1");

        assertFalse(response.exists());
        assertEquals("session-1", response.sessionId());
    }

    @Test
    void shouldMapGoalSnapshot() {
        GoalSnapshotVO snapshot = new GoalSnapshotVO("session-1", "goal-1", 3, "标题", "描述", GoalStatus.ACTIVE, null, 8, 1, true, Instant.EPOCH, Instant.EPOCH);
        GoalQueryApi api = new GoalQueryApi(sessionId -> Optional.of(snapshot));

        GoalViewResponse response = api.getGoal("session-1");

        assertTrue(response.exists());
        assertEquals("goal-1", response.goalId());
        assertEquals("ACTIVE", response.status());
    }

    @Test
    void shouldRejectBlankSessionId() {
        GoalQueryApi api = new GoalQueryApi(sessionId -> Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> api.getGoal(" "));
    }
}
