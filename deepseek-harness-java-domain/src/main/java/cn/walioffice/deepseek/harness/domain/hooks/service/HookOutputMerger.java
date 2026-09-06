package cn.walioffice.deepseek.harness.domain.hooks.service;

import cn.walioffice.deepseek.harness.domain.hooks.model.valobj.HookOutput;
import cn.walioffice.deepseek.harness.domain.hooks.model.valobj.MergedHookOutcome;
import java.util.List;
import java.util.Optional;

/**
 * Restrictive merge of hook outputs: block/deny wins over allow/approve;
 * additional contexts and system messages are concatenated; stop wins.
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public final class HookOutputMerger {

    private HookOutputMerger() {}

    public static MergedHookOutcome merge(List<HookOutput> outputs) {
        Optional<HookOutput.HookDecision> decision = Optional.empty();
        StringBuilder additionalContext = new StringBuilder();
        StringBuilder systemMessage = new StringBuilder();
        boolean stop = false;
        Optional<String> stopReason = Optional.empty();

        for (HookOutput output : outputs) {
            // Decision: block/deny wins
            if (output.decision().isPresent()) {
                HookOutput.HookDecision d = output.decision().get();
                if (d == HookOutput.HookDecision.BLOCK || d == HookOutput.HookDecision.DENY) {
                    decision = Optional.of(d);
                } else if (decision.isEmpty()) {
                    decision = Optional.of(d);
                }
            }
            // Continue flag: false means stop
            if (output.continueFlag().isPresent() && !output.continueFlag().get()) {
                stop = true;
                if (stopReason.isEmpty()) stopReason = output.stopReason();
            }
            output.additionalContext().ifPresent(s -> {
                if (!additionalContext.isEmpty()) additionalContext.append("\n");
                additionalContext.append(s);
            });
            output.systemMessage().ifPresent(s -> {
                if (!systemMessage.isEmpty()) systemMessage.append("\n");
                systemMessage.append(s);
            });
        }

        return new MergedHookOutcome(
                decision,
                additionalContext.toString(),
                systemMessage.toString(),
                stop,
                stopReason
        );
    }
}
