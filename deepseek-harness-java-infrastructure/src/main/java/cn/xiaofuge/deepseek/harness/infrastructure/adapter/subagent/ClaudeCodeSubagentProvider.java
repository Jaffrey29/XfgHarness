package cn.xiaofuge.deepseek.harness.infrastructure.adapter.subagent;

import java.util.List;
import java.util.Map;

/**
 * Out-of-process Claude Code subagent backend. Drives a child agent in a
 * spawned subprocess using the Claude Code CLI. Corresponds to TS
 * {@code subagent-claude-code}.
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public class ClaudeCodeSubagentProvider extends OutOfProcessSubagentProvider {

    /**
     * 处理Claude Code Subagent Provider相关逻辑。
     */
    public ClaudeCodeSubagentProvider(String command, List<String> args, String cwd,
                                      Map<String, String> env, long timeoutMs) {
        super(command, args, cwd, env, timeoutMs);
    }

    @Override
    public String name() { return "claude-code"; }

    @Override
    protected String encodePrompt(SubagentStartRequest request) {
        // Claude Code CLI: prompt is passed as plain text on stdin
        return request.prompt();
    }
}
