package cn.xiaofuge.deepseek.harness.infrastructure.adapter.tool;

import cn.xiaofuge.deepseek.harness.domain.tool.service.ToolArgumentsParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;

/**
 * Parses model tool-call arguments with Jackson.
 */
public class JacksonToolArgumentsParser implements ToolArgumentsParser {

    private final ObjectMapper objectMapper;

    /**
     * 处理Jackson Tool Arguments Parser相关逻辑。
     */
    public JacksonToolArgumentsParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Object parse(String arguments) {
        if (arguments == null || arguments.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(arguments, Object.class);
        } catch (Exception ignored) {
            return arguments;
        }
    }
}
