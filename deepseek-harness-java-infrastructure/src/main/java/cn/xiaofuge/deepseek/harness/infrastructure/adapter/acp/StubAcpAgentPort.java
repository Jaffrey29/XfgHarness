package cn.xiaofuge.deepseek.harness.infrastructure.adapter.acp;

import cn.xiaofuge.deepseek.harness.domain.acp.adapter.port.IAcpAgentPort;
import cn.xiaofuge.deepseek.harness.domain.acp.model.valobj.AcpPromptRequest;
import cn.xiaofuge.deepseek.harness.domain.acp.model.valobj.AcpPromptResponse;
import cn.xiaofuge.deepseek.harness.domain.acp.model.valobj.AcpSessionInfo;

/** Stub ACP agent port used when no ACP command is configured. */
public class StubAcpAgentPort implements IAcpAgentPort {
    @Override public AcpSessionInfo initialize(String cwd, String mcpConfig) {
        throw new IllegalStateException("ACP command not configured");
    }
    @Override public AcpPromptResponse prompt(AcpPromptRequest request) {
        throw new IllegalStateException("ACP command not configured");
    }
    @Override public void cancel(String sessionId) {}
    @Override public void dispose(String sessionId) {}
}
