package cn.xiaofuge.deepseek.harness.api.response.terminal;

import cn.xiaofuge.deepseek.harness.domain.terminal.model.valobj.TerminalReadResultVO;

/**
 * 终端读取响应。
 * <p>
 * 执行流程：
 * 1. 读取当前输出片段；
 * 2. 映射为响应体；
 * 3. 返回给前端轮询。
 *
 * <p>调用流程：Controller/DTO → 应用门面 → 用例；响应对象由用例结果转换。</p>
 * <p>示例：DTO 只做协议隔离，不承载领域规则。</p>
 * @author 小傅哥
 * @website bugstack.cn
 */
public record TerminalReadResponse(
        String sessionId,
        String content,
        boolean truncated,
        String status,
        Integer exitCode
) {

    /**
     * 根据目标对象。
     * 流程：应用领域规则 → 更新领域对象 → 返回结果。
     * @param result 结果对象。
     * @return 创建的领域对象。
     */
    public static TerminalReadResponse from(TerminalReadResultVO result) {
        return new TerminalReadResponse(
                result.sessionId(),
                result.content(),
                result.truncated(),
                result.status().name(),
                result.exitCode()
        );
    }
}
