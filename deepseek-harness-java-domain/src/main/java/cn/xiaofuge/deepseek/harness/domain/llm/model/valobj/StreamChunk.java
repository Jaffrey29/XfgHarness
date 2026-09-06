package cn.xiaofuge.deepseek.harness.domain.llm.model.valobj;

import cn.xiaofuge.deepseek.harness.domain.model.valobj.ContentBlock;

/**
 * Raw streaming protocol emitted by adapters.
 * <p>
 * Block indexes correlate interleaved deltas, and {@link BlockEnd} carries the
 * assembled block. Adapters emit usage before the terminal finish and nothing
 * afterward. Mirrors TS {@code StreamChunk}.
 * <p>
 * Java 17 note: this is a sealed interface with record permits, exhaustive
 * {@code instanceof} chains replace pattern-matching switch.
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public sealed interface StreamChunk permits
        StreamChunk.BlockStart,
        StreamChunk.TextDelta,
        StreamChunk.ReasoningDelta,
        StreamChunk.ToolCallDelta,
        StreamChunk.BlockEnd,
        StreamChunk.Usage,
        StreamChunk.Finish,
        StreamChunk.Retry {

    /** A new content block started at the given index. */
    record BlockStart(int index, String blockType) implements StreamChunk {}

    /** Incremental text delta for the block at {@code index}. */
    record TextDelta(int index, String text) implements StreamChunk {}

    /** Incremental reasoning/thinking delta for the block at {@code index}. */
    record ReasoningDelta(int index, String text) implements StreamChunk {}

    /** Incremental tool-call argument delta for the block at {@code index}. */
    record ToolCallDelta(int index, String id, String name, String argumentsDelta) implements StreamChunk {}

    /** A content block finished assembling; carries the final block. */
    record BlockEnd(int index, ContentBlock block) implements StreamChunk {}

    /** Token usage report (may arrive once per stream, before finish). */
    record Usage(TokenUsage usage) implements StreamChunk {}

    /** Terminal chunk: the stream is done. */
    record Finish(FinishReason reason) implements StreamChunk {}

    /** A retry was scheduled after a failed model request attempt. */
    record Retry(int attempt, Integer maxAttempts, String failureMessage) implements StreamChunk {}

    /** Convenience: is this a text/reasoning/tool delta with content? */
    static boolean isTokenDelta(StreamChunk chunk) {
        if (chunk instanceof TextDelta td) return !td.text().isEmpty();
        if (chunk instanceof ReasoningDelta rd) return !rd.text().isEmpty();
        if (chunk instanceof ToolCallDelta tcd) return !tcd.argumentsDelta().isEmpty() || tcd.name() != null;
        return false;
    }
}
