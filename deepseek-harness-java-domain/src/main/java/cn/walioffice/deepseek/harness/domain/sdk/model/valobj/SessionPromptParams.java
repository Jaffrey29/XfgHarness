package cn.walioffice.deepseek.harness.domain.sdk.model.valobj;

import cn.walioffice.deepseek.harness.domain.llm.model.valobj.ContentBlock;
import java.util.List;

/**
 * One user turn on one SDK session.
 *
 * @param sessionId     the SDK-side session id
 * @param contentBlocks the prompt content blocks, sent verbatim as the user message
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public record SessionPromptParams(String sessionId, List<ContentBlock> contentBlocks) {}
