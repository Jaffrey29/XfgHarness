package cn.walioffice.deepseek.harness.domain.tool.web;

import cn.walioffice.deepseek.harness.domain.llm.model.valobj.ContentBlock;
import cn.walioffice.deepseek.harness.domain.llm.model.valobj.TextBlock;
import cn.walioffice.deepseek.harness.domain.tool.web.adapter.port.IWebPort;
import cn.walioffice.deepseek.harness.domain.tool.model.entity.ToolDefinition;
import cn.walioffice.deepseek.harness.domain.tool.model.entity.ToolExecutionResult;
import cn.walioffice.deepseek.harness.domain.tool.model.entity.ToolRunContext;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 网页搜索工具。
 * <p>
 * 执行流程：
 * 1. 接收搜索词；
 * 2. 调用网页搜索服务获取结果；
 * 3. 将结果整理后返回给模型。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public class WebSearchTool implements ToolDefinition {

    private static final String NAME = "web_search";
    private static final String DESCRIPTION =
            "Search the web for current information. Returns a list of sources " +
            "with titles, URLs, and snippets. Use this for finding up-to-date " +
            "information, documentation, or answers to factual questions.";
    private static final int DEFAULT_MAX_RESULTS = 8;
    private static final long TIMEOUT_MS = 30_000;

    private final IWebPort webService;

    public WebSearchTool(IWebPort webService) {
        this.webService = webService;
    }

    @Override public String name() { return NAME; }
    @Override public String description() { return DESCRIPTION; }

    @Override
    public Map<String, Object> parameters() {
        return Map.of(
                "type", "object",
                "additionalProperties", false,
                "properties", Map.of(
                        "query", Map.of(
                                "type", "string",
                                "description", "The search query."
                        ),
                        "max_results", Map.of(
                                "type", "integer",
                                "description", "Maximum number of results. Default 8."
                        )
                ),
                "required", List.of("query")
        );
    }

    @Override
    public Map<String, Object> outputSchema() {
        return Map.of(
                "type", "object",
                "additionalProperties", true,
                "properties", Map.of(
                        "content", Map.of("type", "string"),
                        "sources", Map.of(
                                "type", "array",
                                "items", Map.of(
                                        "type", "object",
                                        "properties", Map.of(
                                                "url", Map.of("type", "string"),
                                                "title", Map.of("type", "string"),
                                                "snippet", Map.of("type", "string")
                                        )
                                )
                        )
                )
        );
    }

    @Override
    @SuppressWarnings("unchecked")
    public CompletableFuture<ToolExecutionResult> execute(Object args, ToolRunContext ctx) {
        try {
            Map<String, Object> argMap = (Map<String, Object>) args;
            String query = String.valueOf(argMap.get("query"));
            if (query == null || query.isBlank()) {
                return CompletableFuture.completedFuture(
                        ToolExecutionResult.fail("query parameter is required", "MISSING_PARAM"));
            }

            int maxResults = argMap.get("max_results") != null
                    ? ((Number) argMap.get("max_results")).intValue()
                    : DEFAULT_MAX_RESULTS;

            IWebPort.SearchResult result = webService.search(query, maxResults);

            StringBuilder sb = new StringBuilder();
            if (result.content() != null && !result.content().isEmpty()) {
                sb.append(result.content()).append("\n\n");
            }

            if (result.sources() != null && !result.sources().isEmpty()) {
                sb.append("Sources:\n");
                for (IWebPort.WebSource src : result.sources()) {
                    String label = (src.title() != null && !src.title().isEmpty())
                            ? src.title()
                            : hostname(src.url());
                    List<String> meta = new java.util.ArrayList<>();
                    if (src.snippet() != null && !src.snippet().isEmpty()) meta.add(src.snippet());
                    if (src.publishedAt() != null && !src.publishedAt().isEmpty()) meta.add("(" + src.publishedAt() + ")");
                    String suffix = meta.isEmpty() ? "" : " — " + String.join(" ", meta);
                    sb.append("- [").append(label).append("](").append(src.url()).append(")").append(suffix).append("\n");
                }
            } else if (result.content() == null || result.content().isEmpty()) {
                sb.append("No results found.");
            }

            if (result.truncated()) {
                sb.append("\n(Showing the first ").append(result.sources().size()).append(" sources. Refine the query for more.)");
            }
            sb.append("\n\nCite the relevant URLs above as markdown links in your answer.");

            return CompletableFuture.completedFuture(
                    ToolExecutionResult.ok(List.of(new TextBlock(sb.toString()))));
        } catch (Exception e) {
            return CompletableFuture.completedFuture(
                    ToolExecutionResult.fail("web_search failed: " + e.getMessage(), "EXECUTION_ERROR"));
        }
    }

    @Override
    public boolean isConcurrencySafe(Object args) { return true; }

    @Override
    public long timeoutMs() { return TIMEOUT_MS; }

    private static String hostname(String url) {
        try {
            return new java.net.URI(url).getHost();
        } catch (Exception e) {
            return url;
        }
    }
}
