package cn.xiaofuge.deepseek.harness.domain.guard.service;

import cn.xiaofuge.deepseek.harness.domain.guard.IGuardService;
import cn.xiaofuge.deepseek.harness.domain.guard.model.valobj.GuardDecision;
import cn.xiaofuge.deepseek.harness.domain.guard.reminder.RepeatToolReminderEngine;

import java.util.Optional;

/**
 * Composes the timeout-policy and repeat-tool-reminder guards into one seam.
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public class GuardService implements IGuardService {

    private final RepeatToolReminderEngine reminderEngine;

    public GuardService(RepeatToolReminderEngine reminderEngine) {
        this.reminderEngine = reminderEngine;
    }

    public GuardService() {
        this(new RepeatToolReminderEngine());
    }

    @Override
    public GuardDecision postExecute(Optional<String> agentId, String toolName, String arguments, String resultJson) {
        if (agentId.isEmpty()) return GuardDecision.allow();
        Optional<String> reminder = reminderEngine.observe(agentId.get(), toolName, arguments);
        return reminder.map(GuardDecision::allowWithReminder).orElse(GuardDecision.allow());
    }

    @Override
    public void preStep(String agentId, boolean hasUserMessage) {
        if (hasUserMessage) reminderEngine.reset(agentId);
    }
}
