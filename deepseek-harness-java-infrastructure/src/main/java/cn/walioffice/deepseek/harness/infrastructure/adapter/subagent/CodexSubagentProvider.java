package cn.walioffice.deepseek.harness.infrastructure.adapter.subagent;

import cn.walioffice.deepseek.harness.domain.agent.subagent.SubagentProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import java.util.Map;

/**
 * Out-of-process Codex subagent backend. Drives a child agent in a spawned
 * subprocess using the Codex wire protocol. Corresponds to TS
 * {@code subagent-codex}.
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public class CodexSubagentProvider extends OutOfProcessSubagentProvider {

    private final ObjectMapper mapper = new ObjectMapper();

    public CodexSubagentProvider(String command, List<String> args, String cwd,
                                 Map<String, String> env, long timeoutMs) {
        super(command, args, cwd, env, timeoutMs);
    }

    @Override
    public String name() { return "codex"; }

    @Override
    protected String encodePrompt(SubagentStartRequest request) {
        // Codex wire format: JSON with instructions and optional tools
        try {
            ObjectNode root = mapper.createObjectNode();
            root.put("instructions", request.prompt());
            if (request.parentCwd() != null) root.put("cwd", request.parentCwd());
            return mapper.writeValueAsString(root);
        } catch (Exception e) {
            return request.prompt();
        }
    }
}
