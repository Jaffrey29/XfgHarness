package cn.xiaofuge.deepseek.harness.domain.hooks.service;

import cn.xiaofuge.deepseek.harness.domain.hooks.model.valobj.HookDialect;
import cn.xiaofuge.deepseek.harness.domain.hooks.model.valobj.MatcherGroup;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Matches a tool name against matcher groups using the dialect's mode.
 *
 * <p>Claude Code uses literal match (pipe = exact-match alternation) when the
 * pattern is purely {@code [A-Za-z0-9_|]+} and regex otherwise; Codex is always regex.
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public final class HookMatcher {

    /**
     * 处理Hook Matcher。
     * 流程：应用领域规则 → 更新领域对象 → 返回结果。
     * @return 方法执行结果。
     */
    private HookMatcher() {}

    /**
     * 处理matches。
     * 流程：应用领域规则 → 更新领域对象 → 返回结果。
     * @param dialect dialect参数。
     * @param matcher matcher参数。
     * @param toolName 工具 名称参数。
     * @return 方法执行结果。
     */
    public static boolean matches(HookDialect dialect, String matcher, String toolName) {
        if (matcher == null || matcher.isEmpty() || "*".equals(matcher)) return true;
        if (toolName == null) return false;

        if (dialect == HookDialect.CLAUDE_CODE && matcher.matches("[A-Za-z0-9_|]+")) {
            // Literal alternation
            for (String part : matcher.split("\\|")) {
                if (part.equals(toolName)) return true;
            }
            return false;
        }
        // Regex
        try {
            return Pattern.compile(matcher).matcher(toolName).find();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 过滤 Matching。
     * 流程：应用领域规则 → 更新领域对象 → 返回结果。
     * @param dialect dialect参数。
     * @param groups groups参数。
     * @param toolName 工具 名称参数。
     * @return 方法执行结果。
     */
    public static List<MatcherGroup> filterMatching(HookDialect dialect, List<MatcherGroup> groups, String toolName) {
        return groups.stream()
                .filter(g -> matches(dialect, g.matcher(), toolName))
                .toList();
    }
}
