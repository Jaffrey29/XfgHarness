package cn.xiaofuge.deepseek.harness.domain.tool.fs;

import cn.xiaofuge.deepseek.harness.domain.model.valobj.TextBlock;
import cn.xiaofuge.deepseek.harness.domain.tool.fs.adapter.port.IFsPort;
import cn.xiaofuge.deepseek.harness.domain.model.entity.ToolDefinition;
import cn.xiaofuge.deepseek.harness.domain.model.entity.ToolExecutionResult;
import cn.xiaofuge.deepseek.harness.domain.model.entity.ToolRunContext;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 文件写入工具。
 * <p>
 * 执行流程：
 * 1. 接收文件路径和写入内容；
 * 2. 按需创建或覆盖文件；
 * 3. 返回写入结果。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public class FsWriteTool implements ToolDefinition {

    private static final String NAME = "fs_write";
    private static final String DESCRIPTION =
            "Write content to a file. Creates the file if it doesn't exist, " +
            "overwrites if it does. Parent directories are created automatically. " +
            "Use this for creating new files, updating configurations, or saving output.";

    private final IFsPort fsService;

    /**
     * 处理Fs Write Tool相关逻辑。
     */
    public FsWriteTool(IFsPort fsService) {
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
                                "description", "Path to the file to write. Relative paths resolve against the agent's working directory."
                        ),
                        "content", Map.of(
                                "type", "string",
                                "description", "The content to write to the file."
                        )
                ),
                "required", List.of("path", "content")
        );
    }

    @Override
    public Map<String, Object> outputSchema() {
        return Map.of(
                "type", "object",
                "additionalProperties", true,
                "properties", Map.of(
                        "path", Map.of("type", "string"),
                        "bytes_written", Map.of("type", "integer")
                )
        );
    }

    @Override
    @SuppressWarnings("unchecked")
    public CompletableFuture<ToolExecutionResult> execute(Object args, ToolRunContext ctx) {
        try {
            Map<String, Object> argMap = (Map<String, Object>) args;
            String path = String.valueOf(argMap.get("path"));
            String content = String.valueOf(argMap.get("content"));

            if (path == null || path.isBlank()) {
                return CompletableFuture.completedFuture(
                        ToolExecutionResult.fail("path parameter is required", "MISSING_PARAM"));
            }
            if (content == null) {
                content = "";
            }

            fsService.write(path, content);

            int bytes = content.getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
            String result = "Wrote " + bytes + " bytes to " + path;
            return CompletableFuture.completedFuture(
                    ToolExecutionResult.ok(List.of(new TextBlock(result))));
        } catch (Exception e) {
            return CompletableFuture.completedFuture(
                    ToolExecutionResult.fail("fs_write failed: " + e.getMessage(), "EXECUTION_ERROR"));
        }
    }

    @Override
    public boolean isConcurrencySafe(Object args) { return false; }
}
