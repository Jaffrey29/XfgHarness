package cn.walioffice.deepseek.harness.domain.goal.service;

import static org.junit.jupiter.api.Assertions.*;

import cn.walioffice.deepseek.harness.domain.goal.adapter.repository.IGoalRepository;
import cn.walioffice.deepseek.harness.domain.goal.model.entity.GoalAggregate;
import cn.walioffice.deepseek.harness.domain.goal.model.valobj.GoalAction;
import cn.walioffice.deepseek.harness.domain.goal.model.valobj.GoalCommandVO;
import cn.walioffice.deepseek.harness.domain.goal.model.valobj.GoalSnapshotVO;
import cn.walioffice.deepseek.harness.domain.goal.model.valobj.GoalStatus;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class GoalServiceTest {

    @Test
    void createPauseAndResumeShouldAdvanceState() {
        InMemoryGoalRepository repository = new InMemoryGoalRepository();
        GoalService service = new GoalService(repository, 8);

        GoalSnapshotVO created = service.apply("session-1", new GoalCommandVO(
                GoalAction.CREATE, null, "重构架构", "先补齐领域边界", null, null, true));
        assertEquals("goal-session-1", created.goalId());
        assertEquals(GoalStatus.ACTIVE, created.status());
        assertEquals(8, created.maxRounds());

        GoalSnapshotVO paused = service.apply("session-1", new GoalCommandVO(
                GoalAction.PAUSE, null, null, null, null, null, null));
        assertEquals(GoalStatus.PAUSED, paused.status());
        assertEquals(2L, paused.revision());

        GoalSnapshotVO resumed = service.apply("session-1", new GoalCommandVO(
                GoalAction.RESUME, null, null, null, null, null, null));
        assertEquals(GoalStatus.ACTIVE, resumed.status());
        assertEquals(1, resumed.roundsStarted());

        assertTrue(service.getGoal("session-1").isPresent());
        assertEquals(3, repository.saved.size());
    }

    @Test
    void createShouldRejectDuplicateActiveGoal() {
        InMemoryGoalRepository repository = new InMemoryGoalRepository();
        GoalService service = new GoalService(repository, 4);

        service.apply("session-2", new GoalCommandVO(GoalAction.CREATE, null, "目标一", null, null, 4, null));
        assertThrows(IllegalStateException.class, () -> service.apply("session-2", new GoalCommandVO(
                GoalAction.CREATE, null, "目标二", null, null, 4, null)));
    }

    private static final class InMemoryGoalRepository implements IGoalRepository {
        private final Map<String, GoalAggregate> data = new HashMap<>();
        private final List<GoalAggregate> saved = new ArrayList<>();

        @Override
        public Optional<GoalAggregate> findBySessionId(String sessionId) {
            return Optional.ofNullable(data.get(sessionId));
        }

        @Override
        public void save(GoalAggregate aggregate) {
            data.put(aggregate.sessionId(), aggregate);
            saved.add(aggregate);
        }

        @Override
        public List<GoalAggregate> queryAll() {
            return new ArrayList<>(data.values());
        }
    }
}
