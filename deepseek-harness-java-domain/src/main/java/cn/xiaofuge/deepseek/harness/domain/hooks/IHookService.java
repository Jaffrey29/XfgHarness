package cn.xiaofuge.deepseek.harness.domain.hooks;

import cn.xiaofuge.deepseek.harness.domain.hooks.model.valobj.HookDialect;
import cn.xiaofuge.deepseek.harness.domain.hooks.model.valobj.HookPoint;
import cn.xiaofuge.deepseek.harness.domain.hooks.model.valobj.MergedHookOutcome;

import java.util.Map;

/**
 * Application-facing hook seam. Matches configured hooks against a hook point
 * and tool name, runs them, and merges outcomes.
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public interface IHookService {

    /**
     * Run all matching hooks for a given hook point.
     *
     * @param dialect     hook dialect (claude-code or codex)
     * @param point       hook interception point
     * @param toolName    tool name for PreToolUse/PostToolUse (null otherwise)
     * @param payloadJson hook event payload as JSON
     * @param environment environment variables for hook processes
     * @return merged outcome
     */
    MergedHookOutcome runHooks(HookDialect dialect, HookPoint point, String toolName,
                               String payloadJson, Map<String, String> environment);
}
