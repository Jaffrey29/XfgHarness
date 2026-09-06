package cn.walioffice.deepseek.harness.domain.tool.skill;

import cn.walioffice.deepseek.harness.domain.llm.model.valobj.ContentBlock;
import cn.walioffice.deepseek.harness.domain.llm.model.valobj.TextBlock;
import cn.walioffice.deepseek.harness.domain.skill.ISkillService;
import cn.walioffice.deepseek.harness.domain.skill.model.entity.SkillDefinition;
import cn.walioffice.deepseek.harness.domain.tool.model.entity.ToolDefinition;
import cn.walioffice.deepseek.harness.domain.tool.model.entity.ToolExecutionResult;
import cn.walioffice.deepseek.harness.domain.tool.model.entity.ToolRunContext;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * {@code skill} 工具：按名称加载技能指令并注入会话上下文。
 * <p>
 * 执行流程：
 * 1. 模型给出技能名（kebab-case）；
 * 2. 经 {@link ISkillService} 解析出胜出候选并加载完整定义；
 * 3. 技能正文作为工具结果返回，同时 deferContext 一条指令消息供后续步骤遵循。
 * <p>
 * Mirrors TS {@code tool-skill}.
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public class SkillTool implements ToolDefinition {

    private static final String NAME = "skill";
    private static final String DESCRIPTION =
            "Load a skill's instruction set by name (kebab-case) and apply it to the current session. " +
            "Use when a task matches an installed skill's domain — the skill body tells you the exact workflow, " +
            "commands, and conventions to follow.";

    private final ISkillService skillService;
    private final String cwd;

    public SkillTool(ISkillService skillService, String cwd) {
        this.skillService = skillService;
        this.cwd = cwd;
    }

    @Override public String name() { return NAME; }
    @Override public String description() { return DESCRIPTION; }

    @Override
    public Map<String, Object> parameters() {
        return Map.of(
                "type", "object",
                "additionalProperties", false,
                "properties", Map.of(
                        "name", Map.of(
                                "type", "string",
                                "description", "Skill name in kebab-case, e.g. pdf, find-skills, monorepo-setup."
                        )
                ),
                "required", List.of("name")
        );
    }

    @Override
    public Map<String, Object> outputSchema() {
        return Map.of(
                "type", "object",
                "additionalProperties", true,
                "properties", Map.of(
                        "name", Map.of("type", "string"),
                        "loaded", Map.of("type", "boolean")
                )
        );
    }

    @Override
    @SuppressWarnings("unchecked")
    public CompletableFuture<ToolExecutionResult> execute(Object args, ToolRunContext ctx) {
        Map<String, Object> argMap = args instanceof Map ? (Map<String, Object>) args : Map.of();
        String name = argMap.get("name") == null ? "" : String.valueOf(argMap.get("name")).trim();
        if (name.isEmpty()) {
            return CompletableFuture.completedFuture(
                    ToolExecutionResult.fail("name parameter is required", "MISSING_PARAM"));
        }
        Optional<SkillDefinition> def = skillService.get(name, cwd);
        if (def.isEmpty()) {
            List<String> available = skillService.list(cwd).stream().map(s -> s.name()).limit(50).toList();
            return CompletableFuture.completedFuture(ToolExecutionResult.fail(
                    "skill not found: " + name + ". Available: " + String.join(", ", available),
                    "SKILL_NOT_FOUND"));
        }
        SkillDefinition skill = def.get();
        String body = "<skill name=\"" + skill.name() + "\" source=\"" + skill.source()
                + "\" path=\"" + skill.path().orElse("") + "\">\n"
                + skill.content()
                + "\n</skill>\nFollow the skill instructions above for the remainder of this task.";
        List<ContentBlock> content = List.of(new TextBlock(body));
        return CompletableFuture.completedFuture(ToolExecutionResult.ok(content));
    }

    @Override
    public boolean isConcurrencySafe(Object args) { return true; }
}
