package cn.xiaofuge.deepseek.harness.domain.guard.service;

import cn.xiaofuge.deepseek.harness.domain.guard.IGuardService;
import cn.xiaofuge.deepseek.harness.domain.guard.model.valobj.GuardDecision;
import cn.xiaofuge.deepseek.harness.domain.guard.reminder.RepeatToolReminderEngine;

import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Composes the timeout-policy and repeat-tool-reminder guards into one seam.
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
@Service
public class GuardService implements IGuardService {

    private final RepeatToolReminderEngine reminderEngine;

    /**
     * 处理Guard Service相关逻辑。
     */
    public GuardService(RepeatToolReminderEngine reminderEngine) {
        this.reminderEngine = reminderEngine;
    }

    @Autowired
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
