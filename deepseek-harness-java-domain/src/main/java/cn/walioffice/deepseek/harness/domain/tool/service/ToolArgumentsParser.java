package cn.walioffice.deepseek.harness.domain.tool.service;

/**
 * Converts a model-provided JSON argument string into the tool execution value.
 */
@FunctionalInterface
public interface ToolArgumentsParser {

    Object parse(String arguments);

    static ToolArgumentsParser identity() {
        return arguments -> arguments;
    }
}
