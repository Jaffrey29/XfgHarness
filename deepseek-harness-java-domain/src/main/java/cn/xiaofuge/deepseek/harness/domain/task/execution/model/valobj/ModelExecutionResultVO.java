package cn.xiaofuge.deepseek.harness.domain.task.execution.model.valobj;

/**
 * Returns one assistant reply from the configured model provider.
 *
 * @param providerCode provider identifier
 * @param modelCode model identifier
 * @param content assistant reply text
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public record ModelExecutionResultVO(
        String providerCode,
        String modelCode,
        String content
) {
}
