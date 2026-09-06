package cn.xiaofuge.deepseek.harness.domain.hooks.model.valobj;

import java.util.Optional;

/**
 * Restrictive merge of multiple hook outputs at one hook point.
 *
 * @param decision          merged decision (block/deny wins over allow/approve)
 * @param additionalContext concatenated additional contexts
 * @param systemMessage     concatenated system messages
 * @param stop              whether any hook asked to stop
 * @param stopReason        reason from the first stop
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public record MergedHookOutcome(
        Optional<HookOutput.HookDecision> decision,
        String additionalContext,
        String systemMessage,
        boolean stop,
        Optional<String> stopReason
) {}
