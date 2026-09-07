package cn.xiaofuge.deepseek.harness.domain.acp.adapter.port;

import cn.xiaofuge.deepseek.harness.domain.acp.model.valobj.AcpPromptRequest;
import cn.xiaofuge.deepseek.harness.domain.acp.model.valobj.AcpPromptResponse;
import cn.xiaofuge.deepseek.harness.domain.acp.model.valobj.AcpSessionInfo;

/**
 * Port for driving an ACP agent over JSON-RPC stdio.
 *
 * <p>调用流程：应用领域规则 → 更新聚合或值对象 → 发布事件/返回结果。</p>
 * <p>示例：领域对象只暴露业务语言方法，不直接依赖基础设施。</p>
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
