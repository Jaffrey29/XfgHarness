package cn.walioffice.deepseek.harness.domain.acp.adapter.port;

import cn.walioffice.deepseek.harness.domain.acp.model.valobj.AcpPromptRequest;
import cn.walioffice.deepseek.harness.domain.acp.model.valobj.AcpPromptResponse;
import cn.walioffice.deepseek.harness.domain.acp.model.valobj.AcpSessionInfo;

/**
 * Port for driving an ACP agent over JSON-RPC stdio.
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public interface IAcpAgentPort {

    /**
     * Initialize a new ACP session.
     *
     * @param cwd       working directory
     * @param mcpConfig MCP configuration JSON (optional)
     * @return session info
     */
    AcpSessionInfo initialize(String cwd, String mcpConfig);

    /**
     * Send a prompt to an ACP session and wait for the turn to end.
     *
     * @param request prompt request
     * @return prompt response
     */
    AcpPromptResponse prompt(AcpPromptRequest request);

    /**
     * Cancel an in-flight prompt turn.
     *
     * @param sessionId session id
     */
    void cancel(String sessionId);

    /**
     * Dispose a session and close the agent connection.
     *
     * @param sessionId session id
     */
    void dispose(String sessionId);
}
