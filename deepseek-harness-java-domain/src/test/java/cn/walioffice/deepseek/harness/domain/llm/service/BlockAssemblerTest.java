package cn.walioffice.deepseek.harness.domain.llm.service;

import static org.junit.jupiter.api.Assertions.*;

import cn.walioffice.deepseek.harness.domain.llm.model.valobj.ContentBlock;
import cn.walioffice.deepseek.harness.domain.llm.model.valobj.FinishReason;
import cn.walioffice.deepseek.harness.domain.llm.model.valobj.StreamChunk;
import cn.walioffice.deepseek.harness.domain.llm.model.valobj.TextBlock;
import cn.walioffice.deepseek.harness.domain.llm.model.valobj.ToolCallBlock;
import cn.walioffice.deepseek.harness.domain.llm.model.valobj.TokenUsage;
import java.util.List;
import org.junit.jupiter.api.Test;

class BlockAssemblerTest {

    @Test
    void assemblesTextDeltasIntoSingleBlock() {
        BlockAssembler assembler = new BlockAssembler();
        assembler.push(new StreamChunk.BlockStart(0, "text"));
        assembler.push(new StreamChunk.TextDelta(0, "Hello"));
        assembler.push(new StreamChunk.TextDelta(0, " World"));
        assembler.push(new StreamChunk.Finish(new FinishReason.Stop()));

        List<ContentBlock> blocks = assembler.blocks();
        assertEquals(1, blocks.size());
        assertInstanceOf(TextBlock.class, blocks.get(0));
        assertEquals("Hello World", ((TextBlock) blocks.get(0)).text());
    }

    @Test
    void assemblesToolCallFromDeltasWithoutBlockStart() {
        BlockAssembler assembler = new BlockAssembler();
        // delta-only protocol: no BlockStart, just ToolCallDelta
        assembler.push(new StreamChunk.ToolCallDelta(0, "call-1", "fs.read", "{\"path\""));
        assembler.push(new StreamChunk.ToolCallDelta(0, null, null, ":\"a\"}"));
        assembler.push(new StreamChunk.Finish(new FinishReason.ToolCalls()));

        List<ContentBlock> blocks = assembler.blocks();
        assertEquals(1, blocks.size());
        assertInstanceOf(ToolCallBlock.class, blocks.get(0));
        ToolCallBlock tc = (ToolCallBlock) blocks.get(0);
        assertEquals("call-1", tc.id());
        assertEquals("fs.read", tc.name());
        assertEquals("{\"path\":\"a\"}", tc.arguments());
    }

    @Test
    void preservesBlockOrderAcrossMultipleBlocks() {
        BlockAssembler assembler = new BlockAssembler();
        assembler.push(new StreamChunk.BlockStart(0, "text"));
        assembler.push(new StreamChunk.TextDelta(0, "first"));
        assembler.push(new StreamChunk.BlockStart(1, "text"));
        assembler.push(new StreamChunk.TextDelta(1, "second"));
        assembler.push(new StreamChunk.Finish(new FinishReason.Stop()));

        List<ContentBlock> blocks = assembler.blocks();
        assertEquals(2, blocks.size());
        assertEquals("first", ((TextBlock) blocks.get(0)).text());
        assertEquals("second", ((TextBlock) blocks.get(1)).text());
    }

    @Test
    void maxTokensDropsToolCallBlocks() {
        BlockAssembler assembler = new BlockAssembler();
        assembler.push(new StreamChunk.BlockStart(0, "text"));
        assembler.push(new StreamChunk.TextDelta(0, "partial text"));
        assembler.push(new StreamChunk.BlockStart(1, "tool-call"));
        assembler.push(new StreamChunk.ToolCallDelta(1, "call-1", "shell.exec", "{}"));
        assembler.push(new StreamChunk.Finish(new FinishReason.MaxTokens()));

        List<ContentBlock> blocks = assembler.blocks();
        assertEquals(1, blocks.size());
        assertInstanceOf(TextBlock.class, blocks.get(0));
    }

    @Test
    void blockEndAuthoritativeOverDeltas() {
        BlockAssembler assembler = new BlockAssembler();
        assembler.push(new StreamChunk.BlockStart(0, "text"));
        assembler.push(new StreamChunk.TextDelta(0, "should be replaced"));
        assembler.push(new StreamChunk.BlockEnd(0, new TextBlock("final")));
        // straggler delta after close → ignored
        assembler.push(new StreamChunk.TextDelta(0, " ignored"));

        List<ContentBlock> blocks = assembler.blocks();
        assertEquals(1, blocks.size());
        assertEquals("final", ((TextBlock) blocks.get(0)).text());
    }

    @Test
    void usageAndFinishTracked() {
        BlockAssembler assembler = new BlockAssembler();
        TokenUsage usage = new TokenUsage(10, 20);
        assembler.push(new StreamChunk.Usage(usage));
        assembler.push(new StreamChunk.Finish(new FinishReason.Stop()));

        assertEquals(usage, assembler.usage());
        assertInstanceOf(FinishReason.Stop.class, assembler.finish());
    }

    @Test
    void defaultFinishIsStopWhenNoneProvided() {
        BlockAssembler assembler = new BlockAssembler();
        assertInstanceOf(FinishReason.Stop.class, assembler.finish());
    }
}
