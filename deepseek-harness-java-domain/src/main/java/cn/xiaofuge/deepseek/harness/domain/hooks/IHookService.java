package cn.xiaofuge.deepseek.harness.domain.hooks;

import cn.xiaofuge.deepseek.harness.domain.hooks.model.valobj.HookDialect;
import cn.xiaofuge.deepseek.harness.domain.hooks.model.valobj.HookPoint;
import cn.xiaofuge.deepseek.harness.domain.hooks.model.valobj.MergedHookOutcome;

import java.util.Map;

/**
 * Application-facing hook seam. Matches configured hooks against a hook point
 * and tool name, runs them, and merges outcomes.
 *
 * <p>调用流程：应用领域规则 → 更新聚合或值对象 → 发布事件/返回结果。</p>
 * <p>示例：领域对象只暴露业务语言方法，不直接依赖基础设施。</p>
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
