package cn.walioffice.plugin.sample;

import cn.xiaofuge.deepseek.harness.domain.model.entity.AbstractTool;
import cn.xiaofuge.deepseek.harness.domain.model.entity.ToolExecutionResult;
import cn.xiaofuge.deepseek.harness.domain.model.entity.ToolRunContext;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 文件摘要工具。
 * <p>
 * 继承 {@link AbstractTool}，演示一个有文件系统副作用的工具（非并发安全）。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public class FileSummaryTool extends AbstractTool {

    private static final int MAX_PREVIEW_LINES = 20;

    @Override
    public String name() {
        return "file_summary";
    }

    @Override
    public String description() {
        return "读取指定文件并返回摘要：行数、字符数、前几行内容预览。";
    }

    @Override
    public Map<String, Object> parameters() {
        return objectSchema()
                .prop("path", stringSchema("文件绝对路径"))
                .prop("maxLines", integerSchema("预览行数上限（默认 20）"))
                .required("path")
                .build();
    }

    @Override
    public Map<String, Object> outputSchema() {
        return objectSchema()
                .prop("path", stringSchema(null))
                .prop("totalLines", integerSchema(null))
                .prop("totalChars", integerSchema(null))
                .prop("preview", stringSchema(null))
                .build();
    }

    @Override
    public boolean isConcurrencySafe(Object args) {
        return false;
    }

    @Override
    public long timeoutMs() {
        return 10_000;
    }

    @Override
    protected CompletableFuture<ToolExecutionResult> run(Map<String, Object> args, ToolRunContext ctx) {
        String pathStr = str(args, "path");
        int maxLines = intVal(args, "maxLines", MAX_PREVIEW_LINES);

        if (pathStr.isBlank()) {
            return fail("path 参数不能为空", "MISSING_PATH");
        }

        Path filePath = Path.of(pathStr);
        if (!Files.exists(filePath)) {
            return fail("文件不存在: " + pathStr, "FILE_NOT_FOUND");
        }
        if (Files.isDirectory(filePath)) {
            return fail("路径是目录，不是文件: " + pathStr, "IS_DIRECTORY");
        }

        try {
            List<String> lines = Files.readAllLines(filePath);
            int totalLines = lines.size();
            int totalChars = lines.stream().mapToInt(String::length).sum();
            int previewCount = Math.min(maxLines, totalLines);
            List<String> previewLines = lines.subList(0, previewCount);

            StringBuilder sb = new StringBuilder();
            sb.append("文件: ").append(pathStr).append("\n");
            sb.append("总行数: ").append(totalLines).append("\n");
            sb.append("总字符数: ").append(totalChars).append("\n");
            sb.append("前 ").append(previewCount).append(" 行预览:\n");
            sb.append("```\n");
            for (int i = 0; i < previewLines.size(); i++) {
                sb.append(String.format("%4d | %s%n", i + 1, previewLines.get(i)));
            }
            if (totalLines > previewCount) {
                sb.append("... (还有 ").append(totalLines - previewCount).append(" 行未显示)\n");
            }
            sb.append("```");

            return ok(sb.toString());
        } catch (IOException e) {
            return fail("读取文件失败: " + e.getMessage(), "READ_ERROR");
        }
    }
}
