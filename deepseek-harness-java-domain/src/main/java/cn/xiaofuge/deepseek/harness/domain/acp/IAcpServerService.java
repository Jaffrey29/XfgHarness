package cn.xiaofuge.deepseek.harness.domain.acp;

import cn.xiaofuge.deepseek.harness.domain.acp.model.valobj.AcpPromptRequest;
import cn.xiaofuge.deepseek.harness.domain.acp.model.valobj.AcpPromptResponse;
import cn.xiaofuge.deepseek.harness.domain.acp.model.valobj.AcpSessionInfo;

/**
 * Application-facing ACP server seam. Exposes fresh harness sessions to trusted
 * programmatic clients over JSON-RPC stdio.
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public interface IAcpServerService {

    /**
     * Create a new ACP session.
     *
     * @param cwd       working directory
     * @param mcpConfig MCP configuration JSON (optional)
     * @return session info
     */
    AcpSessionInfo newSession(String cwd, String mcpConfig);

    /**
     * Send a prompt to an ACP session.
     *
     * @param request prompt request
     * @return prompt response
     */
    AcpPromptResponse prompt(AcpPromptRequest request);

    /**
     * Cancel an in-flight prompt.
     *
     * @param sessionId session id
     */
    void cancel(String sessionId);

    /**
     * Close a session.
     *
     * @param sessionId session id
     */
    void closeSession(String sessionId);
}
