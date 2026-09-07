package cn.xiaofuge.deepseek.harness.domain.tool.schedule;

import cn.xiaofuge.deepseek.harness.domain.model.valobj.TextBlock;
import cn.xiaofuge.deepseek.harness.domain.schedule.IScheduleService;
import cn.xiaofuge.deepseek.harness.domain.schedule.model.entity.ScheduleRecord;
import cn.xiaofuge.deepseek.harness.domain.schedule.model.valobj.ScheduleView;
import cn.xiaofuge.deepseek.harness.domain.model.entity.ToolDefinition;
import cn.xiaofuge.deepseek.harness.domain.model.entity.ToolExecutionResult;
import cn.xiaofuge.deepseek.harness.domain.model.entity.ToolRunContext;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 调度提醒工具组：schedule_create / schedule_list / schedule_delete。
 * <p>
 * Mirrors TS {@code tool-schedule}. 提醒在当前会话内到期后被派发为一条提示。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public final class ScheduleTools {

    /**
     * 处理Schedule Tools相关逻辑。
     */
    private ScheduleTools() {}

    /**
     * Base 定义当前模块中的class契约与职责。
     */
    private abstract static class Base implements ToolDefinition {
        final IScheduleService schedules;
        final String sessionId;

        Base(IScheduleService schedules, String sessionId) {
            this.schedules = schedules;
            this.sessionId = sessionId;
        }

        @SuppressWarnings("unchecked")
        static Map<String, Object> argMap(Object args) {
            return args instanceof Map ? (Map<String, Object>) args : Map.of();
        }

        static String str(Map<String, Object> m, String k) {
            Object v = m.get(k);
            return v == null ? "" : String.valueOf(v).trim();
        }

        static long longVal(Map<String, Object> m, String k, long def) {
            Object v = m.get(k);
            if (v instanceof Number n) return n.longValue();
            if (v == null) return def;
            try { return Long.parseLong(String.valueOf(v)); } catch (NumberFormatException e) { return def; }
        }

        CompletableFuture<ToolExecutionResult> ok(String text) {
            return CompletableFuture.completedFuture(
                    ToolExecutionResult.ok(List.of(new TextBlock(text))));
        }

        CompletableFuture<ToolExecutionResult> fail(String msg, String code) {
            return CompletableFuture.completedFuture(ToolExecutionResult.fail(msg, code));
        }

        static String recordLine(ScheduleRecord r) {
            return "- " + r.id() + " [" + r.kind() + "] at " + r.scheduledAt() + ": " + r.prompt();
        }
    }

        /**
         * 创建。
         */
    /** schedule_create：创建一次性或周期性提醒。 */
    public static final class Create extends Base {
        /**
         * 创建。
         */
        public Create(IScheduleService schedules, String sessionId) { super(schedules, sessionId); }
        @Override public String name() { return "schedule_create"; }
        @Override public String description() {
            return "Create a reminder for this session: after N seconds (once), at an ISO instant (once), " +
                    "or every N seconds (recurring, min 300s). The reminder is delivered into this session when due.";
        }
        @Override public Map<String, Object> parameters() {
            return Map.of("type", "object", "additionalProperties", false,
                    "properties", Map.of(
                            "prompt", Map.of("type", "string", "description", "Reminder content delivered when due."),
                            "after_seconds", Map.of("type", "integer", "description", "One-shot: fire after N seconds."),
                            "at", Map.of("type", "string", "description", "One-shot: fire at ISO-8601 instant (UTC)."),
                            "every_seconds", Map.of("type", "integer", "description", "Recurring: fire every N seconds (min 300).")),
                    "required", List.of("prompt"));
        }
        @Override public CompletableFuture<ToolExecutionResult> execute(Object args, ToolRunContext ctx) {
            Map<String, Object> m = argMap(args);
            String prompt = str(m, "prompt");
            if (prompt.isEmpty()) return fail("prompt is required", "MISSING_PARAM");
            try {
                ScheduleRecord r;
                if (m.get("every_seconds") != null) {
                    r = schedules.createEvery(sessionId, prompt,
                            Duration.ofSeconds(longVal(m, "every_seconds", 300)));
                } else if (m.get("at") != null) {
                    r = schedules.createAt(sessionId, prompt, Instant.parse(str(m, "at")));
                } else if (m.get("after_seconds") != null) {
                    r = schedules.createAfter(sessionId, prompt,
                            Duration.ofSeconds(longVal(m, "after_seconds", 60)));
                } else {
                    return fail("one of after_seconds / at / every_seconds is required", "MISSING_PARAM");
                }
                return ok("Schedule created:\n" + recordLine(r));
            } catch (Exception e) {
                return fail("schedule_create failed: " + e.getMessage(), "EXECUTION_ERROR");
            }
        }
    }

    /** schedule_list：列出会话内提醒。 */
    public static final class ListAll extends Base {
        /**
         * 列出All。
         */
        public ListAll(IScheduleService schedules, String sessionId) { super(schedules, sessionId); }
        @Override public String name() { return "schedule_list"; }
        @Override public String description() {
            return "List active reminders for this session.";
        }
        @Override public Map<String, Object> parameters() {
            return Map.of("type", "object", "additionalProperties", false, "properties", Map.of());
        }
        @Override public CompletableFuture<ToolExecutionResult> execute(Object args, ToolRunContext ctx) {
            List<ScheduleView> views = schedules.list(sessionId);
            if (views.isEmpty()) return ok("No active reminders.");
            StringBuilder sb = new StringBuilder("Active reminders:\n");
            for (ScheduleView v : views) {
                sb.append(recordLine(v.record())).append(" [").append(v.state()).append("]\n");
            }
            return ok(sb.toString());
        }
        @Override public boolean isConcurrencySafe(Object args) { return true; }
    }

    /** schedule_delete：删除提醒。 */
    public static final class Delete extends Base {
        /**
         * 处理Delete相关逻辑。
         */
        public Delete(IScheduleService schedules, String sessionId) { super(schedules, sessionId); }
        @Override public String name() { return "schedule_delete"; }
        @Override public String description() {
            return "Delete a reminder by id.";
        }
        @Override public Map<String, Object> parameters() {
            return Map.of("type", "object", "additionalProperties", false,
                    "properties", Map.of(
                            "id", Map.of("type", "string", "description", "Reminder id.")),
                    "required", List.of("id"));
        }
        @Override public CompletableFuture<ToolExecutionResult> execute(Object args, ToolRunContext ctx) {
            String id = str(argMap(args), "id");
            if (id.isEmpty()) return fail("id is required", "MISSING_PARAM");
            boolean removed = schedules.delete(sessionId, id);
            return removed
                    ? ok("Reminder " + id + " deleted.")
                    : fail("reminder not found: " + id, "NOT_FOUND");
        }
    }
}
