package cn.walioffice.deepseek.harness.domain.hooks.model.valobj;

import java.util.Optional;

/**
 * Dialect-neutral outcome a hook produced, parsed from exit code + stdout JSON + stderr.
 * Every field is optional because a hook may exercise any subset.
 *
 * @param exitCode          raw process exit code (null if hook could not run)
 * @param stderr            trimmed stderr
 * @param stdout            trimmed stdout
 * @param continueFlag      false means the hook asked to halt
 * @param stopReason        human-readable reason when continueFlag is false
 * @param decision          neutral blocking decision
 * @param reason            reason/explanation accompanying decision
 * @param additionalContext extra context to inject for the next model request
 * @param systemMessage     warning surfaced to the user
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public record HookOutput(
        Integer exitCode,
        String stderr,
        String stdout,
        Optional<Boolean> continueFlag,
        Optional<String> stopReason,
        Optional<HookDecision> decision,
        Optional<String> reason,
        Optional<String> additionalContext,
        Optional<String> systemMessage
) {
    public enum HookDecision { APPROVE, ALLOW, BLOCK, DENY, ASK }
}
