package cn.walioffice.deepseek.harness.domain.runtime.profile.model.valobj;

/**
 * Describes the resolved execution mode for one profile code.
 *
 * @param profileCode execution profile code
 * @param executionMode resolved execution mode
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public record ExecutionProfileVO(
        String profileCode,
        String executionMode
) {
}
