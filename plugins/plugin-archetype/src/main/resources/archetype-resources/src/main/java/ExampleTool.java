package ${package};

import cn.walioffice.deepseek.harness.domain.tool.model.entity.AbstractTool;
import cn.walioffice.deepseek.harness.domain.tool.model.entity.ToolExecutionResult;
import cn.walioffice.deepseek.harness.domain.tool.model.entity.ToolRunContext;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 示例工具：回显输入文本。
 * <p>
 * 继承 {@link AbstractTool}，实现 name/description/parameters/run 即可。
 * 参数读取用 {@code str/dbl/intVal/bool}，结果用 {@code ok/fail}。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public class ExampleTool extends AbstractTool {

    @Override
    public String name() {
        return "example_echo";
    }

    @Override
    public String description() {
        return "Echoes back the provided text.";
    }

    @Override
    public Map<String, Object> parameters() {
        return objectSchema()
                .prop("text", stringSchema("The text to echo back"))
                .required("text")
                .build();
    }

    @Override
    protected CompletableFuture<ToolExecutionResult> run(Map<String, Object> args, ToolRunContext ctx) {
        String text = str(args, "text");
        if (text.isBlank()) {
            return fail("text 不能为空", "MISSING_TEXT");
        }
        return ok("Echo: " + text);
    }
}
