package cn.xiaofuge.deepseek.harness.domain.tool.lsp;

import cn.xiaofuge.deepseek.harness.domain.model.valobj.TextBlock;
import cn.xiaofuge.deepseek.harness.domain.lsp.ILspService;
import cn.xiaofuge.deepseek.harness.domain.lsp.model.valobj.LspLocation;
import cn.xiaofuge.deepseek.harness.domain.lsp.model.valobj.LspQueryResult;
import cn.xiaofuge.deepseek.harness.domain.lsp.model.valobj.LspQueryType;
import cn.xiaofuge.deepseek.harness.domain.tool.fs.adapter.port.IFsPort;
import cn.xiaofuge.deepseek.harness.domain.model.entity.ToolDefinition;
import cn.xiaofuge.deepseek.harness.domain.model.entity.ToolExecutionResult;
import cn.xiaofuge.deepseek.harness.domain.model.entity.ToolRunContext;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * {@code lsp} 工具：语言服务查询（定义/引用/实现/hover）。
 * <p>
 * Mirrors TS {@code tool-lsp}. 依赖 {@link ILspService} 注册的 per-extension provider；
 * 无 provider 时返回明确提示而不是空结果。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public class LspTool implements ToolDefinition {

    private final ILspService lspService;
    private final IFsPort fsService;

    /**
     * 处理Lsp Tool相关逻辑。
     */
    public LspTool(ILspService lspService, IFsPort fsService) {
        this.lspService = lspService;
        this.fsService = fsService;
    }

    @Override public String name() { return "lsp"; }

    @Override
    public String description() {
        return "Query language-server intelligence for a symbol at a file position: " +
                "definition, references, implementation, or hover. " +
                "Provide the file path plus 0-based line and character of the symbol.";
    }

    @Override
    public Map<String, Object> parameters() {
        return Map.of(
                "type", "object",
                "additionalProperties", false,
                "properties", Map.of(
                        "query", Map.of(
                                "type", "string",
                                "enum", List.of("definition", "references", "implementation", "hover"),
                                "description", "Query type."),
                        "path", Map.of("type", "string", "description", "File path."),
                        "line", Map.of("type", "integer", "description", "0-based line number."),
                        "character", Map.of("type", "integer", "description", "0-based character offset.")
                ),
                "required", List.of("query", "path", "line", "character")
        );
    }

    @Override
    @SuppressWarnings("unchecked")
    public CompletableFuture<ToolExecutionResult> execute(Object args, ToolRunContext ctx) {
        try {
            Map<String, Object> m = args instanceof Map ? (Map<String, Object>) args : Map.of();
            String query = m.get("query") == null ? "" : String.valueOf(m.get("query")).trim().toLowerCase();
            String path = m.get("path") == null ? "" : String.valueOf(m.get("path")).trim();
            if (query.isEmpty() || path.isEmpty()) {
                return CompletableFuture.completedFuture(
                        ToolExecutionResult.fail("query and path are required", "MISSING_PARAM"));
            }
            LspQueryType type = switch (query) {
                case "definition" -> LspQueryType.DEFINITION;
                case "references" -> LspQueryType.REFERENCES;
                case "implementation" -> LspQueryType.IMPLEMENTATION;
                case "hover" -> LspQueryType.HOVER;
                default -> null;
            };
            if (type == null) {
                return CompletableFuture.completedFuture(
                        ToolExecutionResult.fail("unknown query type: " + query, "INVALID_ARGS"));
            }
            int line = m.get("line") instanceof Number n ? n.intValue() : 0;
            int character = m.get("character") instanceof Number n2 ? n2.intValue() : 0;

            String content = fsService.read(path, 1, -1);
            Optional<LspQueryResult> result = lspService.query(type, path, content, line, character);
            if (result.isEmpty()) {
                return CompletableFuture.completedFuture(ToolExecutionResult.ok(List.of(new TextBlock(
                        "No language server registered for this file type, or the query returned nothing: "
                                + path + " (" + query + " @" + line + ":" + character + ")"))));
            }
            LspQueryResult r = result.get();
            StringBuilder sb = new StringBuilder();
            sb.append(query).append(" results for ").append(path)
              .append(" @").append(line).append(":").append(character).append("\n");
            r.hoverText().ifPresent(h -> sb.append("--- hover ---\n").append(h).append("\n"));
            for (LspLocation loc : r.locations()) {
                sb.append("- ").append(loc.toString()).append("\n");
            }
            if (r.locations().isEmpty() && r.hoverText().isEmpty()) {
                sb.append("(no results)");
            }
            return CompletableFuture.completedFuture(
                    ToolExecutionResult.ok(List.of(new TextBlock(sb.toString()))));
        } catch (Exception e) {
            return CompletableFuture.completedFuture(
                    ToolExecutionResult.fail("lsp failed: " + e.getMessage(), "EXECUTION_ERROR"));
        }
    }

    @Override
    public boolean isConcurrencySafe(Object args) { return true; }
}
