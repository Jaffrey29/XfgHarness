package cn.xiaofuge.deepseek.harness.domain.acp.model.valobj;

/**
 * Request to send a prompt to an ACP session.
 *
 * @param sessionId  session id
 * @param messages   prompt messages
 * @param cwd        working directory for the session
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public record AcpPromptRequest(String sessionId, java.util.List<AcpMessage> messages, String cwd) {}
