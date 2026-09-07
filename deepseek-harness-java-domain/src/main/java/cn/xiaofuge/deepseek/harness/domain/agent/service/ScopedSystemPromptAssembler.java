package cn.xiaofuge.deepseek.harness.domain.agent.service;

import cn.xiaofuge.deepseek.harness.domain.agent.model.entity.AgentOptions;
import cn.xiaofuge.deepseek.harness.domain.model.valobj.ToolSchema;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * Enhanced system prompt assembler with sections, contexts, variables, and scope support.
 * <p>
 * Mirrors TS {@code dsh-system-prompt} package: ordered sections (persona, identity,
 * tool guidance), dynamic contexts (runtime snapshots), tool schemas with canonical
 * ordering, and {@code {{variable}}} interpolation.
 * <p>
 * Scope: global registrations plus per-agent scoped sections that shadow globals.
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public class ScopedSystemPromptAssembler implements SystemPromptAssembler {

    /** Reserved section name for the deployment persona. */
    public static final String PERSONA_SECTION = "deployment:persona";

    /** Prompt order of the persona slot. */
    public static final int PERSONA_ORDER = 0;

    // ── Global registry ──────────────────────────────────────────
    private final Map<String, PromptSection> sections = new LinkedHashMap<>();
    private final Map<String, PromptContext> contexts = new LinkedHashMap<>();
    private final List<Function<AssembleContext, ToolProviderResult>> toolProviders = new ArrayList<>();
    private final Map<String, Function<AssembleContext, String>> variables = new LinkedHashMap<>();
    private final List<Runnable> runtimeContextSuppressors = new ArrayList<>();

    private final String harnessIdentity;
    private final String persona;
    private final List<String> toolOrder;

    /**
     * 处理Scoped System 提示词 Assembler。
     * 流程：应用领域规则 → 更新领域对象 → 返回结果。
     * @param harnessIdentity harness Identity参数。
     * @param persona persona参数。
     * @param toolOrder 工具 Order参数。
     * @return 方法执行结果。
     */
    public ScopedSystemPromptAssembler(String harnessIdentity, String persona, List<String> toolOrder) {
        this.harnessIdentity = harnessIdentity != null ? harnessIdentity
                : "You are an AI agent powered by DeepSeek Harness.";
        this.persona = persona != null ? persona : "";
        this.toolOrder = toolOrder;

        // Register fixed sections
        registerSection(new PromptSection("harness:identity", -100, this.harnessIdentity));
        registerSection(new PromptSection(PERSONA_SECTION, PERSONA_ORDER, this.persona));
    }

    /** No-arg constructor with defaults. */
    public ScopedSystemPromptAssembler() {
        this(null, null, List.of());
    }

    // ── Registration API ─────────────────────────────────────────

    /**
     * 注册 Section。
     * 流程：应用领域规则 → 更新领域对象 → 返回结果。
     * @param section section参数。
     */
    public synchronized void registerSection(PromptSection section) {
        sections.put(section.name(), section);
    }

    /**
     * 移除 Section。
     * 流程：应用领域规则 → 更新领域对象 → 返回结果。
     * @param name 名称。
     */
    public synchronized void removeSection(String name) {
        sections.remove(name);
    }

    /**
     * 注册 上下文。
     * 流程：应用领域规则 → 更新领域对象 → 返回结果。
     * @param context 上下文对象。
     */
    public synchronized void registerContext(PromptContext context) {
        contexts.put(context.name(), context);
    }

    /**
     * 移除 上下文。
     * 流程：应用领域规则 → 更新领域对象 → 返回结果。
     * @param name 名称。
     */
    public synchronized void removeContext(String name) {
        contexts.remove(name);
    }

    /**
     * 注册 工具 模型提供方。
     * 流程：应用领域规则 → 更新领域对象 → 返回结果。
     * @param Function<AssembleContext Function Assemble 上下文参数。
     * @param provider 模型提供方。
     */
    public synchronized void registerToolProvider(Function<AssembleContext, ToolProviderResult> provider) {
        toolProviders.add(provider);
    }

    /**
     * 注册 Variable。
     * 流程：应用领域规则 → 更新领域对象 → 返回结果。
     * @param name 名称。
     * @param Function<AssembleContext Function Assemble 上下文参数。
     * @param provider 模型提供方。
     */
    public synchronized void registerVariable(String name, Function<AssembleContext, String> provider) {
        if (!isValidVariableName(name)) {
            throw new IllegalArgumentException("invalid variable name: " + name);
        }
        variables.put(name, provider);
    }

    /**
     * 处理suppress 运行时 上下文。
     * 流程：应用领域规则 → 更新领域对象 → 返回结果。
     */
    public synchronized void suppressRuntimeContext() {
        runtimeContextSuppressors.add(() -> {});
    }

    // ── Assembly ────────────────────────────────────────────────

    @Override
    public CompletableFuture<PromptAssembly> assemble(
            String agentId,
            AgentOptions options,
            String cwd,
            List<ToolSchema> tools
    ) {
        AssembleContext ctx = new AssembleContext(agentId, options, cwd, null);

        // Resolve sections
        List<AssembledSection> resolvedSections = new ArrayList<>();
        boolean hasComplete = false;
        AssembledSection completeSection = null;

        List<PromptSection> sorted = new ArrayList<>(sections.values());
        sorted.sort(Comparator.comparingInt(PromptSection::order));

        for (PromptSection sec : sorted) {
            String text = sec.text() instanceof String s ? s
                    : sec.text() instanceof Function f ? (String) f.apply(ctx) : "";
            AssembledSection assembled = new AssembledSection(sec.name(), text);
            if (sec.complete()) {
                hasComplete = true;
                completeSection = assembled;
            }
            resolvedSections.add(assembled);
        }

        // Resolve contexts
        boolean runtimeSuppressed = !runtimeContextSuppressors.isEmpty();
        List<AssembledContext> resolvedContexts = new ArrayList<>();
        if (!runtimeSuppressed) {
            List<PromptContext> sortedCtx = new ArrayList<>(contexts.values());
            sortedCtx.sort(Comparator.comparingInt(PromptContext::order));
            for (PromptContext c : sortedCtx) {
                String text = c.text() instanceof String s ? s
                        : c.text() instanceof Function f ? (String) f.apply(ctx) : "";
                resolvedContexts.add(new AssembledContext(c.name(), text));
            }
        }

        // Collect tool schemas
        List<ToolSchema> collected = new ArrayList<>(tools != null ? tools : List.of());
        for (Function<AssembleContext, ToolProviderResult> provider : toolProviders) {
            ToolProviderResult result = provider.apply(ctx);
            collected.addAll(result.schemas());
        }
        List<ToolSchema> orderedTools = orderTools(collected);

        // Resolve variables
        Map<String, String> resolvedVars = new LinkedHashMap<>();
        for (Map.Entry<String, Function<AssembleContext, String>> entry : variables.entrySet()) {
            resolvedVars.put(entry.getKey(), entry.getValue().apply(ctx));
        }

        // Build the PromptAssembly
        PromptAssemblyFull assembly = new PromptAssemblyFull(
                resolvedSections,
                resolvedContexts,
                orderedTools,
                resolvedVars
        );

        // Render
        String system = renderPrompt(assembly);
        String runtimeContext = renderContextSnapshot(assembly);
        if (!runtimeContext.isEmpty()) {
            system = runtimeContext + "\n\n" + system;
        }

        // If complete section, use only that
        if (hasComplete && completeSection != null) {
            system = interpolate(completeSection, resolvedVars);
        }

        return CompletableFuture.completedFuture(new SystemPromptAssembler.PromptAssembly(system, orderedTools));
    }

    // ── Rendering ───────────────────────────────────────────────

    /**
     * Interpolate {{variable}} references, drop empty sections, join with blank lines.
     */
    public static String renderPrompt(PromptAssemblyFull assembly) {
        return assembly.sections().stream()
                .map(s -> interpolate(s, assembly.variables()))
                .filter(s -> !s.isEmpty())
                .reduce((a, b) -> a + "\n\n" + b)
                .orElse("");
    }

    /**
     * Render the dynamic context snapshot.
     */
    public static String renderContextSnapshot(PromptAssemblyFull assembly) {
        String body = assembly.contexts().stream()
                .map(c -> interpolate(c, assembly.variables()))
                .filter(s -> !s.isEmpty())
                .reduce((a, b) -> a + "\n\n" + b)
                .orElse("");
        if (body.isEmpty()) return "";
        return "Current runtime context. This snapshot supersedes earlier runtime-context snapshots.\n\n" + body;
    }

    /** Interpolate one section or context. */
    private static String interpolate(AssembledSection input, Map<String, String> variables) {
        return doInterpolate(input.text(), variables, "section", input.name());
    }

    /**
     * 插值目标对象。
     * 流程：应用领域规则 → 更新领域对象 → 返回结果。
     * @param input 输入内容。
     * @param Map<String 映射 String参数。
     * @param variables variables参数。
     * @return 方法执行结果。
     */
    private static String interpolate(AssembledContext input, Map<String, String> variables) {
        return doInterpolate(input.text(), variables, "context", input.name());
    }

    /**
     * 处理do Interpolate。
     * 流程：应用领域规则 → 更新领域对象 → 返回结果。
     * @param text text参数。
     * @param Map<String 映射 String参数。
     * @param variables variables参数。
     * @param kind kind参数。
     * @param name 名称。
     * @return 方法执行结果。
     */
    private static String doInterpolate(String text, Map<String, String> variables, String kind, String name) {
        if (text == null || text.isEmpty()) return "";
        StringBuilder result = new StringBuilder();
        int last = 0;
        int open = text.indexOf("{{");
        while (open >= 0) {
            int close = text.indexOf("}}", open + 2);
            if (close < 0) {
                result.append(text, last, open + 2);
                break;
            }
            String varName = text.substring(open + 2, close).trim();
            if (varName.isEmpty()) {
                throw new IllegalArgumentException("malformed prompt variable reference in " + kind + " \"" + name + "\"");
            }
            if (!variables.containsKey(varName)) {
                // Unknown variable — leave literal
                result.append(text, last, close + 2);
            } else {
                String value = variables.get(varName);
                if (value == null) {
                    throw new IllegalArgumentException("prompt variable \"" + varName + "\" has no value in " + kind + " \"" + name + "\"");
                }
                result.append(text, last, open);
                result.append(value);
            }
            last = close + 2;
            open = text.indexOf("{{", last);
        }
        result.append(text, last, text.length());
        return result.toString();
    }

    // ── Tool ordering ───────────────────────────────────────────

    private static final String TOOL_ORDER_REST = "<unlisted-tools>";

    /**
     * 处理order Tools。
     * 流程：应用领域规则 → 更新领域对象 → 返回结果。
     * @param tools 工具列表。
     * @return 方法执行结果。
     */
    private List<ToolSchema> orderTools(List<ToolSchema> tools) {
        if (toolOrder == null || toolOrder.isEmpty()) {
            return tools.stream().sorted((a, b) -> a.name().compareTo(b.name())).toList();
        }
        List<String> order = new ArrayList<>(toolOrder);
        if (!order.contains(TOOL_ORDER_REST)) {
            return tools.stream().sorted((a, b) -> a.name().compareTo(b.name())).toList();
        }
        var listed = new java.util.HashSet<String>(order);
        var rest = tools.stream().filter(t -> !listed.contains(t.name()))
                .sorted((a, b) -> a.name().compareTo(b.name())).toList();
        List<ToolSchema> result = new ArrayList<>();
        for (String name : order) {
            if (name.equals(TOOL_ORDER_REST)) {
                result.addAll(rest);
            } else {
                tools.stream().filter(t -> t.name().equals(name)).findFirst().ifPresent(result::add);
            }
        }
        return result;
    }

    /**
     * 判断Valid Variable Name是否成立。
     */
    private static boolean isValidVariableName(String name) {
        return name != null && name.matches("^[a-z][a-z0-9_]*$");
    }

    // ── Types ───────────────────────────────────────────────────

    /** A registered prompt section. */
    public record PromptSection(
            String name,
            int order,
            Object text, // String or Function<AssembleContext, String>
            boolean complete
    ) {
        /**
         * 发起提示词 Section。
         * 流程：应用领域规则 → 更新领域对象 → 返回结果。
         * @param name 名称。
         * @param order 排序规则。
         * @param text text参数。
         * @return 方法执行结果。
         */
        public PromptSection(String name, int order, String text) {
            this(name, order, text, false);
        }
        /**
         * 发起提示词 Section。
         * 流程：应用领域规则 → 更新领域对象 → 返回结果。
         * @param name 名称。
         * @param order 排序规则。
         * @param text text参数。
         * @return 方法执行结果。
         */
        public PromptSection(String name, int order, Object text) {
            this(name, order, text, false);
        }
    }

    /** A registered dynamic context. */
    public record PromptContext(
            String name,
            int order,
            Object text // String or Function<AssembleContext, String>
    ) {}

    /** One resolved section. */
    public record AssembledSection(String name, String text) {}

    /** One resolved context. */
    public record AssembledContext(String name, String text) {}

    /** Tool provider result. */
    public record ToolProviderResult(
            List<ToolSchema> schemas,
            List<String> knownNames
    ) {
        /**
         * 转换为 ol 模型提供方 结果。
         * 流程：应用领域规则 → 更新领域对象 → 返回结果。
         * @param schemas schemas参数。
         * @return 方法执行结果。
         */
        public ToolProviderResult(List<ToolSchema> schemas) {
            this(schemas, schemas.stream().map(ToolSchema::name).toList());
        }
    }

    /** Full assembly before rendering. */
    public record PromptAssemblyFull(
            List<AssembledSection> sections,
            List<AssembledContext> contexts,
            List<ToolSchema> tools,
            Map<String, String> variables
    ) {}

    /** Assembly context. */
    public record AssembleContext(
            String agentId,
            AgentOptions options,
            String cwd,
            Object signal
    ) {}
}
