package cn.xiaofuge.deepseek.harness.cases.agent.node;

import cn.xiaofuge.deepseek.harness.api.dto.AgentMessageRequestDTO;
import cn.xiaofuge.deepseek.harness.api.dto.AgentMessageResponseDTO;
import cn.xiaofuge.deepseek.harness.cases.agent.factory.AgentMessageDynamicContext;
import cn.xiaofuge.deepseek.harness.cases.orchestration.AbstractStrategyRouter;

/**
 * Agent 消息策略树节点的抽象基类。
 *
 * <p>调用流程：HTTP/API 门面 → AgentUseCase → AgentMessageFactory → 策略树 → ReactLoopAgent；会话事件写入 SessionEventLog。</p>
 * <p>示例：调用方传入 agentId、用户消息和可选模型配置，返回助手文本与工具调用结果。</p>
 * @author 小傅哥
 * @website bugstack.cn
 */
public abstract class AbstractAgentMessageNode
        extends AbstractStrategyRouter<AgentMessageRequestDTO, AgentMessageDynamicContext, AgentMessageResponseDTO> {
}
