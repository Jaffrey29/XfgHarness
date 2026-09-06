package cn.xiaofuge.deepseek.harness.domain.session.event.model.valobj;

/**
 * Payload for the {@code plan/mode} session event.
 * <p>
 * Records a plan-mode transition: entering or exiting plan mode, with
 * the approved plan content when exiting.
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public record PlanModePayload(
        String mode,         // "enter" | "exit"
        String plan          // the approved plan text (null on enter)
) {
    public PlanModePayload {
        if (mode == null || (!mode.equals("enter") && !mode.equals("exit"))) {
            throw new IllegalArgumentException("mode must be 'enter' or 'exit'");
        }
    }
}
