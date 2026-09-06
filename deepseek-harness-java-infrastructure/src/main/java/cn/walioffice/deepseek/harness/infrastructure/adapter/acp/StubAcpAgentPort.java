package cn.walioffice.deepseek.harness.infrastructure.adapter.acp;

import cn.walioffice.deepseek.harness.domain.acp.adapter.port.IAcpAgentPort;
import cn.walioffice.deepseek.harness.domain.acp.model.valobj.*;

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
