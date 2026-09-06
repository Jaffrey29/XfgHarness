package cn.walioffice.deepseek.harness.domain.llm.service;

import cn.walioffice.deepseek.harness.domain.llm.model.entity.Message;
import cn.walioffice.deepseek.harness.domain.llm.model.entity.MessageSource;
import cn.walioffice.deepseek.harness.domain.llm.model.valobj.ContentBlock;
import cn.walioffice.deepseek.harness.domain.llm.model.valobj.TextBlock;
import cn.walioffice.deepseek.harness.domain.llm.model.valobj.ReasoningBlock;
import cn.walioffice.deepseek.harness.domain.llm.model.valobj.ToolCallBlock;
import cn.walioffice.deepseek.harness.domain.llm.model.valobj.ToolResultBlock;
import cn.walioffice.deepseek.harness.domain.llm.model.valobj.FinishReason;
import cn.walioffice.deepseek.harness.domain.llm.model.valobj.StreamChunk;
import cn.walioffice.deepseek.harness.domain.llm.model.valobj.TokenUsage;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Incrementally assembles raw {@link StreamChunk}s into complete
 * {@link ContentBlock}s and a final assistant {@link Message}.
 * <p>
 * The agent loop feeds it while logging raw chunks for replay fidelity, then
 * reads {@link #blocks()}/{@link #message(MessageSource)}/{@link #usage}/
 * {@link #finish()} once the stream ends.
 * <p>
 * Tolerant of delta-only protocols (no block-start/end); deltas arriving for
 * an index already closed by {@code block-end} are ignored (malformed stream).
 * Mirrors TS {@code BlockAssembler}.
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public class BlockAssembler {

    private static final class PartialBlock {
        String blockType;
        StringBuilder text = new StringBuilder();
        String toolCallId;
        String toolCallName;
        /** Set by block-end — authoritative, freezes the partial. */
        ContentBlock block;

        PartialBlock(String blockType) {
            this.blockType = blockType;
        }
    }

    private final Map<Integer, PartialBlock> partials = new LinkedHashMap<>();
    private final List<Integer> order = new ArrayList<>();
    private TokenUsage usage;
    private FinishReason finish;

    /** Feed one chunk into the assembly state. */
    public void push(StreamChunk chunk) {
        if (chunk instanceof StreamChunk.BlockStart bs) {
            if (!partials.containsKey(bs.index())) {
                order.add(bs.index());
                partials.put(bs.index(), new PartialBlock(bs.blockType()));
            }
        } else if (chunk instanceof StreamChunk.TextDelta td) {
            PartialBlock partial = ensure(td.index(), "text");
            if (partial.block != null) return; // closed; ignore stragglers
            partial.text.append(td.text());
        } else if (chunk instanceof StreamChunk.ReasoningDelta rd) {
            PartialBlock partial = ensure(rd.index(), "reasoning");
            if (partial.block != null) return;
            partial.text.append(rd.text());
        } else if (chunk instanceof StreamChunk.ToolCallDelta tcd) {
            PartialBlock partial = ensure(tcd.index(), "tool-call");
            if (partial.block != null) return;
            if (tcd.id() != null && !tcd.id().isEmpty()) partial.toolCallId = tcd.id();
            if (tcd.name() != null && !tcd.name().isEmpty()) partial.toolCallName = tcd.name();
            partial.text.append(tcd.argumentsDelta());
        } else if (chunk instanceof StreamChunk.BlockEnd be) {
            PartialBlock partial = ensure(be.index(), be.block() != null ? be.block().getClass().getSimpleName() : "unknown");
            if (partial.block != null) return; // first close wins
            partial.block = be.block();
        } else if (chunk instanceof StreamChunk.Usage u) {
            this.usage = u.usage();
        } else if (chunk instanceof StreamChunk.Finish f) {
            this.finish = f.reason();
        }
    }

    private PartialBlock ensure(int index, String blockType) {
        PartialBlock partial = partials.get(index);
        if (partial == null) {
            partial = new PartialBlock(blockType);
            partials.put(index, partial);
            order.add(index);
        }
        return partial;
    }

    private ContentBlock assemble(PartialBlock partial, int index) {
        if (partial.block != null) return partial.block;
        switch (partial.blockType) {
            case "text", "TextBlock" -> {
                return new TextBlock(partial.text.toString());
            }
            case "reasoning", "ReasoningBlock" -> {
                return new ReasoningBlock(partial.text.toString());
            }
            case "tool-call", "ToolCallBlock" -> {
                return new ToolCallBlock(
                        partial.toolCallId != null ? partial.toolCallId : "call-" + index,
                        partial.toolCallName != null ? partial.toolCallName : "",
                        partial.text.toString()
                );
            }
            default -> throw new IllegalStateException(
                    "cannot assemble incomplete block of type \"" + partial.blockType + "\"");
        }
    }

    /**
     * Assemble all blocks seen so far, in stream order.
     * <p>
     * Max-token truncation drops tool calls that cannot be executed safely.
     */
    public List<ContentBlock> blocks() {
        List<ContentBlock> all = new ArrayList<>();
        for (int index : order) {
            PartialBlock partial = partials.get(index);
            if (partial == null) throw new IllegalStateException(
                    "BlockAssembler invariant violated: no partial for index " + index);
            all.add(assemble(partial, index));
        }
        // max-tokens truncation: drop tool-call blocks
        if (finish != null && finish instanceof FinishReason.MaxTokens) {
            List<ContentBlock> kept = new ArrayList<>();
            for (ContentBlock block : all) {
                if (!(block instanceof ToolCallBlock)) {
                    kept.add(block);
                }
            }
            return kept;
        }
        return all;
    }

    /** Usage from the usage chunk; null until one arrives. */
    public TokenUsage usage() {
        return usage;
    }

    /** Finish reason from the finish chunk; {@code Stop} when the stream ended without one. */
    public FinishReason finish() {
        return finish != null ? finish : new FinishReason.Stop();
    }

    /**
     * The assembled assistant message.
     *
     * @param source producer attribution for the assembled message
     * @return an immutable assistant-role message over {@link #blocks()}
     */
    public Message message(MessageSource source) {
        return new Message(
                java.util.UUID.randomUUID().toString(),
                "assistant",
                blocks(),
                source != null ? source : new MessageSource.PluginSource("dsh-llm/assembler")
        );
    }
}
