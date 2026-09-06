package cn.xiaofuge.deepseek.harness.domain.acp.service;

import cn.xiaofuge.deepseek.harness.domain.acp.IAcpServerService;
import cn.xiaofuge.deepseek.harness.domain.acp.adapter.port.IAcpAgentPort;
import cn.xiaofuge.deepseek.harness.domain.acp.model.valobj.AcpPromptRequest;
import cn.xiaofuge.deepseek.harness.domain.acp.model.valobj.AcpPromptResponse;
import cn.xiaofuge.deepseek.harness.domain.acp.model.valobj.AcpSessionInfo;

/**
 * ACP server service: delegates session management and prompt driving to the
 * agent port.
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public class AcpServerService implements IAcpServerService {

    private final IAcpAgentPort agentPort;

    public AcpServerService(IAcpAgentPort agentPort) {
        this.agentPort = agentPort;
    }

    @Override
    public AcpSessionInfo newSession(String cwd, String mcpConfig) {
        return agentPort.initialize(cwd, mcpConfig);
    }

    @Override
    public AcpPromptResponse prompt(AcpPromptRequest request) {
        return agentPort.prompt(request);
    }

    @Override
    public void cancel(String sessionId) {
        agentPort.cancel(sessionId);
    }

    @Override
    public void closeSession(String sessionId) {
        agentPort.dispose(sessionId);
    }
}
