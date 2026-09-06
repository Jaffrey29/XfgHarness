package cn.walioffice.deepseek.harness.domain.agent.compaction;

import cn.walioffice.deepseek.harness.domain.llm.model.entity.Message;
import java.util.List;

/**
 * Result of a compaction operation.
 * <p>
 * Mirrors TS {@code CompactionResult}: records the summary message,
 * the replaced event range, and token accounting.
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public record CompactionResult(
        String summary,
        long startSeq,
        long endSeq,
        long tokensBefore,
        long tokensAfter,
        List<Message> replacedMessages,
        Message summaryMessage
) {}
