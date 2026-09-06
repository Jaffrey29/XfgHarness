package cn.walioffice.deepseek.harness.domain.acp.model.valobj;

import java.util.List;

/**
 * Response from an ACP prompt turn.
 *
 * @param sessionId     session id
 * @param stopReason    reason the turn ended (end_turn, cancelled, etc.)
 * @param messages      assistant output messages
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public record AcpPromptResponse(String sessionId, String stopReason, List<AcpMessage> messages) {}
