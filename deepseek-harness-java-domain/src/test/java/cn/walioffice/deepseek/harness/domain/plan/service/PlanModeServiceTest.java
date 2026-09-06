package cn.walioffice.deepseek.harness.domain.plan.service;

import static org.junit.jupiter.api.Assertions.*;

import cn.walioffice.deepseek.harness.domain.plan.model.valobj.PlanModeState;
import cn.walioffice.deepseek.harness.domain.session.event.model.entity.SessionHeader;
import cn.walioffice.deepseek.harness.domain.session.event.model.entity.SessionLog;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class PlanModeServiceTest {

    @Test
    void enterSubmitAndFoldStateShouldWork() {
        PlanModeService service = new PlanModeService();
        SessionLog session = new SessionLog("session-1", SessionHeader.create("session-1", Instant.now(), "/tmp", "agent"));
        AtomicReference<PlanModeState> state = new AtomicReference<>(new PlanModeState.Off());

        service.enterPlanMode(session, state);
        assertInstanceOf(PlanModeState.Planning.class, state.get());
        assertEquals(1, session.size());

        service.submitPlan(session, state, "完成重构后再上线");
        assertInstanceOf(PlanModeState.Approved.class, state.get());
        assertEquals("完成重构后再上线", ((PlanModeState.Approved) state.get()).plan());

        PlanModeState folded = service.foldState(session);
        assertInstanceOf(PlanModeState.Approved.class, folded);
        assertEquals("完成重构后再上线", ((PlanModeState.Approved) folded).plan());
    }

    @Test
    void validatesInputs() {
        PlanModeService service = new PlanModeService();
        SessionLog session = new SessionLog("session-2", SessionHeader.create("session-2", Instant.now(), "/tmp", "agent"));
        AtomicReference<PlanModeState> state = new AtomicReference<>(new PlanModeState.Off());

        assertThrows(IllegalArgumentException.class, () -> service.enterPlanMode(null, state));
        assertThrows(IllegalArgumentException.class, () -> service.enterPlanMode(session, null));
        assertThrows(IllegalArgumentException.class, () -> service.submitPlan(session, state, " "));
        assertThrows(IllegalArgumentException.class, () -> service.foldState(null));
    }
}
