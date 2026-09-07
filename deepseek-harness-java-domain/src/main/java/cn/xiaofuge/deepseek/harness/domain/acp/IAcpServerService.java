package cn.xiaofuge.deepseek.harness.domain.acp;

import cn.xiaofuge.deepseek.harness.domain.acp.model.valobj.AcpPromptRequest;
import cn.xiaofuge.deepseek.harness.domain.acp.model.valobj.AcpPromptResponse;
import cn.xiaofuge.deepseek.harness.domain.acp.model.valobj.AcpSessionInfo;

/**
 * Application-facing ACP server seam. Exposes fresh harness sessions to trusted
 * programmatic clients over JSON-RPC stdio.
 *
 * <p>调用流程：应用领域规则 → 更新聚合或值对象 → 发布事件/返回结果。</p>
 * <p>示例：领域对象只暴露业务语言方法，不直接依赖基础设施。</p>
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
