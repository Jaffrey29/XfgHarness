package cn.xiaofuge.deepseek.harness.domain.acp.model.valobj;

/**
 * Session metadata for an ACP session.
 *
 * @param sessionId  session id
 * @param cwd        working directory
 * @param mcpConfig  MCP configuration JSON (optional)
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public record AcpSessionInfo(String sessionId, String cwd, String mcpConfig) {}
