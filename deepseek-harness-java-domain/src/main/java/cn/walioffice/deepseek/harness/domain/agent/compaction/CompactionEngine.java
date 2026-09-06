package cn.walioffice.deepseek.harness.domain.agent.compaction;

import cn.walioffice.deepseek.harness.domain.agent.model.entity.AgentOptions;
import cn.walioffice.deepseek.harness.domain.llm.model.entity.Message;
import cn.walioffice.deepseek.harness.domain.llm.model.valobj.GenerateOptions;
import cn.walioffice.deepseek.harness.domain.session.event.model.entity.SessionLog;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Port for compaction engines.
 * <p>
 * Mirrors TS {@code CompactionEngine}: implementations own the trigger
 * policy, retention, and summarization. A successful run replaces the
 * selected surface span with one summary message.
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public interface CompactionEngine {

    /**
     * Consider automatic compaction.
     *
     * @param session the session log to compact
     * @param options agent options for model routing
     * @param trigger the reason compaction is being considered
     * @return the result, or null if no compaction was needed
     */
    CompletableFuture<CompactionResult> compactIfNeeded(
            SessionLog session,
            AgentOptions options,
            CompactionTrigger trigger
    );

    /**
     * Explicitly compact useful history.
     *
     * @param session the session log to compact
     * @param options agent options for model routing
     * @return the result, or null if no safe range exists
     */
    CompletableFuture<CompactionResult> compactNow(
            SessionLog session,
            AgentOptions options
    );

    /** Why compaction is being considered. */
    enum CompactionTrigger {
        PRESSURE,
        CONTEXT_OVERFLOW
    }
}
