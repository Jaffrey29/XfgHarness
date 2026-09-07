package cn.xiaofuge.deepseek.harness.domain.guard.model.valobj;

import java.util.Optional;

/**
 * Decision returned by a guard after observing a tool execution.
 *
 * @param kind              {@code allow} or {@code block}
 * @param feedback          model-facing feedback when blocked
 * @param additionalContext optional reminder text to prepend into the next request
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public record GuardDecision(Kind kind, String feedback, Optional<String> additionalContext) {

    /**
     * Kind 定义当前模块中的enum契约与职责。
     */
    public enum Kind { ALLOW, BLOCK }

    /**
     * 处理allow相关逻辑。
     */
    public static GuardDecision allow() {
        return new GuardDecision(Kind.ALLOW, null, Optional.empty());
    }

    /**
     * 处理allow With Reminder相关逻辑。
     */
    public static GuardDecision allowWithReminder(String reminder) {
        return new GuardDecision(Kind.ALLOW, null, Optional.of(reminder));
    }

    /**
     * 处理block相关逻辑。
     */
    public static GuardDecision block(String feedback) {
        return new GuardDecision(Kind.BLOCK, feedback, Optional.empty());
    }

    /**
     * 处理block With Reminder相关逻辑。
     */
    public static GuardDecision blockWithReminder(String feedback, String reminder) {
        return new GuardDecision(Kind.BLOCK, feedback, Optional.of(reminder));
    }
}
