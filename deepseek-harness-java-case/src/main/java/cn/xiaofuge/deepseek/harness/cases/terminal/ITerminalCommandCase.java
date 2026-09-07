package cn.xiaofuge.deepseek.harness.cases.terminal;

import cn.xiaofuge.deepseek.harness.api.dto.TerminalOpenRequestDTO;
import cn.xiaofuge.deepseek.harness.api.dto.TerminalSendRequestDTO;
import cn.xiaofuge.deepseek.harness.api.dto.TerminalSendResponseDTO;
import cn.xiaofuge.deepseek.harness.api.dto.TerminalSessionResponseDTO;

/**
 * 终端打开、执行命令和关闭用例契约。
 * <p>流程：Trigger/前端 → 本接口 → TerminalPipelineFactory → 终端领域命令服务。</p>
 * <p>数据示例：打开传入 {@code name=build, cwd=/tmp/demo}；
 * 执行传入 {@code command=ls, timeoutMs=3000}；关闭传入 {@code sessionId=term-001}。</p>
 */
public interface ITerminalCommandCase {
    TerminalSessionResponseDTO open(TerminalOpenRequestDTO command);
    TerminalSendResponseDTO send(String sessionId, TerminalSendRequestDTO command);
    TerminalSessionResponseDTO close(String sessionId);
}
