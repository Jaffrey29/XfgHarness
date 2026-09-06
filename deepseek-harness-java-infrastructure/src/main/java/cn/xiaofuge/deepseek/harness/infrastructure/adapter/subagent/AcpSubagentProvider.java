package cn.xiaofuge.deepseek.harness.infrastructure.adapter.subagent;

import java.util.List;
import java.util.Map;

/**
 * Out-of-process ACP subagent backend. Each child has its own process, session,
 * model, and tools, so it shares no parent context. Corresponds to TS
 * {@code subagent-acp}.
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public class AcpSubagentProvider extends OutOfProcessSubagentProvider {

    public AcpSubagentProvider(String command, List<String> args, String cwd,
                               Map<String, String> env, long timeoutMs) {
        super(command, args, cwd, env, timeoutMs);
    }

    @Override
    public String name() { return "acp"; }

    @Override
    protected String encodePrompt(SubagentStartRequest request) {
        // ACP uses JSON-RPC; for simplicity we send the prompt as plain text.
        // A full implementation would do initialize → session/new → session/prompt.
        return request.prompt();
    }
}
