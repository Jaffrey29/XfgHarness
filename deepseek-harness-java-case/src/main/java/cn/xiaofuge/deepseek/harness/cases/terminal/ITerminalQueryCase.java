package cn.xiaofuge.deepseek.harness.cases.terminal;

import cn.xiaofuge.deepseek.harness.api.dto.TerminalReadResponseDTO;
import cn.xiaofuge.deepseek.harness.api.dto.TerminalSessionResponseDTO;
import java.util.List;

/**
 * 终端会话列表和输出查询用例契约。
 * <p>流程：Trigger/前端 → 本接口 → TerminalPipelineFactory → 终端领域查询服务。</p>
 * <p>数据示例：列表无入参；读取传入 {@code sessionId=term-001, maxChars=8000}，返回终端文本。</p>
 */
public interface ITerminalQueryCase {
    List<TerminalSessionResponseDTO> listSessions();
    TerminalReadResponseDTO read(String sessionId, int maxChars);
}
