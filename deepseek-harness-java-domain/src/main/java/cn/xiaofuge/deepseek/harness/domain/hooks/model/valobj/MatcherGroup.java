package cn.xiaofuge.deepseek.harness.domain.hooks.model.valobj;

import java.util.List;

/**
 * One matcher group: a matcher pattern (null/empty/"*" = match-all) plus the
 * command hooks that run when it matches.
 *
 * @param matcher matcher pattern (null = match-all)
 * @param hooks   command hooks to run on match
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public record MatcherGroup(String matcher, List<CommandHook> hooks) {
    public MatcherGroup {
        hooks = hooks == null ? List.of() : List.copyOf(hooks);
    }
}
