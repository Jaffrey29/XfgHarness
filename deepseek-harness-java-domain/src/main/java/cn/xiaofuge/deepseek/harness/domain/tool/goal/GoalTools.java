package cn.xiaofuge.deepseek.harness.domain.tool.goal;

import cn.xiaofuge.deepseek.harness.domain.goal.IGoalCommandService;
import cn.xiaofuge.deepseek.harness.domain.goal.IGoalQueryService;
import cn.xiaofuge.deepseek.harness.domain.goal.model.valobj.GoalAction;
import cn.xiaofuge.deepseek.harness.domain.goal.model.valobj.GoalCommandVO;
import cn.xiaofuge.deepseek.harness.domain.goal.model.valobj.GoalSnapshotVO;
import cn.xiaofuge.deepseek.harness.domain.model.valobj.TextBlock;
import cn.xiaofuge.deepseek.harness.domain.model.entity.ToolDefinition;
import cn.xiaofuge.deepseek.harness.domain.model.entity.ToolExecutionResult;
import cn.xiaofuge.deepseek.harness.domain.model.entity.ToolRunContext;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 目标管理工具组：goal_get / goal_create / goal_update。
 * <p>
 * Mirrors TS {@code tool-goal}. 目标是跨回合的长期任务状态机，
 * 适合多步骤、需要续跑的任务。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public final class GoalTools {

    /**
     * 处理Goal Tools相关逻辑。
     */
    private GoalTools() {}

    /**
     * Base 定义当前模块中的class契约与职责。
     */
    private abstract static class Base implements ToolDefinition {
        @SuppressWarnings("unchecked")
        static Map<String, Object> argMap(Object args) {
            return args instanceof Map ? (Map<String, Object>) args : Map.of();
        }

        static String str(Map<String, Object> m, String k) {
            Object v = m.get(k);
            return v == null ? null : String.valueOf(v).trim();
        }

        CompletableFuture<ToolExecutionResult> ok(String text) {
            return CompletableFuture.completedFuture(
                    ToolExecutionResult.ok(List.of(new TextBlock(text))));
        }

        CompletableFuture<ToolExecutionResult> fail(String msg, String code) {
            return CompletableFuture.completedFuture(ToolExecutionResult.fail(msg, code));
        }

        static String snapshot(GoalSnapshotVO s) {
            return s == null ? "(no goal)" : s.toString();
        }
    }

    /** goal_get：读取当前目标。 */
    public static final class Get extends Base {
        private final IGoalQueryService query;
        /**
         * 获取。
         */
        private final String sessionId;

        /**
         * 获取。
         */
        public Get(IGoalQueryService query, String sessionId) {
            this.query = query;
            this.sessionId = sessionId;
        }

        @Override public String name() { return "goal_get"; }
        @Override public String description() {
            return "Get the current goal state for this session (title, status, rounds).";
        }
        @Override public Map<String, Object> parameters() {
            return Map.of("type", "object", "additionalProperties", false, "properties", Map.of());
        }
        @Override public CompletableFuture<ToolExecutionResult> execute(Object args, ToolRunContext ctx) {
            try {
                return ok("Current goal:\n" + query.getGoal(sessionId)
                        .map(Base::snapshot).orElse("(no active goal)"));
            } catch (Exception e) {
                return fail("goal_get failed: " + e.getMessage(), "EXECUTION_ERROR");
            }
        }
        @Override public boolean isConcurrencySafe(Object args) { return true; }
    }

    /** goal_create：创建目标。 */
    public static final class Create extends Base {
        /**
         * 创建。
         */
        private final IGoalCommandService command;
        private final String sessionId;

        /**
         * 创建。
         */
        public Create(IGoalCommandService command, String sessionId) {
            this.command = command;
            this.sessionId = sessionId;
        }

        @Override public String name() { return "goal_create"; }
        @Override public String description() {
            return "Create a long-running goal for this session. Goals persist across turns " +
                    "and drive continuation until completed or blocked.";
        }
        @Override public Map<String, Object> parameters() {
            return Map.of("type", "object", "additionalProperties", false,
                    "properties", Map.of(
                            "title", Map.of("type", "string", "description", "Short goal title."),
                            "description", Map.of("type", "string", "description", "Detailed goal description."),
                            "max_rounds", Map.of("type", "integer", "description", "Optional cap on continuation rounds.")),
                    "required", List.of("title"));
        }
        @Override public CompletableFuture<ToolExecutionResult> execute(Object args, ToolRunContext ctx) {
            Map<String, Object> m = argMap(args);
            String title = str(m, "title");
            if (title == null || title.isEmpty()) return fail("title is required", "MISSING_PARAM");
            Integer maxRounds = m.get("max_rounds") instanceof Number n ? n.intValue() : null;
            try {
                GoalSnapshotVO s = command.apply(sessionId, new GoalCommandVO(
                        GoalAction.CREATE, null, title, str(m, "description"), null, maxRounds, null));
                return ok("Goal created:\n" + snapshot(s));
            } catch (Exception e) {
                return fail("goal_create failed: " + e.getMessage(), "EXECUTION_ERROR");
            }
        }
    }

    /** goal_update：更新/推进目标状态。 */
    public static final class Update extends Base {
        private final IGoalCommandService command;
        private final String sessionId;

        /**
         * 处理Update相关逻辑。
         */
        public Update(IGoalCommandService command, String sessionId) {
            this.command = command;
            this.sessionId = sessionId;
        }

        @Override public String name() { return "goal_update"; }
        @Override public String description() {
            return "Update the current goal: change title/description, or transition state " +
                    "(pause/resume/complete/block/clear).";
        }
        @Override public Map<String, Object> parameters() {
            return Map.of("type", "object", "additionalProperties", false,
                    "properties", Map.of(
                            "action", Map.of("type", "string",
                                    "enum", List.of("update", "pause", "resume", "complete", "block", "clear"),
                                    "description", "State transition or plain update (default)."),
                            "title", Map.of("type", "string", "description", "New title (for update)."),
                            "description", Map.of("type", "string", "description", "New description (for update)."),
                            "blocked_reason", Map.of("type", "string", "description", "Why blocked (for block)."),
                            "continuation_enabled", Map.of("type", "boolean", "description", "Toggle auto-continuation.")));
        }
        @Override public CompletableFuture<ToolExecutionResult> execute(Object args, ToolRunContext ctx) {
            Map<String, Object> m = argMap(args);
            String action = str(m, "action");
            GoalAction ga = action == null || action.isEmpty() ? GoalAction.UPDATE
                    : switch (action.toLowerCase()) {
                        case "update" -> GoalAction.UPDATE;
                        case "pause" -> GoalAction.PAUSE;
                        case "resume" -> GoalAction.RESUME;
                        case "complete" -> GoalAction.COMPLETE;
                        case "block" -> GoalAction.BLOCK;
                        case "clear" -> GoalAction.CLEAR;
                        default -> null;
                    };
            if (ga == null) return fail("unknown action: " + action, "INVALID_ARGS");
            Boolean cont = m.get("continuation_enabled") == null
                    ? null : Boolean.parseBoolean(String.valueOf(m.get("continuation_enabled")));
            try {
                GoalSnapshotVO s = command.apply(sessionId, new GoalCommandVO(
                        ga, null, str(m, "title"), str(m, "description"),
                        str(m, "blocked_reason"), null, cont));
                return ok("Goal updated:\n" + snapshot(s));
            } catch (Exception e) {
                return fail("goal_update failed: " + e.getMessage(), "EXECUTION_ERROR");
            }
        }
    }
}
