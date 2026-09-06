package cn.xiaofuge.deepseek.harness.domain.tool.fs;

import cn.xiaofuge.deepseek.harness.domain.tool.fs.adapter.port.IFsPort;

import cn.xiaofuge.deepseek.harness.domain.model.valobj.ContentBlock;
import cn.xiaofuge.deepseek.harness.domain.model.valobj.TextBlock;
import cn.xiaofuge.deepseek.harness.domain.model.entity.ToolDefinition;
import cn.xiaofuge.deepseek.harness.domain.model.entity.ToolExecutionResult;
import cn.xiaofuge.deepseek.harness.domain.model.entity.ToolRunContext;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 文件读取工具。
 * <p>
 * 执行流程：
 * 1. 接收文件路径和可选行范围；
 * 2. 读取文件内容并转成 UTF-8 文本；
 * 3. 将结果返回给调用方。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public class FsReadTool implements ToolDefinition {

    private static final String NAME = "fs_read";
    private static final String DESCRIPTION =
            "Read the contents of a text file. Supports line offset and limit " +
            "for reading large files in chunks. Output is truncated to 2000 lines " +
            "or 50KB, whichever is hit first.";

    private static final int DEFAULT_LIMIT = 2000;

    private final IFsPort fsService;

    public FsReadTool(IFsPort fsService) {
        this.fsService = fsService;
    }

    @Override public String name() { return NAME; }
    @Override public String description() { return DESCRIPTION; }

    @Override
    public Map<String, Object> parameters() {
        return Map.of(
                "type", "object",
                "additionalProperties", false,
                "properties", Map.of(
                        "path", Map.of(
                                "type", "string",
                                "description", "Path to the file to read. Relative paths resolve against the agent's working directory."
                        ),
                        "offset", Map.of(
                                "type", "integer",
                                "description", "Line number to start reading from (1-indexed). Default 1."
                        ),
                        "limit", Map.of(
                                "type", "integer",
                                "description", "Maximum number of lines to read. Default 2000."
                        )
                ),
                "required", List.of("path")
        );
    }

    @Override
    public Map<String, Object> outputSchema() {
        return Map.of(
                "type", "object",
                "additionalProperties", true,
                "properties", Map.of(
                        "content", Map.of("type", "string"),
                        "lines_read", Map.of("type", "integer"),
                        "total_lines", Map.of("type", "integer")
                )
        );
    }

    @Override
    @SuppressWarnings("unchecked")
    public CompletableFuture<ToolExecutionResult> execute(Object args, ToolRunContext ctx) {
        try {
            Map<String, Object> argMap = (Map<String, Object>) args;
            String path = String.valueOf(argMap.get("path"));
            if (path == null || path.isBlank()) {
                return CompletableFuture.completedFuture(
                        ToolExecutionResult.fail("path parameter is required", "MISSING_PARAM"));
            }

            int offset = argMap.get("offset") != null
                    ? ((Number) argMap.get("offset")).intValue()
                    : 1;
            int limit = argMap.get("limit") != null
                    ? ((Number) argMap.get("limit")).intValue()
                    : DEFAULT_LIMIT;

            if (offset < 1) offset = 1;
            if (limit < 1) limit = DEFAULT_LIMIT;

            String content = fsService.read(path, offset, limit);
            List<ContentBlock> blocks = List.of(new TextBlock(content));
            return CompletableFuture.completedFuture(ToolExecutionResult.ok(blocks));
        } catch (Exception e) {
            return CompletableFuture.completedFuture(
                    ToolExecutionResult.fail("fs_read failed: " + e.getMessage(), "EXECUTION_ERROR"));
        }
    }

    @Override
    public boolean isConcurrencySafe(Object args) { return true; }
}
